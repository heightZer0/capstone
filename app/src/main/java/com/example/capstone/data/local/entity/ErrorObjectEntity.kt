package com.example.capstone.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "error_object",
    foreignKeys = [
        ForeignKey(
            entity = ResultEntity::class,
            parentColumns = ["analysis_id"],
            childColumns = ["analysis_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ErrorCodeEntity::class,
            parentColumns = ["error_code"],
            childColumns = ["error_code"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["analysis_id"]),
        Index(value = ["error_code"])
    ]
)
data class ErrorObjectEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "error_id")
    val errorId: Int = 0,

    @ColumnInfo(name = "analysis_id")
    val analysisId: Int,

    @ColumnInfo(name = "error_code")
    val errorCode: String,

    @ColumnInfo(name = "pouch_no")
    val pouchNo: Int,

    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,

    @ColumnInfo(name = "created_time")
    val createdTime: Long = System.currentTimeMillis()
)