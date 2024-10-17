// IBackupService.aidl
package org.secuso.privacyfriendlybackup.api;

interface IBackupService {

    ParcelFileDescriptor performRestore();
    void performBackup(in ParcelFileDescriptor input);

    Intent send(in Intent data);
}
