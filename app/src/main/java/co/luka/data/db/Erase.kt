package co.luka.data.db

import androidx.room.withTransaction

suspend fun LukaDatabase.eraseAll() {
    withTransaction {
        val db = openHelper.writableDatabase
        listOf(
            "goal_contributions",
            "goals",
            "budgets",
            "subscriptions",
            "movements",
            "captures",
            "merchants",
            "categories",
            "sources",
        ).forEach { table -> db.execSQL("DELETE FROM $table") }
    }
}
