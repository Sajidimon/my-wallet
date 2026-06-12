package com.example.services

import android.content.Context
import com.example.data.local.db.*
import com.example.domain.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FinanceExportService {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // Data class representing entire backup contents
    data class BackupPayload(
        val accounts: List<Account> = emptyList(),
        val categories: List<Category> = emptyList(),
        val transactions: List<Transaction> = emptyList(),
        val budgets: List<Budget> = emptyList(),
        val goals: List<Goal> = emptyList()
    )

    // Export entire database to a clean JSON string
    suspend fun exportToJson(database: WalletDatabase): String = withContext(Dispatchers.IO) {
        val dao = database.walletDao()
        val accounts = dao.getAllAccounts().first()
        val categories = dao.getAllCategories().first()
        val transactions = dao.getAllTransactions().first()
        val budgets = dao.getAllBudgets().first()
        val goals = dao.getAllGoals().first()

        val payload = BackupPayload(accounts, categories, transactions, budgets, goals)
        val adapter = moshi.adapter(BackupPayload::class.java)
        adapter.indent("  ").toJson(payload)
    }

    // Import database from a JSON string, running inside a database transaction to ensure safety
    suspend fun importFromJson(database: WalletDatabase, jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val adapter = moshi.adapter(BackupPayload::class.java)
            val payload = adapter.fromJson(jsonString) ?: return@withContext false

            // To do a safer sync, we compile a transaction manually
            database.runInTransaction {
                    // Let's clear everything first
                    // SQLite handles clearing safely if there are no strict FK constraints. Room exportSchema is false.
                    dbExecute(database, "DELETE FROM transactions")
                    dbExecute(database, "DELETE FROM accounts")
                    dbExecute(database, "DELETE FROM categories")
                    dbExecute(database, "DELETE FROM budgets")
                    dbExecute(database, "DELETE FROM goals")

                    // Insert accounts with explicit original ID
                    payload.accounts.forEach { account ->
                        dbExecute(database, 
                            "INSERT INTO accounts (id, name, type, balance, icon, color, createdAt) VALUES (?, ?, ?, ?, ?, ?, ?)",
                            arrayOf(account.id, account.name, account.type, account.balance, account.icon, account.color, account.createdAt)
                        )
                    }

                    // Insert categories
                    payload.categories.forEach { category ->
                        dbExecute(database,
                            "INSERT INTO categories (id, name, type, icon, color) VALUES (?, ?, ?, ?, ?)",
                            arrayOf(category.id, category.name, category.type, category.icon, category.color)
                        )
                    }

                    // Insert transactions
                    payload.transactions.forEach { tx ->
                        dbExecute(database,
                            "INSERT INTO transactions (id, amount, type, accountId, toAccountId, categoryId, date, note, tags, attachmentPath) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            arrayOf(tx.id, tx.amount, tx.type, tx.accountId, tx.toAccountId, tx.categoryId, tx.date, tx.note, tx.tags, tx.attachmentPath)
                        )
                    }

                    // Insert budgets
                    payload.budgets.forEach { budget ->
                        dbExecute(database,
                            "INSERT INTO budgets (id, categoryId, limitAmount, month, year) VALUES (?, ?, ?, ?, ?)",
                            arrayOf(budget.id, budget.categoryId, budget.limitAmount, budget.month, budget.year)
                        )
                    }

                    // Insert goals
                    payload.goals.forEach { goal ->
                        dbExecute(database,
                            "INSERT INTO goals (id, name, targetAmount, currentAmount, deadline) VALUES (?, ?, ?, ?, ?)",
                            arrayOf(goal.id, goal.name, goal.targetAmount, goal.currentAmount, goal.deadline)
                        )
                    }
                }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun dbExecute(database: WalletDatabase, sql: String, bindArgs: Array<Any?> = emptyArray()) {
        val stmt = database.compileStatement(sql)
        for (i in bindArgs.indices) {
            val arg = bindArgs[i]
            if (arg == null) {
                stmt.bindNull(i + 1)
            } else when (arg) {
                is Double -> stmt.bindDouble(i + 1, arg)
                is Float -> stmt.bindDouble(i + 1, arg.toDouble())
                is Long -> stmt.bindLong(i + 1, arg)
                is Int -> stmt.bindLong(i + 1, arg.toLong())
                is Short -> stmt.bindLong(i + 1, arg.toLong())
                is Byte -> stmt.bindLong(i + 1, arg.toLong())
                is Boolean -> stmt.bindLong(i + 1, if (arg) 1L else 0L)
                else -> stmt.bindString(i + 1, arg.toString())
            }
        }
        stmt.executeInsert()
    }

    // Export transaction list with detailed info to a clean CSV layout (Excel compatible)
    suspend fun exportToCsv(database: WalletDatabase): String = withContext(Dispatchers.IO) {
        val dao = database.walletDao()
        val txs = dao.getTransactionsWithDetails().first()

        val sb = java.lang.StringBuilder()
        // Header
        sb.append("ID,Date,Type,Amount,From Account,To Account,Category,Notes,Tags\n")

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        for (item in txs) {
            val dateStr = sdf.format(Date(item.transaction.date))
            val amountStr = item.transaction.amount.toString()
            val noteCleaned = (item.transaction.note ?: "").replace("\"", "\"\"")
            val tagsCleaned = (item.transaction.tags ?: "").replace("\"", "\"\"")

            sb.append(item.transaction.id).append(",")
            sb.append("\"").append(dateStr).append("\",")
            sb.append("\"").append(item.transaction.type).append("\",")
            sb.append(amountStr).append(",")
            sb.append("\"").append(item.accountName).append("\",")
            sb.append("\"").append(item.toAccountName ?: "").append("\",")
            sb.append("\"").append(item.categoryName ?: "").append("\",")
            sb.append("\"").append(noteCleaned).append("\",")
            sb.append("\"").append(tagsCleaned).append("\"\n")
        }

        sb.toString()
    }
}
