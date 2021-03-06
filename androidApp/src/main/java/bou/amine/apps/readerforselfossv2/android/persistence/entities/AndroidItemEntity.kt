package bou.amine.apps.readerforselfossv2.android.persistence.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import bou.amine.apps.readerforselfossv2.rest.SelfossModel

@Entity(tableName = "items")
data class AndroidItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "datetime")
    val datetime: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "content")
    val content: String,
    @ColumnInfo(name = "unread")
    val unread: Boolean,
    @ColumnInfo(name = "starred")
    var starred: Boolean,
    @ColumnInfo(name = "thumbnail")
    val thumbnail: String?,
    @ColumnInfo(name = "icon")
    val icon: String?,
    @ColumnInfo(name = "link")
    val link: String,
    @ColumnInfo(name = "sourcetitle")
    val sourcetitle: String,
    @ColumnInfo(name = "tags")
    val tags: String
)