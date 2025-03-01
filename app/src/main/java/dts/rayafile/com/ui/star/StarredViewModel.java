package dts.rayafile.com.ui.star;

import androidx.lifecycle.MutableLiveData;

import dts.rayafile.com.SeafException;
import dts.rayafile.com.account.Account;
import dts.rayafile.com.account.SupportAccountManager;
import dts.rayafile.com.framework.util.Objs;
import dts.rayafile.com.ui.base.viewmodel.BaseViewModel;
import dts.rayafile.com.framework.http.HttpIO;
import dts.rayafile.com.framework.data.model.ResultModel;
import dts.rayafile.com.framework.data.db.entities.StarredModel;

import java.util.List;

import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import kotlin.Pair;

public class StarredViewModel extends BaseViewModel {
    private final MutableLiveData<List<StarredModel>> listLiveData = new MutableLiveData<>();
    private final MutableLiveData<Pair<String, ResultModel>> UnStarredResultLiveData = new MutableLiveData<>();

    public MutableLiveData<List<StarredModel>> getListLiveData() {
        return listLiveData;
    }

    public MutableLiveData<Pair<String, ResultModel>> getUnStarredResultLiveData() {
        return UnStarredResultLiveData;
    }

    public void loadData() {
        getRefreshLiveData().setValue(true);

        Account account = SupportAccountManager.getInstance().getCurrentAccount();

        Single<List<StarredModel>> listSingle = Objs.getStarredSingleFromServer(account);

        addSingleDisposable(listSingle, new Consumer<List<StarredModel>>() {
            @Override
            public void accept(List<StarredModel> starredModels) throws Exception {
                getRefreshLiveData().setValue(false);
                getListLiveData().setValue(starredModels);
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                getRefreshLiveData().setValue(false);
                SeafException seafException = getExceptionByThrowable(throwable);

                if (seafException == SeafException.REMOTE_WIPED_EXCEPTION) {
                    //post a request
                    completeRemoteWipe();
                }

                getSeafExceptionLiveData().setValue(seafException);
            }
        });
    }

    public void unStarItem(String repoId, String path) {
        Single<ResultModel> flowable = HttpIO.getCurrentInstance().execute(StarredService.class).unStar(repoId, path);
        addSingleDisposable(flowable, new Consumer<ResultModel>() {
            @Override
            public void accept(ResultModel resultModel) throws Exception {
                getUnStarredResultLiveData().setValue(new Pair<>(path, resultModel));
            }
        });
    }

}
