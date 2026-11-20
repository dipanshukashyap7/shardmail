package com.shardmail.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "from_addr")
    val fromAddr: String,
    @ColumnInfo(name = "to_addr")
    val toAddr: String,
    val subject: String,
    val text: String,
    val html: String,
    val code: String?,
    val date: String,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    val read: Int = 0
)
