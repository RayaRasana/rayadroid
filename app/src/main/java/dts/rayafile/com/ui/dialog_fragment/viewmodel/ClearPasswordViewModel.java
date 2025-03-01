package dts.rayafile.com.ui.dialog_fragment.viewmodel;

import dts.rayafile.com.framework.data.db.AppDatabase;
import dts.rayafile.com.ui.base.viewmodel.BaseViewModel;

import io.reactivex.Completable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;

public class ClearPasswordViewModel extends BaseViewModel {
    public void clear(Consumer<Boolean> consumer) {
        Completable completable = AppDatabase.getInstance().encKeyCacheDAO().deleteAll();
        addCompletableDisposable(completable, new Action() {
            @Override
            public void run() throws Exception {
                if (consumer != null) {
                    consumer.accept(true);
                }
            }
        });

    }
}
