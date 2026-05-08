package com.example.capstone.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "error_code")
data class ErrorCodeEntity(
    @PrimaryKey
    @ColumnInfo(name = "error_code")
    val errorCode: String,

    @ColumnInfo(name = "error_name")
    val errorName: String,

    @ColumnInfo(name = "error_desc")
    val errorDesc: String? = null
)