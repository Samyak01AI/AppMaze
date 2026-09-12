package com.appmaze.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ─── Entities ────────────────────────────────────────────

/**
 * Stores the grid position and distraction status of each app.
 */
@Entity(tableName = "app_positions")
data class AppPositionEntity(
    @PrimaryKey
    val packageName: String,
    val position: Int,
    val isDistracting: Boolean = false,
    val distractionScore: Int = 0,
    val lastScrambledTimestamp: Long = 0L,
    val scrambleCount: Int = 0,
    val isManuallyMarked: Boolean = false
)

/**
 * Stores daily usage statistics per app.
 */
@Entity(
    tableName = "daily_usage",
    primaryKeys = ["packageName", "date"]
)
data class DailyUsageEntity(
    val packageName: String,
    val date: String, // yyyy-MM-dd
    val usageMinutes: Long = 0,
    val launchCount: Int = 0
)

/**
 * Stores scramble events for the impact dashboard.
 */
@Entity(tableName = "scramble_events")
data class ScrambleEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val packageName: String,
    val fromPosition: Int,
    val toPosition: Int
)

@Entity(
    tableName = "scramble_position_history"
)
data class ScramblePositionHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val position: Int,
    val timestamp: Long
)

/**
 * Stores completed focus sessions.
 */
@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val durationMinutes: Int,
    val completed: Boolean
)

/**
 * Stores intervention events — when Shuffle shows a friction dialog.
 */
@Entity(tableName = "intervention_events")
data class InterventionEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val packageName: String,
    val userChoice: String // "continue" or "break"
)

/**
 * Stores app launch events for pattern detection.
 */
@Entity(tableName = "app_launch_events")
data class AppLaunchEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val packageName: String
)

// ─── DAOs ────────────────────────────────────────────────

@Dao
interface ScramblePositionHistoryDao {

    @Insert
    suspend fun insert(entity: ScramblePositionHistoryEntity)

