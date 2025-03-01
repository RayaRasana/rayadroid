package dts.rayafile.com.ui.dialog_fragment.viewmodel;

import android.text.TextUtils;

import androidx.lifecycle.MutableLiveData;

import com.blankj.utilcode.util.CollectionUtils;
import dts.rayafile.com.framework.datastore.sp_livedata.AlbumBackupSharePreferenceHelper;
import dts.rayafile.com.framework.datastore.sp_livedata.FolderBackupSharePreferenceHelper;
import dts.rayafile.com.framework.util.SLogs;
import dts.rayafile.com.ui.base.viewmodel.BaseViewModel;
import dts.rayafile.com.framework.data.model.ResultModel;
import dts.rayafile.com.framework.http.HttpIO;
import dts.rayafile.com.ui.dialog_fragment.DialogService;
import dts.rayafile.com.ui.folder_backup.RepoConfig;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import kotlin.Pair;

public class DeleteRepoViewModel extends BaseViewModel {
    private final MutableLiveData<ResultModel> ActionLiveData = new MutableLiveData<>();

    public MutableLiveData<ResultModel> getActionLiveData() {
        return ActionLiveData;
    }

    public void deleteRepo(List<String> repoIds) {
        if (CollectionUtils.isEmpty(repoIds)) {
            return;
        }
        getRefreshLiveData().setValue(true);

        List<Flowable<Pair<String, String>>> flowableList = new ArrayList<>();
        for (String repoId : repoIds) {
            flowableList.add(getDeleteFlowable(repoId));
        }

        Flowable<Pair<String, String>> mergedFlowable = Flowable.mergeDelayError(flowableList, 5, Flowable.bufferSize());
        addFlowableDisposable(mergedFlowable, new Consumer<Pair<String, String>>() {
            @Override
            public void accept(Pair<String, String> pair) throws Exception {

                SLogs.d("DeleteRepoViewModel deleteRepo result: repoId = " + pair.getFirst() + ", result = " + pair.getSecond());


                String repoId = pair.getFirst();
                String o = pair.getSecond();

                ResultModel resultModel1 = new ResultModel();
                if (TextUtils.equals("success", o)) {
                    resultModel1.success = true;
                } else {
                    resultModel1.error_msg = o;
                }

                if (resultModel1.success) {
                    //check album backup repo config
                    RepoConfig albumRepoConfig = AlbumBackupSharePreferenceHelper.readRepoConfig();
                    if (albumRepoConfig != null && TextUtils.equals(repoId, albumRepoConfig.getRepoId())) {
                        AlbumBackupSharePreferenceHelper.writeBackupSwitch(false);
                    }

                    //check folder backup repo config
                    RepoConfig folderRepoConfig = FolderBackupSharePreferenceHelper.readRepoConfig();
                    if (folderRepoConfig != null && TextUtils.equals(repoId, folderRepoConfig.getRepoId())) {
                        FolderBackupSharePreferenceHelper.writeBackupSwitch(false);
                    }
                }
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                getRefreshLiveData().setValue(false);

                ResultModel resultModel = new ResultModel();
                resultModel.error_msg = getErrorMsgByThrowable(throwable);
                getActionLiveData().setValue(resultModel);
            }
        }, new Action() {
            @Override
            public void run() throws Exception {
                //todo 检查一下getActionLiveData
                getRefreshLiveData().setValue(false);
                getActionLiveData().setValue(new ResultModel());
            }
        });
    }

    private Flowable<Pair<String, String>> getDeleteFlowable(String repoId) {
        Single<String> single = HttpIO.getCurrentInstance().execute(DialogService.class).deleteRepo(repoId);
        return single.flatMap(new Function<String, SingleSource<Pair<String, String>>>() {
            @Override
            public SingleSource<Pair<String, String>> apply(String s) throws Exception {
                return Single.just(new Pair<>(repoId, s));
            }
        }).toFlowable();
    }


}
