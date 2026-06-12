package com.example.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Embedded

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // CASH, BANK, CREDIT_CARD, SAVINGS, CUSTOM
    val balance: Double,
    val icon: String,
    val color: Int, // Hex integer
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // INCOME, EXPENSE
    val icon: String,
    val color: Int
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val type: String, // INCOME, EXPENSE, TRANSFER
    val accountId: Int,
    val toAccountId: Int? = null, // Used if type == TRANSFER
    val categoryId: Int? = null, // Nullable for transfers
    val date: Long = System.currentTimeMillis(),
    val note: String = "",
    val tags: String = "", // Comma-separated tags
    val attachmentPath: String? = null
)

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoryId: Int, // 0 means general/overall budget, otherwise specific category
    val limitAmount: Double, // Renamed from "limit" because limit is a SQL keyword! Very important to avoid SQL parsing errors.
    val month: Int, // 1 to 12
    val year: Int
)

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val deadline: Long
)

// Flat structure to show transactions with details in UI without complex manual loading of related entities in memory
data class TransactionWithDetails(
    val id: Int,
    val amount: Double,
    val type: String, // INCOME, EXPENSE, TRANSFER
    val accountId: Int,
    val toAccountId: Int?,
    val categoryId: Int?,
    val date: Long,
    val note: String,
    val tags: String,
    val attachmentPath: String?,
    val accountName: String,
    val accountColor: Int,
    val toAccountName: String?,
    val toAccountColor: Int?,
    val categoryName: String?,
    val categoryIcon: String?,
    val categoryColor: Int?
) {
    // Utility getter to keep compatibility with existing ViewModel transaction references
    val transaction: Transaction 
        get() = Transaction(
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
}
