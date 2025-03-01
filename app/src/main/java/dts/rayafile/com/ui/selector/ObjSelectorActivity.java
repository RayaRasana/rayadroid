package dts.rayafile.com.ui.selector;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.blankj.utilcode.util.CollectionUtils;
import com.blankj.utilcode.util.TimeUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.chad.library.adapter4.QuickAdapterHelper;
import com.github.panpf.recycler.sticky.StickyItemDecoration;
import dts.rayafile.com.R;
import dts.rayafile.com.account.Account;
import dts.rayafile.com.account.SupportAccountManager;
import dts.rayafile.com.config.AbsLayoutItemType;
import dts.rayafile.com.context.NavContext;
import dts.rayafile.com.databinding.ActivitySelectorObjBinding;
import dts.rayafile.com.enums.FileViewType;
import dts.rayafile.com.enums.RepoSelectType;
import dts.rayafile.com.framework.data.db.entities.DirentModel;
import dts.rayafile.com.framework.data.db.entities.EncKeyCacheEntity;
import dts.rayafile.com.framework.data.db.entities.PermissionEntity;
import dts.rayafile.com.framework.data.db.entities.RepoModel;
import dts.rayafile.com.framework.data.model.BaseModel;
import dts.rayafile.com.ui.base.BaseActivity;
import dts.rayafile.com.ui.dialog_fragment.NewDirFileDialogFragment;
import dts.rayafile.com.ui.dialog_fragment.PasswordDialogFragment;
import dts.rayafile.com.ui.dialog_fragment.listener.OnRefreshDataListener;
import dts.rayafile.com.ui.dialog_fragment.listener.OnResultListener;
import dts.rayafile.com.ui.repo.RepoQuickAdapter;
import dts.rayafile.com.view.TipsViews;

import java.util.List;

import io.reactivex.functions.Consumer;

/**
 * can select repo、dir、account
 */
public class ObjSelectorActivity extends BaseActivity {
    private static final int STEP_CHOOSE_ACCOUNT = 1;
    private static final int STEP_CHOOSE_REPO = 2;
    private static final int STEP_CHOOSE_DIR = 3;
    private int mStep = 1;

    public static final String DATA_ACCOUNT = "account";
    public static final String DATA_REPO_ID = "repoID";
    public static final String DATA_REPO_NAME = "repoNAME";
    public static final String DATA_REPO_PERMISSION = "permission";
    public static final String DATA_DIRECTORY_PATH = "dirPath";
    public static final String DATA_DIR = "dir";


    private boolean canChooseAccount;
    private boolean isOnlyChooseRepo;

    private ActivitySelectorObjBinding binding;
    private final NavContext mNavContext = new NavContext();

    private RepoQuickAdapter adapter;
    private ObjSelectorViewModel viewModel;
    private Account mAccount;

