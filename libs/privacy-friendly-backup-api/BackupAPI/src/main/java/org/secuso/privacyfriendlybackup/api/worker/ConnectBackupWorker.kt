package org.secuso.privacyfriendlybackup.api.worker

import android.content.Context
import android.os.*
import android.util.Log
import androidx.work.*
import androidx.work.CoroutineWorker
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.secuso.privacyfriendlybackup.api.IBackupService
import org.secuso.privacyfriendlybackup.api.common.BackupApi
import org.secuso.privacyfriendlybackup.api.pfa.BackupDataStore
import org.secuso.privacyfriendlybackup.api.common.BackupApi.ACTION_SEND_MESSENGER
import org.secuso.privacyfriendlybackup.api.common.BackupApi.MESSAGE_BACKUP
import org.secuso.privacyfriendlybackup.api.common.BackupApi.MESSAGE_DONE
import org.secuso.privacyfriendlybackup.api.common.BackupApi.MESSAGE_ERROR
import org.secuso.privacyfriendlybackup.api.common.BackupApi.MESSAGE_RESTORE
import org.secuso.privacyfriendlybackup.api.common.CommonApiConstants
import org.secuso.privacyfriendlybackup.api.common.PfaApi.EXTRA_CONNECT_PACKAGE_NAME
import org.secuso.privacyfriendlybackup.api.common.PfaError
import org.secuso.privacyfriendlybackup.api.pfa.BackupManager
import org.secuso.privacyfriendlybackup.api.util.BackupApiConnection
import java.lang.ref.WeakReference
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author Christopher Beckmann
 */
class ConnectBackupWorker(val context : Context, params: WorkerParameters) : CoroutineWorker(context, params), BackupApiConnection.IBackupApiListener {

    val TAG = "PFA Backup"

    var mConnection : BackupApiConnection =
        BackupApiConnection(
            context,
            params.inputData.getString(EXTRA_CONNECT_PACKAGE_NAME)
                ?: "org.secuso.privacyfriendlybackup",
            this,
            Messenger(MessageHandler(this))
        )

    var workDone = false
    var errorOccurred = false

    var backupInProgress = AtomicBoolean(false)
    var restoreInProgress = AtomicBoolean(false)

    internal class MessageHandler(worker : ConnectBackupWorker) : Handler(Looper.getMainLooper()) {
        val worker = WeakReference(worker)

        val TAG = "PFA Backup"

        override fun handleMessage(msg: Message) {
            Executors.newSingleThreadExecutor().run {
                when (msg.what) {
                    MESSAGE_BACKUP -> {
                        Log.d(TAG, "\t[Message: MESSAGE_BACKUP($MESSAGE_BACKUP)]")
                        worker.get()?.handleBackup()
                    }
                    MESSAGE_RESTORE -> {
                        Log.d(TAG, "\t[Message: MESSAGE_RESTORE($MESSAGE_RESTORE)]")
                        worker.get()?.handleRestore()
                    }
                    MESSAGE_ERROR -> {
                        Log.d(TAG, "\t[Message: MESSAGE_ERROR($MESSAGE_ERROR)]")
                    }
                    MESSAGE_DONE -> {
                        Log.d(TAG, "\t[Message: MESSAGE_DONE($MESSAGE_DONE)]")
                        worker.get()?.workDone = true
                    }
                    else -> {
                        Log.d(TAG, "\t[Message: Unknown(${msg.what})]")
                        worker.get()?.errorOccurred = true
                        worker.get()?.workDone = true
                    }
                }
            }
        }
    }

    fun handleBackup() {
        Log.d(TAG, "handleBackup() started")
        backupInProgress.set(true)

        if(checkAndSendError()) {
            errorOccurred = true
            workDone = true
            return
        }

        Log.d(TAG, "Retrieve backup from storage")
        var backupData = BackupDataStore.getBackupData(context)

        // no backup data available
        Log.d(TAG, "Check if backup data is available")
        if (backupData == null) {
            Log.d(TAG, "ERROR: Backup data is null")
            sendError()
            errorOccurred = true
            workDone = true
            return
        }

        Log.d(TAG, "Creating pipe")
        val outputStream = mConnection.initBackup()

        Log.d(TAG, "Writing backup data to pipe")

        GlobalScope.launch(IO) {
            outputStream?.use { stream ->
                backupData.use {
                    it.copyTo(stream)
                }
            }
        }
        //BackupManager.backupCreator?.writeBackup(context, stream)

        Log.d(TAG, "Sending backup data to Backup service")
        mConnection.sendBackupData()
        backupInProgress.set(false)

        Log.d(TAG, "handleBackup() finished")
    }

    private fun checkAndSendError() : Boolean {
        // check if an error occured and we need to send it to the backup app
        if(inputData.getInt(CommonApiConstants.RESULT_CODE, CommonApiConstants.RESULT_CODE_SUCCESS) == CommonApiConstants.RESULT_CODE_ERROR) {
            sendError()
            return true
        }
        return false
    }

    private fun sendError() {
        mConnection.send(BackupApi.ACTION_SEND_ERROR, Bundle().apply {
            putInt(BackupApi.EXTRA_ERROR, BackupApi.ERROR_GENERIC)
        })
    }

    fun handleRestore() {
        restoreInProgress.set(true)

        if(checkAndSendError()) {
            errorOccurred = true
            workDone = true
        }

        val stream = mConnection.getRestoreData()

        var restoreData : String? = null

        stream?.use {
            BackupManager.backupRestorer?.restoreBackup(context, it)
        }

        restoreInProgress.set(false)

//        // something went wrong
//        if(!BackupDataStore.isRestoreDataSaved(context)) {
//            errorOccurred = true
//            workDone = true
//            return
//        }

        // enqueue restore worker
//        val restoreBackupWorker = OneTimeWorkRequest.Builder(RestoreBackupWorker::class.java)
//            .addTag("org.secuso.privacyfriendlybackup.api.RestoreBackupWork")
//            .build()
//        WorkManager.getInstance(context)
//            .beginUniqueWork("org.secuso.privacyfriendlybackup.api.ConnectBackupWork", ExistingWorkPolicy.APPEND, restoreBackupWorker).enqueue()
    }

    override suspend fun doWork(): Result {
        mConnection.connect()

        var timeout = 60 * 5

        // wait for connection to finish
        do {
            delay(1000)
        } while(!workDone && --timeout > 0)

        Log.d(TAG, "Work is done! YAY")

        if(mConnection.isBound()) {
            mConnection.disconnect()
        }

        if(errorOccurred || timeout <= 0) {
            return Result.failure()
        }

        // keep backup data as long restore is not done
        BackupDataStore.cleanBackupDataIfNoRestoreData(context)
        return Result.success(Data.EMPTY)
    }

    override fun onBound(service: IBackupService?) {
        Log.d(TAG, "onBound($service)")
        // do nothing
    }

    override fun onError(error: PfaError) {
        Log.d(TAG, error.toString())
        errorOccurred = true
        workDone = true
    }

    override fun onSuccess(action: String) {
        Log.d(TAG, "onSuccess($action)")
        when(action) {
            ACTION_SEND_MESSENGER -> { /* nice but do nothing */ }
            else -> {
                errorOccurred = true
                workDone = true
            }
        }
    }

    override fun onDisconnected() {
        Log.d(TAG, "onDisconnected()")
        if(!workDone) {
            errorOccurred = true
            workDone = true
        }
    }
}