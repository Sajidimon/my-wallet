package com.example.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.db.WalletDatabase
import com.example.data.repository.WalletRepositoryImpl
import com.example.domain.model.*
import com.example.domain.repository.WalletRepository
import com.example.services.AppSettingsService
import com.example.services.FinanceExportService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class BasicFilters(
    val query: String,
    val catId: Int?,
    val accId: Int?,
    val type: String
)

data class DisplayFilters(
    val sort: String,
    val dateRange: String,
    val customStart: Long?,
    val customEnd: Long?
)

class WalletViewModel(application: Application) : AndroidViewModel(application) {

    private val database = WalletDatabase.getInstance(application, viewModelScope)
    private val repository: WalletRepository = WalletRepositoryImpl(database.walletDao())
    val settingsService = AppSettingsService(application)
    private val exportService = FinanceExportService()

    // --- REPLAY/RECOMPOSITION STATE ---
    val appTheme = MutableStateFlow(settingsService.theme)
    val currencySymbol = MutableStateFlow(settingsService.currencySymbol)
    val pinProtected = MutableStateFlow(settingsService.isAppLockEnabled())
    val isAppLocked = MutableStateFlow(settingsService.isAppLockEnabled())
    val userNameState = MutableStateFlow(settingsService.userName)
    val userAvatarPathState = MutableStateFlow(settingsService.userAvatarPath)

    // --- DIRECT DB FLOWS ---
    val accounts: StateFlow<List<Account>> = repository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<TransactionWithDetails>> = repository.getTransactionsWithDetails()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<Budget>> = repository.getAllBudgets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goals: StateFlow<List<Goal>> = repository.getAllGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- FILTER STATES ---
    val searchQuery = MutableStateFlow("")
    val filterCategoryId = MutableStateFlow<Int?>(null) // null = ALL
    val filterAccountId = MutableStateFlow<Int?>(null) // null = ALL
    val filterType = MutableStateFlow("ALL") // ALL, INCOME, EXPENSE, TRANSFER
    val filterSortOrder = MutableStateFlow("DATE_DESC") // DATE_DESC, DATE_ASC, AMOUNT_DESC, AMOUNT_ASC

    // --- DATE RANGE FILTERS ---
    val filterDateRange = MutableStateFlow("ALL") // ALL, THIS_MONTH, LAST_MONTH, THIS_YEAR, CUSTOM
    val filterCustomStartDate = MutableStateFlow<Long?>(null)
    val filterCustomEndDate = MutableStateFlow<Long?>(null)

    // Intermediate trigger flows to avoid the Flow combine() 5-limit bottleneck
    private val basicFiltersFlow: Flow<BasicFilters> = combine(
        searchQuery,
        filterCategoryId,
        filterAccountId,
        filterType
    ) { query, catId, accId, type ->
        BasicFilters(query, catId, accId, type)
    }

    private val displayFiltersFlow: Flow<DisplayFilters> = combine(
        filterSortOrder,
        filterDateRange,
        filterCustomStartDate,
        filterCustomEndDate
    ) { sort, dateRange, customStart, customEnd ->
        DisplayFilters(sort, dateRange, customStart, customEnd)
    }

