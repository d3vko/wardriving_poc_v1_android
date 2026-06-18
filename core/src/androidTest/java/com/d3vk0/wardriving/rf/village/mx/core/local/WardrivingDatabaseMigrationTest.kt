package com.d3vk0.wardriving.rf.village.mx.core.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WardrivingDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        WardrivingDatabase::class.java,
    )

    @Test
    fun migration1To2AddsLteAndUploadColumnsAndBackfillsUploadedRows() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS lte_samples (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    sessionId TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    technology TEXT NOT NULL,
                    state TEXT NOT NULL,
                    mcc TEXT,
                    mnc TEXT,
                    lac INTEGER,
                    cellId INTEGER,
                    band TEXT,
                    rssi INTEGER,
                    rsrp INTEGER,
                    rsrq INTEGER,
                    sinr INTEGER,
                    operator TEXT,
                    longitude REAL,
                    latitude REAL,
                    rawPayload TEXT
                )
                """.trimIndent(),
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS pending_uploads (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    sessionId TEXT NOT NULL,
                    uploadType TEXT NOT NULL,
                    filePath TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    sampleCount INTEGER NOT NULL,
                    retryCount INTEGER NOT NULL,
                    lastError TEXT,
                    uploadedAt INTEGER
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO pending_uploads (
                    sessionId, uploadType, filePath, createdAt, sampleCount, retryCount, lastError, uploadedAt
                ) VALUES ('s1', 'lte', '/tmp/lte.csv', 1, 10, 0, NULL, 2)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO pending_uploads (
                    sessionId, uploadType, filePath, createdAt, sampleCount, retryCount, lastError, uploadedAt
                ) VALUES ('s2', 'lte', '/tmp/lte2.csv', 1, 10, 0, NULL, NULL)
                """.trimIndent(),
            )

            WardrivingDatabase.MIGRATION_1_2.migrate(this)

            assertTrue(columnNames("lte_samples").containsAll(listOf("pci", "earfcn")))
            assertTrue(columnNames("pending_uploads").contains("alreadyUploaded"))
            query("SELECT alreadyUploaded FROM pending_uploads WHERE sessionId = 's1'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
            query("SELECT alreadyUploaded FROM pending_uploads WHERE sessionId = 's2'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
            close()
        }
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.columnNames(table: String): Set<String> {
        return query("PRAGMA table_info($table)").use { cursor ->
            buildSet {
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) {
                    add(cursor.getString(nameIndex))
                }
            }
        }
    }

    private companion object {
        const val TEST_DB = "wardriving-migration-test"
    }
}
