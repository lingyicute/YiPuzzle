package org.secuso.privacyfriendlybackup.api.pfa

import android.content.Intent
import android.os.Binder
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import org.secuso.privacyfriendlybackup.api.IPFAService
import org.secuso.privacyfriendlybackup.api.common.AbstractAuthService
import org.secuso.privacyfriendlybackup.api.common.CommonApiConstants.RESULT_CODE
import org.secuso.privacyfriendlybackup.api.common.CommonApiConstants.RESULT_CODE_ERROR
import org.secuso.privacyfriendlybackup.api.common.CommonApiConstants.RESULT_CODE_SUCCESS
import org.secuso.privacyfriendlybackup.api.common.CommonApiConstants.RESULT_ERROR
import org.secuso.privacyfriendlybackup.api.common.PfaApi.ACTION_CONNECT
import org.secuso.privacyfriendlybackup.api.common.PfaApi.EXTRA_CONNECT_IMMEDIATE
import org.secuso.privacyfriendlybackup.api.common.PfaApi.EXTRA_CONNECT_PACKAGE_NAME
import org.secuso.privacyfriendlybackup.api.common.PfaError
import org.secuso.privacyfriendlybackup.api.util.ApiFormatter
import org.secuso.privacyfriendlybackup.api.worker.ConnectBackupWorker
import org.secuso.privacyfriendlybackup.api.worker.CreateBackupWorker
import java.util.concurrent.Executors

/**
 * This class is meant to be extended by the PFA. Also it should then be included in the PFA's
 * AndroidManifest.xml file.
 *
 * <pre>
 * {@code
 *      <service
 *           android:name=".PFAAuthService"
 *           android:enabled="true"
 *           android:exported="true"
 *           android:process=":backup"
 *           tools:ignore="ExportedService">
 *           <intent-filter>
 *               <action android:name="org.secuso.privacyfriendlybackup.api.pfa.PFAAuthService" />
 *           </intent-filter>
 *      </service>
 *      }
 * </pre>
 *
 * @author Christopher Beckmann
 */
abstract class PFAAuthService : AbstractAuthService() {

    val TAG = "PFA AuthService"

    val executor = Executors.newSingleThreadExecutor()

    override val SUPPORTED_API_VERSIONS = listOf(1)

    override val mBinder : IPFAService.Stub = object : IPFAService.Stub()  {

        override fun send(data: Intent?): Intent {
            Log.d(this.javaClass.simpleName, "Intent received: ${ApiFormatter.formatIntent(data)}")
            val result = canAccess(data, Binder.getCallingUid())
            if(result != null) {
                return result
            }
            // data can not be null here else canAccess(Intent) would have returned an error
            val resultIntent = handle(data!!)
            Log.d(this.javaClass.simpleName, "Sent Reply: ${ApiFormatter.formatIntent(resultIntent)}")
            return resultIntent
        }

        private fun handle(data: Intent): Intent {
            return when(data.action) {
                ACTION_CONNECT -> handleConnect(data)
                else -> Intent().apply {
                    putExtra(RESULT_CODE, RESULT_CODE_ERROR)
                    putExtra(RESULT_ERROR,
                        PfaError(
                            PfaError.PfaErrorCode.ACTION_ERROR,
                            "Action ${data.action} is unsupported."
                        )
                    )
                }
            }
        }

        private fun handleConnect(data: Intent): Intent {
            val backupPackageName = data.getStringExtra(EXTRA_CONNECT_PACKAGE_NAME)
            val connectImmediately = data.getBooleanExtra(EXTRA_CONNECT_IMMEDIATE, false)

            return if(startBackupProcess())
                Intent().apply {
                    putExtra(RESULT_CODE, RESULT_CODE_SUCCESS)
                }
            else
                Intent().apply {
                    putExtra(RESULT_CODE, RESULT_CODE_ERROR)
                }
        }
    }

    private fun startBackupProcess() : Boolean {
        val backupWork = OneTimeWorkRequest.Builder(CreateBackupWorker::class.java)
            //.addTag("org.secuso.privacyfriendlybackup.api.CreateBackupWork")
            //.setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()

        val connectWork = OneTimeWorkRequest.Builder(ConnectBackupWorker::class.java)
            .addTag("org.secuso.privacyfriendlybackup.api.ConnectBackupWork")
            .build()

        // if connection is already running - don't cancel it
        val connectInfo = WorkManager.getInstance(this@PFAAuthService).getWorkInfosByTag("org.secuso.privacyfriendlybackup.api.ConnectBackupWork").get()
        if(connectInfo != null && connectInfo.isNotEmpty() && connectInfo[0].state == WorkInfo.State.RUNNING) {
            return true
        }

        WorkManager.getInstance(this@PFAAuthService)
                .beginUniqueWork("org.secuso.privacyfriendlybackup.api.ConnectBackupWork", ExistingWorkPolicy.REPLACE, backupWork).then(connectWork)
                .enqueue()

        val workInfo = WorkManager.getInstance(this@PFAAuthService).getWorkInfoById(backupWork.id).get()
                ?: return false

        Log.d(TAG, "CreateBackupWorker: ${workInfo.state.name}")
        return workInfo.state == WorkInfo.State.SUCCEEDED
                || workInfo.state == WorkInfo.State.ENQUEUED
                || workInfo.state == WorkInfo.State.RUNNING
    }
}