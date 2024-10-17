package org.secuso.privacyfriendlybackup.api.pfa

import org.secuso.privacyfriendlybackup.api.pfa.IBackupCreator
import org.secuso.privacyfriendlybackup.api.pfa.IBackupRestorer

/**
 * @author Christopher Beckmann
 */
object BackupManager {
    @JvmStatic var backupCreator : IBackupCreator? = null
    @JvmStatic var backupRestorer : IBackupRestorer? = null
}