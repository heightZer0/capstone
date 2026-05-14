package com.example.capstone.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "result")
data class ResultEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "analysis_id")
    val analysisId: Int = 0,

    @ColumnInfo(name = "result_code")
    val resultCode: String,

    @ColumnInfo(name = "total_pouches")
    val totalPouches: Int = 0,

    @ColumnInfo(name = "error_count")
    val errorCount: Int = 0,

    @ColumnInfo(name = "summary_text")
    val summaryText: String? = null,

    @ColumnInfo(name = "analyzed_time")
    val analyzedTime: Long = System.currentTimeMillis()
)