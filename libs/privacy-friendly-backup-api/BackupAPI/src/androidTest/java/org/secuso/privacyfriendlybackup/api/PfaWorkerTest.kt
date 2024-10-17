package org.secuso.privacyfriendlybackup.api

import android.content.Context
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.*
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.runBlocking

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import org.secuso.privacyfriendlybackup.api.pfa.BackupDataStore
import org.secuso.privacyfriendlybackup.api.pfa.BackupManager
import org.secuso.privacyfriendlybackup.api.pfa.IBackupCreator
import org.secuso.privacyfriendlybackup.api.pfa.IBackupRestorer
import org.secuso.privacyfriendlybackup.api.worker.CreateBackupWorker
import org.secuso.privacyfriendlybackup.api.worker.RestoreBackupWorker
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.Executors

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class PfaWorkerTest {
    val packageName = "org.secuso.privacyfriendlybackup.api"
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    lateinit var workManager: WorkManager

    @Before
    fun setUp() {
        val config = Configuration.Builder().setExecutor(SynchronousExecutor())
            .setMinimumLoggingLevel(Log.DEBUG).build()
        WorkManagerTestInitHelper.initializeTestWorkManager(appContext, config)
        workManager = WorkManager.getInstance(appContext)

        BackupManager.backupRestorer = object : IBackupRestorer {
            override fun restoreBackup(context: Context, restoreData: InputStream): Boolean {
                Thread.sleep(1000)
                Log.d("BACKUP CREATOR", "createBackup called.")
                return true
            }
        }

        BackupManager.backupCreator = object : IBackupCreator {
            override fun writeBackup(context: Context, outputStream: OutputStream): Boolean {
                Thread.sleep(1000)
                Log.d("BACKUP RESTORER", "restoreBackup called.")
                outputStream.write("{ 'test': [] }".toByteArray())
                return true
            }
        }
    }

    @Test
    fun testCreateBackupWorker() {
        //val constraints: Constraints =
        //val worker = OneTimeWorkRequestBuilder<CreateBackupWorker>().build()
        //workManager.enqueue(worker)

        // driver = WorkManagerTestInitHelper.getTestDriver(context)
        // driver.setAllConstraintsMet(worker.id)
        //val workInfo = workManager.getWorkInfoById(worker.id).get()
        //assertEquals(WorkInfo.State.SUCCEEDED, workInfo.state)

//        val worker = OneTimeWorkRequestBuilder<CreateBackupWorker>().build()
//        workManager.enqueue(worker)
//        val info = workManager.getWorkInfoById(worker.id).get()
//        assertEquals(WorkInfo.State.SUCCEEDED, info.state)
        assertEquals(WorkInfo.State.SUCCEEDED, runWorker<CreateBackupWorker>().state)
    }

    @Test
    fun testRestoreBackupWorker() {
        BackupDataStore.saveRestoreData(appContext, "{ 'test': [] }".byteInputStream())
        assertEquals(WorkInfo.State.SUCCEEDED, runWorker<RestoreBackupWorker>().state)
    }

    @Test
    fun testRestoreBackupWorkerWithoutRestoreData() {
        assertEquals(WorkInfo.State.FAILED, runWorker<RestoreBackupWorker>().state)
    }

    inline fun <reified T : Worker> runWorker(): WorkInfo {
        val worker = OneTimeWorkRequestBuilder<T>().build()
        workManager.enqueue(worker)
        return workManager.getWorkInfoById(worker.id).get()
    }
}