package com.example.data.local.db

import androidx.room.*
import com.example.domain.model.Account
import com.example.domain.model.Category
import com.example.domain.model.Transaction
import com.example.domain.model.Budget
import com.example.domain.model.Goal
import com.example.domain.model.TransactionWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {

    // --- ACCOUNTS ---
    @Query("SELECT * FROM accounts ORDER BY name ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Int): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account): Long

    @Update
    suspend fun updateAccount(account: Account)

    @Delete
    suspend fun deleteAccount(account: Account)


    // --- CATEGORIES ---
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Int): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)


    // --- TRANSACTIONS ---
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId OR toAccountId = :accountId ORDER BY date DESC, id DESC")
    fun getTransactionsByAccount(accountId: Int): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY date DESC, id DESC")
    fun getTransactionsByCategory(categoryId: Int): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Int): Transaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    // Joined transaction query to fetch detail view models reactively
    @Query("""
        SELECT 
            t.id AS id, t.amount AS amount, t.type AS type, t.accountId AS accountId, 
            t.toAccountId AS toAccountId, t.categoryId AS categoryId, t.date AS date, 
            t.note AS note, t.tags AS tags, t.attachmentPath AS attachmentPath,
            a.name AS accountName, 
            a.color AS accountColor,
            a2.name AS toAccountName,
            a2.color AS toAccountColor,
            c.name AS categoryName,
            c.icon AS categoryIcon,
            c.color AS categoryColor
        FROM transactions t
        INNER JOIN accounts a ON t.accountId = a.id
        LEFT JOIN accounts a2 ON t.toAccountId = a2.id
        LEFT JOIN categories c ON t.categoryId = c.id
        ORDER BY t.date DESC, t.id DESC
    """)
    fun getTransactionsWithDetails(): Flow<List<TransactionWithDetails>>


    // --- BUDGETS ---
    @Query("SELECT * FROM budgets ORDER BY year DESC, month DESC")
    fun getAllBudgets(): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE month = :month AND year = :year")
    fun getBudgetsForMonth(month: Int, year: Int): Flow<List<Budget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget): Long

    @Update
    suspend fun updateBudget(budget: Budget)

    @Delete
    suspend fun deleteBudget(budget: Budget)


    // --- GOALS ---
    @Query("SELECT * FROM goals ORDER BY deadline ASC")
    fun getAllGoals(): Flow<List<Goal>>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getGoalById(id: Int): Goal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal): Long

    @Update
    suspend fun updateGoal(goal: Goal)

    @Delete
    suspend fun deleteGoal(goal: Goal)
}
