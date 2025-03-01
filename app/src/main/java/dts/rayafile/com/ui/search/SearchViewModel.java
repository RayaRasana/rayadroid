package dts.rayafile.com.ui.search;

import android.text.TextUtils;

import androidx.lifecycle.MutableLiveData;

import com.blankj.utilcode.util.CollectionUtils;
import dts.rayafile.com.framework.data.db.AppDatabase;
import dts.rayafile.com.framework.data.db.entities.RepoModel;
import dts.rayafile.com.framework.data.model.search.SearchModel;
import dts.rayafile.com.framework.data.model.search.SearchWrapperModel;
import dts.rayafile.com.framework.http.HttpIO;
import dts.rayafile.com.framework.util.SLogs;
import dts.rayafile.com.ui.base.viewmodel.BaseViewModel;
import dts.rayafile.com.ui.repo.RepoService;

import java.util.List;

import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

public class SearchViewModel extends BaseViewModel {

    private final MutableLiveData<List<SearchModel>> mListLiveData = new MutableLiveData<>();

    public MutableLiveData<List<SearchModel>> getSearchListLiveData() {
        return mListLiveData;
    }

    public void searchNext(String q, int pageNo, int pageSize) {
        if (TextUtils.isEmpty(q)){
            return;
        }

        getRefreshLiveData().setValue(true);
        Single<SearchWrapperModel> single = HttpIO.getCurrentInstance().execute(SearchService.class).search("all", q, "all", pageNo, pageSize);
        addSingleDisposable(single, new Consumer<SearchWrapperModel>() {
            @Override
            public void accept(SearchWrapperModel searchWrapperModel) throws Exception {
                getSearchListLiveData().setValue(searchWrapperModel.results);
                getRefreshLiveData().setValue(false);
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                getSeafExceptionLiveData().setValue(getExceptionByThrowable(throwable));

                getRefreshLiveData().setValue(false);
            }
        });
    }

    public void getRepoModel(String repoId, Consumer<RepoModel> consumer) {
        //from db
        Single<List<RepoModel>> singleDb = AppDatabase.getInstance().repoDao().getRepoById(repoId);

        Single<RepoModel> singleNet = HttpIO.getCurrentInstance().execute(RepoService.class).getRepoInfo(repoId);

        Single<RepoModel> single = singleDb.flatMap(new Function<List<RepoModel>, SingleSource<RepoModel>>() {
            @Override
            public SingleSource<RepoModel> apply(List<RepoModel> repoModels) throws Exception {
                if (CollectionUtils.isEmpty(repoModels)) {
                    return singleNet.flatMap(new Function<RepoModel, SingleSource<RepoModel>>() {
                        @Override
                        public SingleSource<RepoModel> apply(RepoModel repoModel) throws Exception {

                            //insert
                            AppDatabase.getInstance().repoDao().insert(repoModel);

                            return Single.just(repoModel);
                        }
                    });
                } else {
                    return Single.just(repoModels.get(0));
                }
            }
        });

        addSingleDisposable(single, new Consumer<RepoModel>() {
            @Override
            public void accept(RepoModel repoModel) throws Exception {
                if (consumer != null) {
                    consumer.accept(repoModel);
                }
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                SLogs.e(throwable);
            }
        });
    }

}
