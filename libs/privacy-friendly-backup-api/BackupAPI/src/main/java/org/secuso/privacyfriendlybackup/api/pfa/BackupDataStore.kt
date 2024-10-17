package org.secuso.privacyfriendlybackup.api.pfa

import android.content.Context
import org.secuso.privacyfriendlybackup.api.util.copyInputStreamToFile
import java.io.File
import java.io.InputStream

/**
 * @author Christopher Beckmann
 */
object BackupDataStore {

    const val BACKUP_PATH = "temp_backups"
    const val BACKUP_FILE = "BackupDataStore.BACKUP_DATA"
    const val RESTORE_FILE = "BackupDataStore.RESTORE_DATA"

    fun getRestoreData(context: Context) : InputStream? {
        val path = File(context.filesDir, BACKUP_PATH)
        val file = File(path, RESTORE_FILE)
        path.mkdir()

        return if(file.exists()) file.inputStream() else null
    }

    fun saveRestoreData(context: Context, restoreData: InputStream) {
        val path = File(context.filesDir, BACKUP_PATH)
        val file = File(path, RESTORE_FILE)
        path.mkdir()

        file.copyInputStreamToFile(restoreData)
    }

    fun cleanRestoreData(context: Context) {
        val path = File(context.filesDir, BACKUP_PATH)
        val file = File(path, RESTORE_FILE)
        path.mkdir()

        file.delete()
    }

    fun getBackupData(context: Context): InputStream? {
        val path = File(context.filesDir, BACKUP_PATH)
        val file = File(path, BACKUP_FILE)
        path.mkdir()

        return if(file.exists()) file.inputStream() else null
    }

    fun saveBackupData(context: Context, backupData: InputStream) {
        val path = File(context.filesDir, BACKUP_PATH)
        val file = File(path, BACKUP_FILE)
        path.mkdir()

        file.copyInputStreamToFile(backupData)
    }

    private fun cleanBackupData(context: Context) {
        val path = File(context.filesDir, BACKUP_PATH)
        val file = File(path, BACKUP_FILE)
        path.mkdir()

        file.delete()
    }

    fun isBackupDataSaved(context: Context): Boolean {
        val path = File(context.filesDir, BACKUP_PATH)
        val file = File(path, BACKUP_FILE)
        return file.exists()
    }

    fun isRestoreDataSaved(context: Context): Boolean {
        val path = File(context.filesDir, BACKUP_PATH)
        val file = File(path, BACKUP_FILE)
        return file.exists()
    }

    fun cleanBackupDataIfNoRestoreData(context: Context) {
        if(!isRestoreDataSaved(context)) {
            cleanBackupData(context)
        }
    }
}