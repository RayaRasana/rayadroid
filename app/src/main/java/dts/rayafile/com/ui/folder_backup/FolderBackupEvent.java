package dts.rayafile.com.ui.folder_backup;

public class FolderBackupEvent {
    private String backupInfo;

    public FolderBackupEvent(String backupInfo) {
        this.backupInfo = backupInfo;
    }

    public String getBackupInfo() {
        return backupInfo;
    }
}
