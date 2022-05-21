package bou.amine.apps.readerforselfossv2.android.persistence.dao

import androidx.room.Delete
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import bou.amine.apps.readerforselfossv2.android.persistence.entities.SourceEntity
import bou.amine.apps.readerforselfossv2.android.persistence.entities.TagEntity

@Dao
interface DrawerDataDao {
    @Query("SELECT * FROM tags")
    fun tags(): List<TagEntity>

    @Query("SELECT * FROM sources")
    fun sources(): List<SourceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllTags(vararg tags: TagEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllSources(vararg sources: SourceEntity)

    @Query("DELETE FROM tags")
    fun deleteAllTags()

    @Query("DELETE FROM sources")
    fun deleteAllSources()

    @Delete
    fun deleteTag(tag: TagEntity)

    @Delete
    fun deleteSource(source: SourceEntity)
}