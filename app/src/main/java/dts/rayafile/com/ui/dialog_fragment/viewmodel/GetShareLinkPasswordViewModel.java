package dts.rayafile.com.ui.dialog_fragment.viewmodel;

import android.text.TextUtils;

import androidx.lifecycle.MutableLiveData;

import com.blankj.utilcode.util.CollectionUtils;
import com.blankj.utilcode.util.TimeUtils;
import dts.rayafile.com.SeafException;
import dts.rayafile.com.config.DateFormatType;
import dts.rayafile.com.framework.data.model.dirents.DirentPermissionModel;
import dts.rayafile.com.framework.data.model.objs.DirentShareLinkModel;
import dts.rayafile.com.framework.http.HttpIO;
import dts.rayafile.com.ui.base.viewmodel.BaseViewModel;
import dts.rayafile.com.ui.dialog_fragment.DialogService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.reactivex.Single;
import io.reactivex.functions.Consumer;

public class GetShareLinkPasswordViewModel extends BaseViewModel {
    private MutableLiveData<DirentShareLinkModel> linkLiveData = new MutableLiveData<>();

    public MutableLiveData<DirentShareLinkModel> getLinkLiveData() {
        return linkLiveData;
    }

    public void getFirstShareLink(String repoId, String path, String password, String expire_days) {
        getRefreshLiveData().setValue(true);

        Single<List<DirentShareLinkModel>> single = HttpIO.getCurrentInstance().execute(DialogService.class).listAllShareLink(repoId, path);
        addSingleDisposable(single, new Consumer<List<DirentShareLinkModel>>() {
            @Override
            public void accept(List<DirentShareLinkModel> models) throws Exception {
                if (CollectionUtils.isEmpty(models)) {
                    createShareLink(repoId, path, password, expire_days, null);
                } else {
                    Optional<DirentShareLinkModel> optional = models.stream().filter(f -> !f.is_expired).findFirst();
                    if (optional.isPresent()) {
                        getLinkLiveData().setValue(optional.get());
                    } else {
                        getLinkLiveData().setValue(null);
                    }
                    getRefreshLiveData().setValue(false);
                }
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                SeafException seafException = getExceptionByThrowable(throwable);
                getSeafExceptionLiveData().setValue(seafException);
                getRefreshLiveData().setValue(false);
            }
        });
    }

    public void createShareLink(String repoId, String path, String password, String expire_days, DirentPermissionModel permissions) {
        getRefreshLiveData().setValue(true);

        Map<String, Object> requestDataMap = new HashMap<>();
        requestDataMap.put("repo_id", repoId);
        requestDataMap.put("path", path);

        if (!TextUtils.isEmpty(password)) {
            requestDataMap.put("password", password);
        }

        if (!TextUtils.isEmpty(expire_days)) {
            int expirationDay = Integer.parseInt(expire_days);
            long mDay = 24L * 60 * 60 * 1000 * expirationDay;
            long expireDay = TimeUtils.getNowMills() + mDay;
            String expireDayStr = TimeUtils.millis2String(expireDay, DateFormatType.DATE_XXX);
            requestDataMap.put("expiration_time", expireDayStr);
        }

        Single<DirentShareLinkModel> single;
        if (permissions != null) {
            requestDataMap.put("permissions", permissions);
        }

        single = HttpIO.getCurrentInstance().execute(DialogService.class).createMultiShareLink(requestDataMap);
//            single = HttpIO.getCurrentInstance().execute(DialogService.class).createShareLink(requestDataMap);

        addSingleDisposable(single, new Consumer<DirentShareLinkModel>() {
            @Override
            public void accept(DirentShareLinkModel direntShareLinkModel) throws Exception {
                getLinkLiveData().setValue(direntShareLinkModel);
                getRefreshLiveData().setValue(false);
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                SeafException seafException = getExceptionByThrowable(throwable);
                getSeafExceptionLiveData().setValue(seafException);
                getRefreshLiveData().setValue(false);
            }
        });
    }
}