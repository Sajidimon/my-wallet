package com.example.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.domain.model.Account
import com.example.domain.model.Category
import com.example.domain.model.Transaction
import com.example.domain.model.Budget
import com.example.domain.model.Goal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Database(
    entities = [Account::class, Category::class, Transaction::class, Budget::class, Goal::class],
    version = 1,
    exportSchema = false
)
abstract class WalletDatabase : RoomDatabase() {

    abstract fun walletDao(): WalletDao

    companion object {
        @Volatile
        internal var INSTANCE: WalletDatabase? = null

        fun getInstance(context: Context, coroutineScope: CoroutineScope): WalletDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WalletDatabase::class.java,
                    "wallet_database"
                )
                .addCallback(DatabaseCallback(coroutineScope))
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDefaultData(database.walletDao())
                }
            }
        }

        private suspend fun populateDefaultData(dao: WalletDao) {
            // --- Seed Default Accounts ---
            val cashAccId = dao.insertAccount(
                Account(
                    name = "Cash Wallet",
                    type = "CASH",
                    balance = 350.0,
                    icon = "payments",
                    color = 0xFF2ECC71.toInt() // Emerald Green
                )
            ).toInt()

            val bankAccId = dao.insertAccount(
                Account(
                    name = "Main Savings (Bank)",
                    type = "BANK",
                    balance = 4850.0,
                    icon = "account_balance",
                    color = 0xFF3498DB.toInt() // Blue
                )
            ).toInt()

            val cardAccId = dao.insertAccount(
                Account(
                    name = "Credit Card",
                    type = "CREDIT_CARD",
                    balance = -120.0,
                    icon = "credit_card",
                    color = 0xFFE74C3C.toInt() // Alizarin Red
                )
            ).toInt()

            // --- Seed Default Income Categories ---
            val catSalary = dao.insertCategory(Category(name = "Salary", type = "INCOME", icon = "payments", color = 0xFF2ECC71.toInt()))
            val catBonus = dao.insertCategory(Category(name = "Bonus", type = "INCOME", icon = "redeem", color = 0xFFF1C40F.toInt()))
            val catInvest = dao.insertCategory(Category(name = "Investment", type = "INCOME", icon = "trending_up", color = 0xFF3498DB.toInt()))
            val catGift = dao.insertCategory(Category(name = "Gift", type = "INCOME", icon = "card_giftcard", color = 0xFFE91E63.toInt()))
            val catIncOther = dao.insertCategory(Category(name = "Other Income", type = "INCOME", icon = "add_circle_outline", color = 0xFF95A5A6.toInt()))

            // --- Seed Default Expense Categories ---
            val catFood = dao.insertCategory(Category(name = "Food & Dining", type = "EXPENSE", icon = "restaurant", color = 0xFFE67E22.toInt()))
            val catTransport = dao.insertCategory(Category(name = "Transport", type = "EXPENSE", icon = "directions_car", color = 0xFF1ABC9C.toInt()))
            val catShopping = dao.insertCategory(Category(name = "Shopping", type = "EXPENSE", icon = "shopping_bag", color = 0xFF9B59B6.toInt()))
            val catBills = dao.insertCategory(Category(name = "Bills & Utilities", type = "EXPENSE", icon = "receipt_long", color = 0xFFE74C3C.toInt()))
            val catEnt = dao.insertCategory(Category(name = "Entertainment", type = "EXPENSE", icon = "sports_esports", color = 0xFF34495E.toInt()))
            val catHealth = dao.insertCategory(Category(name = "Health & Medical", type = "EXPENSE", icon = "medical_services", color = 0xFFE74C3C.toInt()))
            val catEdu = dao.insertCategory(Category(name = "Education", type = "EXPENSE", icon = "school", color = 0xFF3498DB.toInt()))
            val catTravel = dao.insertCategory(Category(name = "Travel", type = "EXPENSE", icon = "flight", color = 0xFF1ABC9C.toInt()))
            val catRent = dao.insertCategory(Category(name = "Rent & Housing", type = "EXPENSE", icon = "home", color = 0xFFF39C12.toInt()))
            val catSub = dao.insertCategory(Category(name = "Subscriptions", type = "EXPENSE", icon = "subscriptions", color = 0xFF8E44AD.toInt()))
            val catExpOther = dao.insertCategory(Category(name = "Other", type = "EXPENSE", icon = "more_horiz", color = 0xFF7F8C8D.toInt()))

            // --- Seed Calendar/Transaction History (Past Month) ---
            val today = Calendar.getInstance()
            
            // Helping function to get time relative to today
            fun timeAgo(days: Int, hour: Int, minute: Int): Long {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -days)
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                return cal.timeInMillis
            }

            // 1. Salary Transaction (Income, 15 days ago)
            dao.insertTransaction(
                Transaction(
                    amount = 3200.0,
                    type = "INCOME",
                    accountId = bankAccId,
                    categoryId = catSalary.toInt(),
                    date = timeAgo(15, 9, 30),
                    note = "Monthly base salary payout"
                )
            )

            // 2. Rent Transaction (Expense, 6 days ago)
            dao.insertTransaction(
                Transaction(
                    amount = 850.0,
                    type = "EXPENSE",
                    accountId = bankAccId,
                    categoryId = catRent.toInt(),
                    date = timeAgo(6, 12, 0),
                    note = "June Studio Rent"
                )
            )

            // 3. Grocery Shopping (Expense, 1 day ago)
            dao.insertTransaction(
                Transaction(
                    amount = 76.50,
                    type = "EXPENSE",
                    accountId = cashAccId,
                    categoryId = catFood.toInt(),
                    date = timeAgo(1, 18, 45),
                    note = "Weekly groceries at Trader Joe's",
                    tags = "groceries,food"
                )
            )

            // 4. Transport/Uber (Expense, 2 days ago)
            dao.insertTransaction(
                Transaction(
                    amount = 18.25,
                    type = "EXPENSE",
                    accountId = cardAccId,
                    categoryId = catTransport.toInt(),
                    date = timeAgo(2, 10, 15),
                    note = "Uber ride to office",
                    tags = "commute"
                )
            )

            // 5. Subscription/Netflix (Expense, 10 days ago)
            dao.insertTransaction(
                Transaction(
                    amount = 15.49,
                    type = "EXPENSE",
                    accountId = cardAccId,
                    categoryId = catSub.toInt(),
                    date = timeAgo(10, 8, 0),
                    note = "Netflix Premium Standard",
                    tags = "entertainment,recurring"
                )
            )

            // 6. Transfer Cash to Bank (Transfer, 4 days ago)
            dao.insertTransaction(
                Transaction(
                    amount = 100.0,
                    type = "TRANSFER",
                    accountId = cashAccId,
                    toAccountId = bankAccId,
                    date = timeAgo(4, 15, 30),
                    note = "Deposit leftover cash"
                )
            )

            // 7. Coffee/Dining out (Expense, Today!)
            dao.insertTransaction(
                Transaction(
                    amount = 6.75,
                    type = "EXPENSE",
                    accountId = cashAccId,
                    categoryId = catFood.toInt(),
                    date = System.currentTimeMillis() - 3600000, // 1 hour ago
                    note = "Espresso and croissant at Blue Bottle",
                    tags = "coffee,treat"
                )
            )

            // --- Seed Sample Budgets ---
            dao.insertBudget(Budget(categoryId = catFood.toInt(), limitAmount = 400.0, month = today.get(Calendar.MONTH) + 1, year = today.get(Calendar.YEAR)))
            dao.insertBudget(Budget(categoryId = catTransport.toInt(), limitAmount = 150.0, month = today.get(Calendar.MONTH) + 1, year = today.get(Calendar.YEAR)))
            dao.insertBudget(Budget(categoryId = catBills.toInt(), limitAmount = 300.0, month = today.get(Calendar.MONTH) + 1, year = today.get(Calendar.YEAR)))
            // Overall monthly budget limit
            dao.insertBudget(Budget(categoryId = 0, limitAmount = 2500.0, month = today.get(Calendar.MONTH) + 1, year = today.get(Calendar.YEAR)))

            // --- Seed Sample Goals ---
            dao.insertGoal(Goal(name = "Emergency Fund", targetAmount = 5000.0, currentAmount = 2500.0, deadline = System.currentTimeMillis() + 180 * 24 * 60 * 60 * 1000L)) // 6 months deadline
            dao.insertGoal(Goal(name = "Summer Trip to Tokyo", targetAmount = 3000.0, currentAmount = 1200.0, deadline = System.currentTimeMillis() + 90 * 24 * 60 * 60 * 1000L)) // 3 months deadline
        }
    }
}