    @Query("""
        SELECT position
        FROM scramble_position_history
        WHERE packageName = :packageName
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun getRecentPositions(
        packageName: String,
        limit: Int
    ): List<Int>

    @Query("SELECT * FROM scramble_position_history WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    suspend fun getRecentHistory(sinceTimestamp: Long): List<ScramblePositionHistoryEntity>

    @Query("SELECT * FROM scramble_position_history ORDER BY timestamp DESC")
    suspend fun getHistory(): List<ScramblePositionHistoryEntity>

    @Query("DELETE FROM scramble_position_history")
    suspend fun deleteAll()

    @Query("""
        DELETE FROM scramble_position_history
        WHERE timestamp < :before
    """)
    suspend fun deleteOlderThan(before: Long)
}


@Dao
interface AppPositionDao {
    @Query("SELECT * FROM app_positions ORDER BY position ASC")
    fun getAllPositions(): Flow<List<AppPositionEntity>>

    @Query("SELECT * FROM app_positions ORDER BY position ASC")
    suspend fun getAllPositionsList(): List<AppPositionEntity>

    @Query("SELECT * FROM app_positions WHERE packageName = :packageName")
    suspend fun getPosition(packageName: String): AppPositionEntity?

    @Query("SELECT * FROM app_positions WHERE isDistracting = 1")
    fun getDistractingApps(): Flow<List<AppPositionEntity>>

    @Query("SELECT * FROM app_positions WHERE isDistracting = 1")
    suspend fun getDistractingAppsList(): List<AppPositionEntity>

    @Upsert
    suspend fun upsert(entity: AppPositionEntity)

    @Upsert
    suspend fun upsertAll(entities: List<AppPositionEntity>)

    @Query("DELETE FROM app_positions WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("DELETE FROM app_positions WHERE packageName NOT IN (:installedPackages)")
    suspend fun removeUninstalledApps(installedPackages: List<String>)

    @Query("UPDATE app_positions SET isDistracting = :isDistracting, isManuallyMarked = :isManual WHERE packageName = :packageName")
    suspend fun setDistracting(packageName: String, isDistracting: Boolean, isManual: Boolean)

    @Query("UPDATE app_positions SET position = :newPosition, lastScrambledTimestamp = :timestamp, scrambleCount = scrambleCount + 1 WHERE packageName = :packageName")
    suspend fun updatePositionAfterScramble(packageName: String, newPosition: Int, timestamp: Long)
}

@Dao
interface DailyUsageDao {
    @Query("SELECT * FROM daily_usage WHERE date = :date ORDER BY usageMinutes DESC")
    suspend fun getUsageForDate(date: String): List<DailyUsageEntity>

    @Query("SELECT * FROM daily_usage WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, usageMinutes DESC")
    suspend fun getUsageForRange(startDate: String, endDate: String): List<DailyUsageEntity>

    @Query("SELECT SUM(usageMinutes) FROM daily_usage WHERE date = :date")
    suspend fun getTotalUsageForDate(date: String): Long?

    @Upsert
    suspend fun upsert(entity: DailyUsageEntity)

    @Upsert
    suspend fun upsertAll(entities: List<DailyUsageEntity>)

    @Query("DELETE FROM daily_usage WHERE date < :beforeDate")
    suspend fun deleteOldData(beforeDate: String)

    @Query("DELETE FROM daily_usage")
    suspend fun deleteAll()
}

@Dao
interface ScrambleEventDao {
    @Insert
    suspend fun insert(event: ScrambleEventEntity)

    @Query("SELECT COUNT(*) FROM scramble_events WHERE timestamp >= :sinceTimestamp")
    suspend fun countEventsSince(sinceTimestamp: Long): Int

    @Query("SELECT * FROM scramble_events WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    suspend fun getEventsSince(sinceTimestamp: Long): List<ScrambleEventEntity>

    @Query("DELETE FROM scramble_events WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOldEvents(beforeTimestamp: Long)

    @Query("DELETE FROM scramble_events")
    suspend fun deleteAll()
}

@Dao
interface FocusSessionDao {
    @Insert
    suspend fun insert(session: FocusSessionEntity)

    @Query("SELECT * FROM focus_sessions WHERE completed = 1 ORDER BY endTime DESC")
    suspend fun getCompletedSessions(): List<FocusSessionEntity>

    @Query("SELECT * FROM focus_sessions WHERE completed = 1 ORDER BY endTime DESC LIMIT :limit")
    suspend fun getRecentCompletedSessions(limit: Int): List<FocusSessionEntity>

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE completed = 1 AND endTime >= :sinceTimestamp")
    suspend fun countCompletedSince(sinceTimestamp: Long): Int

    @Query("SELECT SUM(durationMinutes) FROM focus_sessions WHERE completed = 1 AND endTime >= :sinceTimestamp")
    suspend fun totalFocusMinutesSince(sinceTimestamp: Long): Long?

    /**
     * Returns the current focus streak — count of consecutive completed sessions
     * (no incomplete sessions in between).
     */
    @Query("SELECT * FROM focus_sessions ORDER BY endTime DESC")
    suspend fun getAllSessionsDesc(): List<FocusSessionEntity>

    @Query("DELETE FROM focus_sessions")
    suspend fun deleteAll()
}

@Dao
interface InterventionEventDao {
    @Insert
    suspend fun insert(event: InterventionEventEntity)

    @Query("SELECT COUNT(*) FROM intervention_events WHERE timestamp >= :sinceTimestamp")
    suspend fun countSince(sinceTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM intervention_events WHERE timestamp >= :sinceTimestamp AND userChoice = 'break'")
    suspend fun countBreaksSince(sinceTimestamp: Long): Int

    @Query("SELECT * FROM intervention_events WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    suspend fun getEventsSince(sinceTimestamp: Long): List<InterventionEventEntity>

    @Query("DELETE FROM intervention_events")
    suspend fun deleteAll()
}

@Dao
interface AppLaunchEventDao {
    @Insert
    suspend fun insert(event: AppLaunchEventEntity)

    @Query("SELECT * FROM app_launch_events WHERE timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    suspend fun getEventsSince(sinceTimestamp: Long): List<AppLaunchEventEntity>

    @Query("SELECT packageName FROM app_launch_events WHERE timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    suspend fun getPackageSequenceSince(sinceTimestamp: Long): List<String>

    @Query("SELECT COUNT(*) FROM app_launch_events WHERE packageName = :packageName AND timestamp >= :sinceTimestamp")
    suspend fun countLaunchesSince(packageName: String, sinceTimestamp: Long): Int

    @Query("DELETE FROM app_launch_events WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOlderThan(beforeTimestamp: Long)

    @Query("DELETE FROM app_launch_events")
    suspend fun deleteAll()
}

// ─── Database ────────────────────────────────────────────
@Database(
    entities = [
        AppPositionEntity::class,
        DailyUsageEntity::class,
        ScrambleEventEntity::class,
        ScramblePositionHistoryEntity::class,
        FocusSessionEntity::class,
        InterventionEventEntity::class,
        AppLaunchEventEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appPositionDao(): AppPositionDao
    abstract fun dailyUsageDao(): DailyUsageDao
    abstract fun scrambleEventDao(): ScrambleEventDao
    abstract fun scramblePositionHistoryDao(): ScramblePositionHistoryDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun interventionEventDao(): InterventionEventDao
    abstract fun appLaunchEventDao(): AppLaunchEventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "appmaze_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
