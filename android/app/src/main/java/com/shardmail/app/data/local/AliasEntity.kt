package com.shardmail.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "aliases")
data class AliasEntity(
    @PrimaryKey
    val id: String,
    val prefix: String,
    val address: String,
    @ColumnInfo(name = "created_at")
    val createdAt: String
)
