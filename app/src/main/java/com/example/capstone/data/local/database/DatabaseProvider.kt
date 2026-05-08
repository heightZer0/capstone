package com.example.capstone.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.capstone.data.local.entity.ErrorCodeEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DatabaseProvider {

    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "capstone_database"
            )
                .fallbackToDestructiveMigration()
                .addCallback(roomCallback)
                .build()

            INSTANCE = instance
            instance
        }
    }

    private val roomCallback = object : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)

            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    database.errorCodeDao().insertErrorCodes(
                        listOf(
                            ErrorCodeEntity(
                                errorCode = "CLR",
                                errorName = "색상 오류",
                                errorDesc = "약 또는 포지의 색상이 기준과 다른 경우"
                            ),
                            ErrorCodeEntity(
                                errorCode = "CNT",
                                errorName = "개수 오류",
                                errorDesc = "포지 안의 약 개수가 기준과 다른 경우"
                            ),
                            ErrorCodeEntity(
                                errorCode = "FIG",
                                errorName = "형태 오류",
                                errorDesc = "약의 모양이 기준과 다른 경우"
                            )
                        )
                    )
                }
            }
        }
    }
}