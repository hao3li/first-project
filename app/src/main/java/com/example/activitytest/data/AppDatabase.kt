package com.example.activitytest.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Room数据库类
@Database(
    // 根据Task实体类创建对应数据表
    entities = [Task::class, TaskHistory::class],
    version = 5,
    exportSchema = false  // 不导出数据库结构
)
abstract class AppDatabase : RoomDatabase() {  // 数据库抽象类

    abstract fun taskDao(): TaskDao     // 任务数据库访问对象
    abstract fun taskHistoryDao(): TaskHistoryDao

    companion object {    // 单例模式，保证数据库对象只创建一次
        @Volatile      // 多线程环境下保证线程安全
        private var INSTANCE: AppDatabase? = null   // 单例对象

        private val MIGRATION_1_2 = object : Migration(1, 2) {   //使用单例模式
            override fun migrate(database: SupportSQLiteDatabase) {
                val columns = mutableSetOf<String>()
                database.query("PRAGMA table_info(`tasks`)").use { cursor ->
                    val nameIndex = cursor.getColumnIndexOrThrow("name")
                    while (cursor.moveToNext()) {
                        columns += cursor.getString(nameIndex)
                    }
                }

                if ("content" !in columns) {
                    database.execSQL(
                        "ALTER TABLE `tasks` ADD COLUMN `content` TEXT NOT NULL DEFAULT ''"
                    )
                }
                if ("deadline" !in columns) {
                    database.execSQL(
                        "ALTER TABLE `tasks` ADD COLUMN `deadline` INTEGER"
                    )
                }
                if ("isCompleted" !in columns) {
                    database.execSQL(
                        "ALTER TABLE `tasks` ADD COLUMN `isCompleted` INTEGER NOT NULL DEFAULT 0"
                    )
                }
                if ("createdAt" !in columns) {
                    database.execSQL(
                        "ALTER TABLE `tasks` ADD COLUMN `createdAt` INTEGER NOT NULL DEFAULT 0"
                    )
                }
                if ("updatedAt" !in columns) {
                    database.execSQL(
                        "ALTER TABLE `tasks` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0"
                    )
                }
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val columns = mutableSetOf<String>()
                database.query("PRAGMA table_info(`tasks`)").use { cursor ->
                    val nameIndex = cursor.getColumnIndexOrThrow("name")
                    while (cursor.moveToNext()) {
                        columns += cursor.getString(nameIndex)
                    }
                }

                if ("description" !in columns) {
                    database.execSQL(
                        "ALTER TABLE `tasks` ADD COLUMN `description` TEXT NOT NULL DEFAULT ''"
                    )
                }
                if ("priority" !in columns) {
                    database.execSQL(
                        "ALTER TABLE `tasks` ADD COLUMN `priority` INTEGER NOT NULL DEFAULT 0"
                    )
                }
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `task_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `originalTaskId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `deadline` INTEGER,
                        `isCompleted` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `deletedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE `task_history` ADD COLUMN `description` TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE `task_history` ADD COLUMN `priority` INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        // 获取数据库实例
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "study_task_database"
                ).addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5
                ).build().also {    // 创建数据库
                    INSTANCE = it   // 保存数据库对象
                }
            }
        }
    }
}