    public static Intent getStartIntent(Context context) {
        Intent intent = new Intent(context, ObjSelectorActivity.class);
        intent.putExtra(ObjSelectorActivity.DATA_ACCOUNT, SupportAccountManager.getInstance().getCurrentAccount());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySelectorObjBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //view model
        viewModel = new ViewModelProvider(this).get(ObjSelectorViewModel.class);

        Intent intent = getIntent();
        if (intent != null) {
            Account account = intent.getParcelableExtra(DATA_ACCOUNT);
            if (account == null) {
                canChooseAccount = true;
            } else {
                mAccount = account;
            }
        }

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                stepBack();
            }
        });

        Toolbar toolbar = getActionBarToolbar();
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.app_name);
        }

        initView();
        initViewModel();
        initRv();

        if (canChooseAccount) {
            mStep = STEP_CHOOSE_ACCOUNT;
        } else {
            mStep = STEP_CHOOSE_REPO;
        }

        checkLoginState();
    }

    private void initView() {
        binding.swipeRefreshLayout.setOnRefreshListener(this::loadData);

        if (isOnlyChooseRepo) {
            binding.ok.setVisibility(View.GONE);
            binding.newFolder.setVisibility(View.GONE);
        }

        binding.ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onOkClick();
            }
        });

        binding.newFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showNewDirDialog();
            }
        });

        binding.cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    private void checkLoginState() {
        boolean isLogin = SupportAccountManager.getInstance().isLogin();
        if (!isLogin) {
            binding.ok.setEnabled(false);
            binding.newFolder.setEnabled(false);
        }

        loadData();
    }

    private void onOkClick() {
        if (!mNavContext.inRepo()) {
            ToastUtils.showLong(R.string.choose_a_library);
            return;
        }

        String repoName = mNavContext.getRepoModel().repo_name;
        String repoID = mNavContext.getRepoModel().repo_id;
        String dir = mNavContext.getNavPath();

        Intent intent = new Intent();
        intent.putExtra(DATA_REPO_NAME, repoName);
        intent.putExtra(DATA_REPO_ID, repoID);
        intent.putExtra(DATA_DIR, dir);
        intent.putExtra(DATA_ACCOUNT, mAccount);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void initViewModel() {
        viewModel.getRefreshLiveData().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                binding.swipeRefreshLayout.setRefreshing(aBoolean);
            }
        });

        viewModel.getObjsListLiveData().observe(this, new Observer<List<BaseModel>>() {
            @Override
            public void onChanged(List<BaseModel> baseModels) {
                notifyDataChanged(baseModels);
            }
        });
    }

    private void initRv() {
        StickyItemDecoration decoration = new StickyItemDecoration.Builder()
                .itemType(AbsLayoutItemType.GROUP_ITEM)
                .build();

        binding.rv.addItemDecoration(decoration);

        adapter = new RepoQuickAdapter();
        adapter.setSelectType(RepoSelectType.DIR);
        adapter.setFileViewType(FileViewType.LIST);

        adapter.setOnItemClickListener((baseQuickAdapter, view, i) -> {
            BaseModel baseModel = adapter.getItems().get(i);
            onItemClick(baseModel);
        });

        QuickAdapterHelper helper = new QuickAdapterHelper.Builder(adapter).build();
        binding.rv.setAdapter(helper.getAdapter());
    }

    private void notifyDataChanged(List<BaseModel> models) {
        if (CollectionUtils.isEmpty(models)) {
            showEmptyTip();
        } else {
            adapter.notifyDataChanged(models);
        }
    }

    private void onItemClick(BaseModel baseModel) {
        if (baseModel instanceof Account) {

            mAccount = (Account) baseModel;
            mStep = STEP_CHOOSE_REPO;
            loadData();
        } else if (baseModel instanceof RepoModel) {

            RepoModel repoModel = (RepoModel) baseModel;
            if (repoModel.encrypted) {
                doEncrypt(repoModel);
            } else {
                mStep = STEP_CHOOSE_DIR;
                mNavContext.push(repoModel);
                loadData();
            }

        } else if (baseModel instanceof DirentModel) {
            DirentModel model = (DirentModel) baseModel;
            if (!model.isDir()) {
                return;
            }

            mNavContext.push(model);
            loadData();
        }


    }

    private void doEncrypt(RepoModel repoModel) {
        viewModel.getEncCacheDB(repoModel.repo_id, new Consumer<EncKeyCacheEntity>() {
            @Override
            public void accept(EncKeyCacheEntity encKeyCacheEntity) throws Exception {
                long now = TimeUtils.getNowMills();
                if (encKeyCacheEntity == null || encKeyCacheEntity.expire_time_long == 0) {
                    showPasswordDialog(repoModel);
                } else if (now < encKeyCacheEntity.expire_time_long) {
                    mStep = STEP_CHOOSE_DIR;
                    mNavContext.push(repoModel);
                    loadData();
                } else {
                    showPasswordDialog(repoModel);
                }
            }
        });
    }

    private void showPasswordDialog(RepoModel repoModel) {
        PasswordDialogFragment dialogFragment = PasswordDialogFragment.newInstance(repoModel.repo_id, repoModel.repo_name);
        dialogFragment.setResultListener(new OnResultListener<RepoModel>() {
            @Override
            public void onResultData(RepoModel uRepoModel) {
                mStep = STEP_CHOOSE_DIR;
                mNavContext.push(repoModel);
                loadData();
            }
        });

        dialogFragment.show(getSupportFragmentManager(), PasswordDialogFragment.class.getSimpleName());
    }

    private void checkCurrentPathHasWritePermission(java.util.function.Consumer<Boolean> consumer) {
        BaseModel model = mNavContext.getTopModel();

        String repo_id = null;
        int pNum = 0;
        if (model instanceof RepoModel m) {
            if (!m.isCustomPermission()) {
                consumer.accept(m.hasWritePermission());
                return;
            } else {
                repo_id = m.repo_id;
                pNum = m.getCustomPermissionNum();
            }
        } else if (model instanceof DirentModel m) {
            if (!m.isCustomPermission()) {
                consumer.accept(m.hasWritePermission());
                return;
            } else {
                repo_id = m.repo_id;
                pNum = m.getCustomPermissionNum();
            }
        } else {
            consumer.accept(false);
            return;
        }

        viewModel.getPermissionFromLocal(repo_id, pNum, new Consumer<PermissionEntity>() {
            @Override
            public void accept(PermissionEntity entity) throws Exception {
                if (!entity.isValid()) {
                    consumer.accept(false);
                    return;
                }

                consumer.accept(entity.create);
            }
        });
    }

    private void showNewDirDialog() {
        if (!mNavContext.inRepo()) {
            ToastUtils.showLong(R.string.choose_a_library);
            return;
        }

        checkCurrentPathHasWritePermission(aBoolean -> {


            String rid = mNavContext.getRepoModel().repo_id;
            String parentPath = mNavContext.getNavPath();
            NewDirFileDialogFragment dialogFragment = NewDirFileDialogFragment.newInstance(rid, parentPath, true);
            dialogFragment.setRefreshListener(new OnRefreshDataListener() {
                @Override
                public void onActionStatus(boolean isDone) {
                    if (isDone) {
                        loadData();
                    }
                }
            });
            dialogFragment.show(getSupportFragmentManager(), NewDirFileDialogFragment.class.getSimpleName());
        });
    }

    private void loadData() {
        // update action bar
        ActionBar bar = getSupportActionBar();
        if (bar == null) {
            return;
        }

        if (mStep == STEP_CHOOSE_ACCOUNT) {

            bar.setDisplayHomeAsUpEnabled(false);
            bar.setTitle(R.string.choose_an_account);

            viewModel.loadAccount();
        } else if (mStep == STEP_CHOOSE_REPO) {

            bar.setDisplayHomeAsUpEnabled(true);
            bar.setTitle(R.string.choose_a_library);

            viewModel.loadReposFromNet(mAccount, false);
        } else if (mStep == STEP_CHOOSE_DIR) {

            bar.setDisplayHomeAsUpEnabled(true);
            bar.setTitle(R.string.choose_a_folder);

            viewModel.loadDirentsFromNet(mAccount, mNavContext);
        }
    }

    private void showEmptyTip() {
        if (mStep == STEP_CHOOSE_ACCOUNT) {
            showAdapterTip(R.string.no_account);
        } else if (mStep == STEP_CHOOSE_REPO) {
            showAdapterTip(R.string.no_repo);
        } else if (mStep == STEP_CHOOSE_DIR) {
            showAdapterTip(R.string.dir_empty);
        }
    }

    private void showAdapterTip(int textRes) {
        adapter.submitList(null);
        TextView tipView = TipsViews.getTipTextView(this);
        tipView.setText(textRes);
        adapter.setStateView(tipView);
        adapter.setStateViewEnable(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            stepBack();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void stepBack() {
        switch (mStep) {
            case STEP_CHOOSE_ACCOUNT: {
                setResult(RESULT_CANCELED);
                finish();
            }
            break;
            case STEP_CHOOSE_REPO: {
                if (canChooseAccount) {
                    mStep = STEP_CHOOSE_ACCOUNT;
                    loadData();
                } else {
                    setResult(RESULT_CANCELED);
                    finish();
                }
            }
            break;
            case STEP_CHOOSE_DIR: {
                if (mNavContext.inRepoRoot()) {
                    mStep = STEP_CHOOSE_REPO;
                } else {
                    mNavContext.pop();
                }
                loadData();
            }
            break;
        }
    }


}
