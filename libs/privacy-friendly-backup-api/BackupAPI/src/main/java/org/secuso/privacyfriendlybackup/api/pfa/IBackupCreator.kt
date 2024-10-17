package org.secuso.privacyfriendlybackup.api.pfa

import android.content.Context
import java.io.OutputStream

/**
 * Interface for the PFA. An instance of this class should be passed to the BackupManager on
 * Application start. The logic to create a backup should be implemented in the
 * {@link #createBackup(Context)} method.
 *
 * @author Christopher Beckmann
 */
interface IBackupCreator {
    fun writeBackup(context: Context, outputStream: OutputStream) : Boolean
}