    // --- FILTERED TRANSACTIONS ---
    val filteredTransactions: StateFlow<List<TransactionWithDetails>> = combine(
        transactions,
        basicFiltersFlow,
        displayFiltersFlow
    ) { txList, basic, display ->
        var list = txList

        // 1. Text Search query
        if (basic.query.isNotEmpty()) {
            list = list.filter {
                (it.transaction.note.contains(basic.query, ignoreCase = true)) ||
                (it.transaction.tags.contains(basic.query, ignoreCase = true)) ||
                (it.categoryName?.contains(basic.query, ignoreCase = true) == true) ||
                (it.accountName.contains(basic.query, ignoreCase = true)) ||
                (it.transaction.amount.toString().contains(basic.query))
            }
        }

        // 2. Category Filter
        if (basic.catId != null && basic.catId != 0) {
            list = list.filter { it.transaction.categoryId == basic.catId }
        }

        // 3. Account Filter
        if (basic.accId != null && basic.accId != 0) {
            list = list.filter { it.transaction.accountId == basic.accId || it.transaction.toAccountId == basic.accId }
        }

        // 4. Transaction Type
        if (basic.type != "ALL") {
            list = list.filter { it.transaction.type == basic.type }
        }

        // 5. Date Filter
        val cal = Calendar.getInstance()
        val now = System.currentTimeMillis()

        list = when (display.dateRange) {
            "THIS_MONTH" -> {
                cal.timeInMillis = now
                val currentMonth = cal.get(Calendar.MONTH)
                val currentYear = cal.get(Calendar.YEAR)
                list.filter {
                    cal.timeInMillis = it.transaction.date
                    cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
                }
            }
            "LAST_MONTH" -> {
                cal.timeInMillis = now
                cal.add(Calendar.MONTH, -1)
                val lastMonth = cal.get(Calendar.MONTH)
                val lastYear = cal.get(Calendar.YEAR)
                list.filter {
                    cal.timeInMillis = it.transaction.date
                    cal.get(Calendar.MONTH) == lastMonth && cal.get(Calendar.YEAR) == lastYear
                }
            }
            "THIS_YEAR" -> {
                cal.timeInMillis = now
                val currentYear = cal.get(Calendar.YEAR)
                list.filter {
                    cal.timeInMillis = it.transaction.date
                    cal.get(Calendar.YEAR) == currentYear
                }
            }
            "CUSTOM" -> {
                val s = display.customStart ?: 0L
                val e = display.customEnd ?: Long.MAX_VALUE
                list.filter { it.transaction.date in s..e }
            }
            else -> list // ALL
        }

        // 6. Sorting
        list = when (display.sort) {
            "DATE_ASC" -> list.sortedWith(compareBy<TransactionWithDetails> { it.transaction.date }.thenBy { it.transaction.id })
            "AMOUNT_DESC" -> list.sortedByDescending { it.transaction.amount }
            "AMOUNT_ASC" -> list.sortedBy { it.transaction.amount }
            else -> list // Already is DATE_DESC ordered by query
        }

        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- MUTATION METHODS ---

    fun updateTheme(newTheme: String) {
        settingsService.theme = newTheme
        appTheme.value = newTheme
    }

    fun updateCurrency(newCurrency: String) {
        settingsService.currency = newCurrency
        currencySymbol.value = settingsService.currencySymbol
    }

    fun enablePinPattern(pin: String) {
        settingsService.pinCode = pin
        pinProtected.value = true
        isAppLocked.value = false
    }

    fun disablePin() {
        settingsService.pinCode = null
        pinProtected.value = false
        isAppLocked.value = false
    }

    fun verifyPin(enteredPin: String): Boolean {
        return if (settingsService.pinCode == enteredPin) {
            isAppLocked.value = false
            true
        } else {
            false
        }
    }

    // --- DATABASE MUTATIONS ---

    // 1. Account mutations with safety
    fun addAccount(name: String, type: String, initBalance: Double, icon: String, color: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertAccount(Account(name = name, type = type, balance = initBalance, icon = icon, color = color))
        }
    }

