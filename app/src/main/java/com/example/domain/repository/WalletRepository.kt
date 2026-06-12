package com.example.domain.repository

import com.example.domain.model.Account
import com.example.domain.model.Category
import com.example.domain.model.Transaction
import com.example.domain.model.Budget
import com.example.domain.model.Goal
import com.example.domain.model.TransactionWithDetails
import kotlinx.coroutines.flow.Flow

interface WalletRepository {
    // Accounts
    fun getAllAccounts(): Flow<List<Account>>
    suspend fun getAccountById(id: Int): Account?
    suspend fun insertAccount(account: Account): Long
    suspend fun updateAccount(account: Account)
    suspend fun deleteAccount(account: Account)

    // Categories
    fun getAllCategories(): Flow<List<Category>>
    suspend fun getCategoryById(id: Int): Category?
    suspend fun insertCategory(category: Category): Long
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(category: Category)

    // Transactions
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getTransactionsByAccount(accountId: Int): Flow<List<Transaction>>
    fun getTransactionsByCategory(categoryId: Int): Flow<List<Transaction>>
    fun getTransactionsWithDetails(): Flow<List<TransactionWithDetails>>
    suspend fun getTransactionById(id: Int): Transaction?
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)

    // Budgets
    fun getAllBudgets(): Flow<List<Budget>>
    fun getBudgetsForMonth(month: Int, year: Int): Flow<List<Budget>>
    suspend fun insertBudget(budget: Budget): Long
    suspend fun updateBudget(budget: Budget)
    suspend fun deleteBudget(budget: Budget)

    // Goals
    fun getAllGoals(): Flow<List<Goal>>
    suspend fun getGoalById(id: Int): Goal?
    suspend fun insertGoal(goal: Goal): Long
    suspend fun updateGoal(goal: Goal)
    suspend fun deleteGoal(goal: Goal)
}
