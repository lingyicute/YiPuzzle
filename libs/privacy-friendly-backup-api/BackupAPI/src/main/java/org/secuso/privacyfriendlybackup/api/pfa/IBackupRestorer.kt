package org.secuso.privacyfriendlybackup.api.pfa

import android.content.Context
import java.io.InputStream

/**
 * Interface for the PFA. An instance of this class should be passed to the BackupManager on
 * Application start. The logic to restore a backup should be implemented in the
 * {@link #restoreBackup(Context, String)} method.
 *
 * @author Christopher Beckmann
 */
interface IBackupRestorer {
    fun restoreBackup(context: Context, restoreData: InputStream) : Boolean
}