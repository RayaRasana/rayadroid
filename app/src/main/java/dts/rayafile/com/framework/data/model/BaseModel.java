package dts.rayafile.com.framework.data.model;

import androidx.room.Ignore;

import dts.rayafile.com.config.Constants;
import dts.rayafile.com.framework.data.db.entities.FileTransferEntity;
import dts.rayafile.com.ui.data_migrate.DataMigrationActivity;

public class BaseModel {
    /**
     * <h3>data version control</h3>
     * <p>when v = 0, In some entities, this represents pre-migration legacy data.
     * eg. {@link FileTransferEntity}.</p>
     * <br>
     * <p>
     * <b>notice</b>: Some values may have invalid or null, please check before using them.</p>
     * see also: {@link DataMigrationActivity}
     * <br>
     * <br>
     * <p>
     * when v = 1, it's new data after migration, and app version is >= 3.0.0.
     * </p>
     * <br>
     * <p><b>default is 1.<b/></p>
     */
    public int v = 1;


    /**
     * @see dts.rayafile.com.config.Constants.DataStatus
     */
    public int data_status = Constants.DataStatus.NORMAL;

    @Ignore
    public boolean is_checked = false;
}
