package org.secuso.privacyfriendlybackup.api.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.Messenger
import android.os.ParcelFileDescriptor
import android.util.Log
import org.secuso.privacyfriendlybackup.api.IBackupService
import org.secuso.privacyfriendlybackup.api.common.BackupApi
import org.secuso.privacyfriendlybackup.api.common.BackupApi.ACTION_SEND_MESSENGER
import org.secuso.privacyfriendlybackup.api.common.BackupApi.EXTRA_MESSENGER
import org.secuso.privacyfriendlybackup.api.common.CommonApiConstants
import org.secuso.privacyfriendlybackup.api.common.CommonApiConstants.EXTRA_API_VERSION
import org.secuso.privacyfriendlybackup.api.common.PfaError
import java.io.InputStream
import java.io.OutputStream


const val API_VERSION = 1

/**
 * @author Christopher Beckmann
 */
class BackupApiConnection(
    private val mContext: Context,
    private val mBackupServiceName: String,
    private val mBackupApiListener: IBackupApiListener? = null,
    private val mMessenger: Messenger? = null,
    private val mApiVersion: Int = API_VERSION
) {

    interface IBackupApiListener {
        fun onBound(service : IBackupService?)
        fun onError(error : PfaError)
        fun onSuccess(action : String)
        fun onDisconnected()
    }

    private var mService : IBackupService? = null
    private var mBackupOutputPipe : ParcelFileDescriptor? = null

    private val mConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            mService = null
            mBackupApiListener?.onDisconnected()
        }
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mService = IBackupService.Stub.asInterface(service)
            mBackupApiListener?.onBound(mService)

            if(mMessenger != null) {
                sendMessenger(mMessenger)
            }
        }
    }

    fun send(action : String, extras : Bundle? = null) {
        if(!isBound()) {
            mBackupApiListener?.onError(
                PfaError(
                    PfaError.PfaErrorCode.SERVICE_NOT_BOUND,
                    "Service is not bound."
                )
            )
            return
        }
        val result : Intent = mService!!.send(Intent().apply {
            putExtra(EXTRA_API_VERSION, mApiVersion)
            extras?.let { putExtras(extras) }
            setAction(action)
        })

        handleResult(result, action)
    }

    fun sendMessenger(messenger : Messenger) {
        if(!isBound()) {
            mBackupApiListener?.onError(
                PfaError(
                    PfaError.PfaErrorCode.SERVICE_NOT_BOUND,
                    "Service is not bound."
                )
            )
            return
        }

        val result : Intent = mService!!.send(Intent().apply {
            putExtra(EXTRA_API_VERSION, mApiVersion)
            putExtra(EXTRA_MESSENGER, messenger)
            setAction(ACTION_SEND_MESSENGER)
        })

        handleResult(result, ACTION_SEND_MESSENGER)
    }

    private fun handleResult(result : Intent, originalAction : String) {
        result.setExtrasClassLoader(mContext.classLoader)

        when(result.getIntExtra(CommonApiConstants.RESULT_CODE, -1)) {
            CommonApiConstants.RESULT_CODE_SUCCESS -> {
                mBackupApiListener?.onSuccess(originalAction)
            }
            CommonApiConstants.RESULT_CODE_ERROR -> {
                try {
                    val error: PfaError? =
                        result.getParcelableExtra(CommonApiConstants.RESULT_ERROR)
                    if (error != null) {
                        mBackupApiListener?.onError(error)
                    } else {
                        mBackupApiListener?.onError(
                            PfaError(
                                PfaError.PfaErrorCode.GENERAL_ERROR,
                                "Unknown error occurred. Couldn't load error."
                            )
                        )
                    }
                } catch (e : Exception) {
                    mBackupApiListener?.onError(
                        PfaError(
                            PfaError.PfaErrorCode.GENERAL_ERROR,
                            "Exception occurred: ${e.message}"
                        )
                    )
                }
            }
            else -> {
                mBackupApiListener?.onError(
                    PfaError(
                        PfaError.PfaErrorCode.GENERAL_ERROR,
                        "RESULT_CODE unknown."
                    )
                )
            }
        }
    }

    fun connect() {
        if(mService == null) {
            Intent(BackupApi.BACKUP_CONNECT_ACTION).also { intent ->
                // this is the name of the Backup Service to connect to
                intent.setPackage(mBackupServiceName)
                mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
            }
        } else {
            mBackupApiListener?.onBound(mService)
        }
    }

    fun disconnect() {
        mContext.unbindService(mConnection)
    }

    fun isBound() : Boolean {
        return mService != null
    }

    fun initBackup() : OutputStream? {
        if(!isBound()) {
            mBackupApiListener?.onError(
                PfaError(
                    PfaError.PfaErrorCode.SERVICE_NOT_BOUND,
                    "Service is not bound."
                )
            )
            return null
        }

        val pipes = ParcelFileDescriptor.createPipe()

        mBackupOutputPipe = pipes[0]

        return ParcelFileDescriptor.AutoCloseOutputStream(pipes[1])
    }

    fun sendBackupData() {
        if(!isBound()) {
            mBackupApiListener?.onError(
                PfaError(
                    PfaError.PfaErrorCode.SERVICE_NOT_BOUND,
                    "Service is not bound."
                )
            )
            return
        }

        if(mBackupOutputPipe == null) {
            mBackupApiListener?.onError(
                PfaError(
                    PfaError.PfaErrorCode.GENERAL_ERROR,
                    "OutputStream can not be null."
                )
            )
            return
        }

        // send read pipe to send backup
        mService!!.performBackup(mBackupOutputPipe)
    }

    fun getRestoreData() : InputStream? {
        if(!isBound()) {
            mBackupApiListener?.onError(
                PfaError(
                    PfaError.PfaErrorCode.SERVICE_NOT_BOUND,
                    "Service is not bound."
                )
            )
            return null
        }

        // return read pipe to perform restore in PFA
        val pipe = mService!!.performRestore()
        return if(pipe != null) ParcelFileDescriptor.AutoCloseInputStream(pipe) else null
    }
}