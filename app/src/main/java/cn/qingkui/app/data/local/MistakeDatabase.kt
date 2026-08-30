package cn.qingkui.app.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "mistake_drafts")
data class MistakeDraftEntity(
    @PrimaryKey val id: String,
    val remoteId: String? = null,
    val ocrTaskId: String? = null,
    val imagePath: String,
    val subject: String,
    val questionText: String,
    val studentWork: String,
    val questionGoal: String,
    val status: String = "draft",
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Dao
interface MistakeDraftDao {
    @Query("SELECT * FROM mistake_drafts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<MistakeDraftEntity>>

    @Query("SELECT * FROM mistake_drafts WHERE id = :id")
    suspend fun get(id: String): MistakeDraftEntity?

    @Query("SELECT * FROM mistake_drafts")
    suspend fun all(): List<MistakeDraftEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(draft: MistakeDraftEntity)

    @Query("UPDATE mistake_drafts SET status = :status, errorMessage = :error, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, error: String?, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE mistake_drafts SET remoteId = :remoteId, ocrTaskId = :taskId, status = 'uploaded', errorMessage = NULL, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markUploaded(id: String, remoteId: String, taskId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM mistake_drafts WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM mistake_drafts")
    suspend fun deleteAll()
}

@Database(entities = [MistakeDraftEntity::class], version = 1, exportSchema = false)
abstract class MistakeDatabase : RoomDatabase() {
    abstract fun drafts(): MistakeDraftDao

    companion object {
        @Volatile private var instance: MistakeDatabase? = null

        fun get(context: Context): MistakeDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                MistakeDatabase::class.java,
                "qingkui-local.db",
            ).build().also { instance = it }
        }
    }
}
