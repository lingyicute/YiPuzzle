package org.secuso.privacyfriendlybackup.api.common

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class PfaError(val code : PfaErrorCode, val errorMessage : String) : Parcelable {
    /**
     * New PFAErrorCodes should always be added at the end to avoid API version errors
     */
    enum class PfaErrorCode {
        INTENT_ERROR,
        AUTHENTICATION_ERROR,
        API_VERSION_UNSUPPORTED,
        ACTION_ERROR,
        SERVICE_NOT_BOUND,
        GENERAL_ERROR
    }
}

object CommonApiConstants {
    // Result Code Action
    const val RESULT_CODE = "RESULT_CODE"

    // Result Codes
    const val RESULT_CODE_ERROR = 0
    const val RESULT_CODE_SUCCESS = 1

    /**
     * If RESULT_CODE is RESULT_CODE_ERROR then the error can be retrieved from RESULT_ERROR
     */
    const val RESULT_ERROR = "RESULT_ERROR"

    // Extras
    const val EXTRA_API_VERSION = "EXTRA_API_VERSION"
}

object PfaApi {
    // API Version
    const val API_VERSION = 1

    // Connect Actions
    const val PFA_CONNECT_ACTION = "org.secuso.privacyfriendlybackup.api.pfa.PFAAuthService"

    // Command Actions
    const val ACTION_CONNECT = "PfaApi.ACTION_CONNECT"

    // Extras
    const val EXTRA_CONNECT_PACKAGE_NAME = "PfaApi.EXTRA_CONNECT_PACKAGE_NAME"
    const val EXTRA_CONNECT_IMMEDIATE = "PfaApi.EXTRA_CONNECT_IMMEDIATE"
}

object BackupApi {
    // API Version
    const val API_VERSION = 2

    // Connect Actions
    const val BACKUP_CONNECT_ACTION = "org.secuso.privacyfriendlybackup.services.BackupService"

    // Command Actions
    const val ACTION_SEND_MESSENGER = "BackupApi.ACTION_SEND_MESSENGER"
    const val ACTION_SEND_ERROR = "BackupApi.ACTION_SEND_ERROR"

    // Extras
    const val EXTRA_MESSENGER = "BackupApi.EXTRA_MESSENGER"
    const val EXTRA_ERROR = "BackupApi.EXTRA_ERROR"

    // Messenger Commands
    const val MESSAGE_BACKUP = 1
    const val MESSAGE_RESTORE = 2
    const val MESSAGE_DONE = 3
    const val MESSAGE_ERROR = 4

    // Error Types
    const val ERROR_GENERIC = 0
}