package dts.rayafile.com.bus;

import com.jeremyliao.liveeventbus.LiveEventBus;
import com.jeremyliao.liveeventbus.core.Observable;
import dts.rayafile.com.enums.TransferOpType;

public class TransferBusHelper {
    private static final String KEY = "BUS_TRANSFER_OP";

    public static Observable<TransferOpType> getTransferObserver() {
        return LiveEventBus.get(KEY, TransferOpType.class);
    }

    public static void startFileMonitor() {
        getTransferObserver().post(TransferOpType.FILE_MONITOR_START);
    }

    public static void resetFileMonitor() {
        getTransferObserver().post(TransferOpType.FILE_MONITOR_RESET);
    }
}
