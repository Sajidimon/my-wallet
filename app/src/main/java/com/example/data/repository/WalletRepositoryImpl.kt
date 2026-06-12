package com.example.data.repository

import com.example.data.local.db.WalletDao
import com.example.domain.model.Account
import com.example.domain.model.Category
import com.example.domain.model.Transaction
import com.example.domain.model.Budget
import com.example.domain.model.Goal
import com.example.domain.model.TransactionWithDetails
import com.example.domain.repository.WalletRepository
import kotlinx.coroutines.flow.Flow

class WalletRepositoryImpl(private val dao: WalletDao) : WalletRepository {

    override fun getAllAccounts(): Flow<List<Account>> = dao.getAllAccounts()

    override suspend fun getAccountById(id: Int): Account? = dao.getAccountById(id)

    override suspend fun insertAccount(account: Account): Long = dao.insertAccount(account)

    override suspend fun updateAccount(account: Account) = dao.updateAccount(account)

    override suspend fun deleteAccount(account: Account) = dao.deleteAccount(account)

    override fun getAllCategories(): Flow<List<Category>> = dao.getAllCategories()

    override suspend fun getCategoryById(id: Int): Category? = dao.getCategoryById(id)

    override suspend fun insertCategory(category: Category): Long = dao.insertCategory(category)

    override suspend fun updateCategory(category: Category) = dao.updateCategory(category)

    override suspend fun deleteCategory(category: Category) = dao.deleteCategory(category)

    override fun getAllTransactions(): Flow<List<Transaction>> = dao.getAllTransactions()

    override fun getTransactionsByAccount(accountId: Int): Flow<List<Transaction>> = dao.getTransactionsByAccount(accountId)

    override fun getTransactionsByCategory(categoryId: Int): Flow<List<Transaction>> = dao.getTransactionsByCategory(categoryId)

    override fun getTransactionsWithDetails(): Flow<List<TransactionWithDetails>> = dao.getTransactionsWithDetails()

    override suspend fun getTransactionById(id: Int): Transaction? = dao.getTransactionById(id)

    override suspend fun insertTransaction(transaction: Transaction): Long = dao.insertTransaction(transaction)

    override suspend fun updateTransaction(transaction: Transaction) = dao.updateTransaction(transaction)

    override suspend fun deleteTransaction(transaction: Transaction) = dao.deleteTransaction(transaction)

    override fun getAllBudgets(): Flow<List<Budget>> = dao.getAllBudgets()

    override fun getBudgetsForMonth(month: Int, year: Int): Flow<List<Budget>> = dao.getBudgetsForMonth(month, year)

    override suspend fun insertBudget(budget: Budget): Long = dao.insertBudget(budget)

    override suspend fun updateBudget(budget: Budget) = dao.updateBudget(budget)

    override suspend fun deleteBudget(budget: Budget) = dao.deleteBudget(budget)

    override fun getAllGoals(): Flow<List<Goal>> = dao.getAllGoals()

    override suspend fun getGoalById(id: Int): Goal? = dao.getGoalById(id)

    override suspend fun insertGoal(goal: Goal): Long = dao.insertGoal(goal)

    override suspend fun updateGoal(goal: Goal) = dao.updateGoal(goal)

    override suspend fun deleteGoal(goal: Goal) = dao.deleteGoal(goal)
}