    fun updateAccountDetails(account: Account) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateAccount(account)
        }
    }

    fun editAccount(id: Int, name: String, type: String, balance: Double, icon: String, color: Int) {
        updateAccountDetails(Account(id = id, name = name, type = type, balance = balance, icon = icon, color = color))
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAccount(account)
        }
    }

    // 2. Custom categories mutations
    fun addCategory(name: String, type: String, icon: String, color: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertCategory(Category(name = name, type = type, icon = icon, color = color))
        }
    }

    fun updateCategoryDetails(category: Category) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCategory(category)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCategory(category)
        }
    }

    // 3. Transactions CRUD with Auto Balance Adjustment
    fun addTransaction(amount: Double, type: String, accountId: Int, toAccountId: Int?, categoryId: Int?, date: Long, note: String, tags: String, attachmentPath: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val tx = Transaction(
                amount = amount,
                type = type,
                accountId = accountId,
                toAccountId = toAccountId,
                categoryId = categoryId,
                date = date,
                note = note,
                tags = tags,
                attachmentPath = attachmentPath
            )
            repository.insertTransaction(tx)

            // Adjust account balances
            adjustAccountBalance(accountId, type, amount, isReversal = false, isToAccount = false)
            if (type == "TRANSFER" && toAccountId != null) {
                adjustAccountBalance(toAccountId, type, amount, isReversal = false, isToAccount = true)
            }
        }
    }

    fun editTransaction(oldTx: Transaction, newTx: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Revert Old Transaction's balance effects
            adjustAccountBalance(oldTx.accountId, oldTx.type, oldTx.amount, isReversal = true, isToAccount = false)
            if (oldTx.type == "TRANSFER" && oldTx.toAccountId != null) {
                adjustAccountBalance(oldTx.toAccountId, oldTx.type, oldTx.amount, isReversal = true, isToAccount = true)
            }

            // 2. Insert/Update New Transaction
            repository.insertTransaction(newTx) // id is the same, so it will overwrite safely

            // 3. Apply New Transaction's balance effects
            adjustAccountBalance(newTx.accountId, newTx.type, newTx.amount, isReversal = false, isToAccount = false)
            if (newTx.type == "TRANSFER" && newTx.toAccountId != null) {
                adjustAccountBalance(newTx.toAccountId, newTx.type, newTx.amount, isReversal = false, isToAccount = true)
            }
        }
    }

    fun editTransaction(id: Int, amount: Double, type: String, accountId: Int, toAccountId: Int?, categoryId: Int?, date: Long, note: String, tags: String, attachmentPath: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val oldTx = repository.getTransactionById(id) ?: return@launch
            val newTx = oldTx.copy(
                id = id,
                amount = amount,
                type = type,
                accountId = accountId,
                toAccountId = toAccountId,
                categoryId = categoryId,
                date = date,
                note = note,
                tags = tags,
                attachmentPath = attachmentPath
            )
            editTransaction(oldTx, newTx)
        }
    }

    fun removeTransaction(tx: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Reverse the balance effects
            adjustAccountBalance(tx.accountId, tx.type, tx.amount, isReversal = true, isToAccount = false)
            if (tx.type == "TRANSFER" && tx.toAccountId != null) {
                adjustAccountBalance(tx.toAccountId, tx.type, tx.amount, isReversal = true, isToAccount = true)
            }

            // 2. Delete transaction from DB
            repository.deleteTransaction(tx)
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val tx = repository.getTransactionById(id) ?: return@launch
            removeTransaction(tx)
        }
    }

    private suspend fun adjustAccountBalance(accId: Int, txType: String, amount: Double, isReversal: Boolean, isToAccount: Boolean) {
        val account = repository.getAccountById(accId) ?: return
        var diff = 0.0

        if (!isReversal) {
            // normal adjustment
            when (txType) {
                "INCOME" -> diff = amount
                "EXPENSE" -> diff = -amount
                "TRANSFER" -> {
                    diff = if (isToAccount) amount else -amount
                }
            }
        } else {
            // reversal adjustment (opposite)
            when (txType) {
                "INCOME" -> diff = -amount
                "EXPENSE" -> diff = amount
                "TRANSFER" -> {
                    diff = if (isToAccount) -amount else amount
                }
            }
        }

        val updatedAccount = account.copy(balance = account.balance + diff)
        repository.updateAccount(updatedAccount)
    }

    // Transfers between accounts quickly
    fun createTransfer(fromAccId: Int, toAccId: Int, amount: Double, note: String) {
        addTransaction(
            amount = amount,
            type = "TRANSFER",
            accountId = fromAccId,
            toAccountId = toAccId,
            categoryId = null,
            date = System.currentTimeMillis(),
            note = note,
            tags = "transfer",
            attachmentPath = null
        )
    }

    // 4. Budgets CRUD
    fun addOrUpdateBudget(categoryId: Int, limitAmount: Double, month: Int, year: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            // Check if budget already exists for this category/month/year
            val currentList = repository.getBudgetsForMonth(month, year).first()
            val existing = currentList.find { it.categoryId == categoryId }
            if (existing != null) {
                repository.updateBudget(existing.copy(limitAmount = limitAmount))
            } else {
                repository.insertBudget(Budget(categoryId = categoryId, limitAmount = limitAmount, month = month, year = year))
            }
        }
    }

    fun addBudget(categoryId: Int, limitAmount: Double, month: Int, year: Int) =
        addOrUpdateBudget(categoryId, limitAmount, month, year)

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBudget(budget)
        }
    }

    // 5. Goals mutations
    fun addGoal(name: String, targetAmount: Double, deadline: Long, currentAmount: Double = 0.0) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertGoal(Goal(name = name, targetAmount = targetAmount, currentAmount = currentAmount, deadline = deadline))
        }
    }

    fun updateGoalDetails(goal: Goal) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateGoal(goal)
        }
    }

    fun contributeToGoal(goalId: Int, contributionAmount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val goal = repository.getGoalById(goalId) ?: return@launch
            val updated = goal.copy(currentAmount = goal.currentAmount + contributionAmount)
            repository.updateGoal(updated)
        }
    }

    fun addGoalContribution(goalId: Int, contributionAmount: Double) =
        contributeToGoal(goalId, contributionAmount)

    fun deleteGoal(goal: Goal) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteGoal(goal)
        }
    }

    fun setupPin(pinCode: String) {
        enablePinPattern(pinCode)
    }

    fun exportBackupJson(onComplete: (String?) -> Unit) {
        backupData(onComplete)
    }

    fun updateProfile(name: String, avatarPath: String?) {
        settingsService.userName = name
        settingsService.userAvatarPath = avatarPath
        userNameState.value = name
        userAvatarPathState.value = avatarPath
    }

    // --- LOCAL BACKUP ACTIONS ---
    fun backupData(onComplete: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val json = exportService.exportToJson(database)
                onComplete(json)
            } catch (e: Exception) {
                onComplete(null)
            }
        }
    }

    fun restoreData(jsonString: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = exportService.importFromJson(database, jsonString)
            onComplete(success)
        }
    }

    fun exportTransactionsToCsv(onComplete: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val csv = exportService.exportToCsv(database)
                onComplete(csv)
            } catch (e: Exception) {
                onComplete(null)
            }
        }
    }
}
