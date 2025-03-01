package dts.rayafile.com.ui.dialog_fragment.viewmodel;

import android.text.TextUtils;

import androidx.lifecycle.MutableLiveData;

import dts.rayafile.com.ui.base.viewmodel.BaseViewModel;
import dts.rayafile.com.framework.data.db.entities.RepoModel;
import dts.rayafile.com.framework.http.HttpIO;
import dts.rayafile.com.ui.dialog_fragment.DialogService;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import okhttp3.RequestBody;

public class NewRepoViewModel extends BaseViewModel {
    private final MutableLiveData<RepoModel> createRepoLiveData = new MutableLiveData<>();

    public MutableLiveData<RepoModel> getCreateRepoLiveData() {
        return createRepoLiveData;
    }

    public void createNewRepo(String repoName, String description, String password) {

        if (TextUtils.isEmpty(repoName)) {
            return;
        }

        getRefreshLiveData().setValue(true);

        Map<String, String> requestDataMap = new HashMap<>();
        requestDataMap.put("name", repoName);

        if (description.length() > 0) {
            requestDataMap.put("desc", description);
        }
        if (!TextUtils.isEmpty(password)) {
            requestDataMap.put("passwd", password);
        }
        Map<String, RequestBody> bodyMap = genRequestBody(requestDataMap);
        Single<RepoModel> single = HttpIO.getCurrentInstance().execute(DialogService.class).createRepo(bodyMap);
        addSingleDisposable(single, new Consumer<RepoModel>() {
            @Override
            public void accept(RepoModel repoModel) throws Exception {

                getRefreshLiveData().setValue(false);

                getCreateRepoLiveData().setValue(repoModel);
            }
        });
    }
}
