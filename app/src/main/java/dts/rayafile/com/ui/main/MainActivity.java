package dts.rayafile.com.ui.main;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.CollectionUtils;
import com.blankj.utilcode.util.TimeUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import dts.rayafile.com.R;
import dts.rayafile.com.account.Account;
import dts.rayafile.com.account.SupportAccountManager;
import dts.rayafile.com.context.NavContext;
import dts.rayafile.com.databinding.ActivityMainBinding;
import dts.rayafile.com.enums.FileViewType;
import dts.rayafile.com.enums.NightMode;
import dts.rayafile.com.enums.SortBy;
import dts.rayafile.com.framework.data.ServerInfo;
import dts.rayafile.com.framework.data.db.entities.DirentModel;
import dts.rayafile.com.framework.data.db.entities.EncKeyCacheEntity;
import dts.rayafile.com.framework.data.db.entities.FileTransferEntity;
import dts.rayafile.com.framework.data.db.entities.PermissionEntity;
import dts.rayafile.com.framework.data.db.entities.RepoModel;
import dts.rayafile.com.framework.data.model.BaseModel;
import dts.rayafile.com.framework.data.model.dirents.DirentFileModel;
import dts.rayafile.com.framework.file_monitor.FileSyncService;
import dts.rayafile.com.framework.helper.NightModeHelper;
import dts.rayafile.com.framework.util.PermissionUtil;
import dts.rayafile.com.framework.util.SLogs;
import dts.rayafile.com.framework.util.TakeCameras;
import dts.rayafile.com.framework.util.Utils;
import dts.rayafile.com.framework.worker.BackgroundJobManagerImpl;
import dts.rayafile.com.preferences.Settings;
import dts.rayafile.com.ui.account.AccountsActivity;
import dts.rayafile.com.ui.activities.AllActivitiesFragment;
import dts.rayafile.com.ui.adapter.ViewPager2Adapter;
import dts.rayafile.com.ui.base.BaseActivity;
import dts.rayafile.com.ui.dialog_fragment.NewDirFileDialogFragment;
import dts.rayafile.com.ui.dialog_fragment.NewRepoDialogFragment;
import dts.rayafile.com.ui.dialog_fragment.PasswordDialogFragment;
import dts.rayafile.com.ui.dialog_fragment.listener.OnRefreshDataListener;
import dts.rayafile.com.ui.repo.RepoQuickFragment;
import dts.rayafile.com.ui.transfer_list.TransferActivity;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import io.reactivex.functions.Consumer;
import kotlin.Pair;

public class MainActivity extends BaseActivity {


    @Override
    protected void attachBaseContext(Context newBase) {
        Locale locale = new Locale("fa");
        Locale.setDefault(locale);

        Configuration config = newBase.getResources().getConfiguration();
        config.setLocale(locale);
        config.setLayoutDirection(locale);

        Context context = newBase.createConfigurationContext(config);
        super.attachBaseContext(context);
    }


    public static final int INDEX_LIBRARY_TAB = 0;
    private int last_orientation;

    private ActivityMainBinding binding;
    private FileSyncService syncService;
    private MainViewModel mainViewModel;

    private Account curAccount;

    private NavContext getNavContext() {
        return mainViewModel.getNavContext();
    }

    private RepoQuickFragment getReposFragment() {
        return (RepoQuickFragment) mainViewModel.getFragments().get(0);
    }
    private String getSavedLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE);
        return prefs.getString(getString(R.string.settings_language), "fa"); // Default to "en" if no language is saved
    }
    private void initAppLocale(){
        String l = "fa";
        Locale locale = new Locale(l);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        config.setLayoutDirection(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initAppLocale();
        if (!isTaskRoot()) {
            final Intent intent = getIntent();
            final String intentAction = getIntent().getAction();
            if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && intentAction != null && intentAction.equals(Intent.ACTION_MAIN)) {
                finish();
                return;
            }
        }

        last_orientation = getResources().getConfiguration().orientation;

        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        binding = ActivityMainBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        setSupportActionBar(getActionBarToolbar());
        // enable ActionBar app icon to behave as action back
        enableUpButton(false);

        curAccount = SupportAccountManager.getInstance().getCurrentAccount();
        if (curAccount == null || !curAccount.hasValidToken()) {
            finishAndStartAccountsActivity();
            return;
        }

        initSettings();

        initFireBase();
        initOnBackPressedDispatcher();
        initBottomNavigation();

        initViewPager();

        initViewModel();

        //service
        bindService();

        requestServerInfo(true);
    }

    private void initSettings() {
        Settings.initUserSettings();
    }

    private void initFireBase() {
        Account account = SupportAccountManager.getInstance().getCurrentAccount();
        if (account != null) {
            FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
            crashlytics.setUserId(account.getSignature());
        }
    }

    private void initOnBackPressedDispatcher() {
        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.pager.getCurrentItem() == INDEX_LIBRARY_TAB) {
                    RepoQuickFragment fragment = (RepoQuickFragment) mainViewModel.getFragments().get(0);
                    boolean canBack = fragment.backTo();
                    if (canBack) {

                    } else {
                        finish();
                    }
                } else {
                    finish();
                }
            }
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);


        // handle notification permission on API level >= 33
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // request notification permission first and then prompt for storage permissions
            // storage permissions handled in onRequestPermissionsResult

            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }

        PermissionUtil.requestExternalStoragePermission(this, multiplePermissionLauncher, manageStoragePermissionLauncher);
    }

    public static void startThis(Activity context) {
        Intent i = new Intent(context, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(i);
    }

    public static void navToThis(Context context, String repo_id, String repo_name, String path, boolean is_dir) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("repo_id", repo_id);
        intent.putExtra("repo_name", repo_name);
        intent.putExtra("path", path);
        intent.putExtra("is_dir", is_dir);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    @Override
    public void onRestart() {
        super.onRestart();
        SLogs.d("onRestart");

//        Account cAccount = SupportAccountManager.getInstance().getCurrentAccount();
//        if (curAccount == null || !curAccount.equals(cAccount)) {
//            finishAndStartAccountsActivity();
//        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        BackgroundJobManagerImpl.getInstance().cancelAllJobs();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        SLogs.d("onNewIntent");

        // if the user started the Seadroid app from the Launcher, keep the old Activity
        final String intentAction = intent.getAction();
        if (intent.hasCategory(Intent.CATEGORY_LAUNCHER)
                && intentAction != null
                && intentAction.equals(Intent.ACTION_MAIN)) {
            return;
        }

        Account selectedAccount = SupportAccountManager.getInstance().getCurrentAccount();
        SLogs.d("Current account: " + curAccount);
        if (!curAccount.equals(selectedAccount)) {
            SLogs.d("Account switched, restarting activity.");
            finishAndStartAccountsActivity();
            return;
        }


        String repoId = intent.getStringExtra("repo_id");
        String repoName = intent.getStringExtra("repo_name");
        String path = intent.getStringExtra("path");
        boolean isDir = intent.getBooleanExtra("is_dir", false);

        if (TextUtils.isEmpty(repoId) || TextUtils.isEmpty(path)) {
            return;
        }

        //
        navToPath(repoId, path, isDir);
    }

    private void navToPath(String repoId, String path, boolean isDir) {
        if (!isDir) {
            path = Utils.getParentPath(path);
        }

        String finalPath = path;
        mainViewModel.requestRepoModel(repoId, new Consumer<RepoModel>() {
            @Override
            public void accept(RepoModel repoModel) throws Exception {
                if (repoModel.encrypted) {
                    mainViewModel.getEncCacheDB(repoId, new Consumer<EncKeyCacheEntity>() {
                        @Override
                        public void accept(EncKeyCacheEntity encKeyCacheEntity) throws Exception {
                            //check encrypt and decrypt_expire_time
                            long now = TimeUtils.getNowMills();
                            if (encKeyCacheEntity == null || (repoModel.encrypted && now > encKeyCacheEntity.expire_time_long)) {
                                // expired
                                showPasswordDialog(repoModel, finalPath);
                            } else {
                                getNavContext().switchToPath(repoModel, finalPath);
                                binding.pager.setCurrentItem(0);
                                getReposFragment().loadDataAsFirst();
                                refreshToolbarTitle();
                            }
                        }
                    });

                } else {
                    getNavContext().switchToPath(repoModel, finalPath);
                    binding.pager.setCurrentItem(0);
                    getReposFragment().loadDataAsFirst();
                    refreshToolbarTitle();
                }
            }
        });
    }

    private void initBottomNavigation() {
        binding.navBottomView.setItemHorizontalTranslationEnabled(true);
        binding.navBottomView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                onBottomNavigationSelected(menuItem);
                return true;
            }
        });
    }

    private void onBottomNavigationSelected(MenuItem menuItem) {
        //Invalidate menu
//        supportInvalidateOptionsMenu();

        //tab
        if (menuItem.getItemId() == R.id.tabs_library) {
            if (getNavContext().inRepo()) {
                setActionbarTitle(getNavContext().getLastPathName());
                enableUpButton(true);
            } else {
                enableUpButton(false);
                setActionbarTitle(getString(R.string.tabs_library));
            }

            binding.pager.setCurrentItem(0);
            return;
        }

        if (menuItem.getItemId() == R.id.tabs_starred) {
            enableUpButton(false);
            setActionbarTitle(getString(R.string.tabs_starred));

            binding.pager.setCurrentItem(1);
            return;
        }

        MenuItem activityMenuItem = binding.navBottomView.getMenu().findItem(R.id.tabs_activity);
        if (null == activityMenuItem) {
            if (menuItem.getItemId() == R.id.tabs_settings) {
                enableUpButton(false);
                setActionbarTitle(getString(R.string.settings));

                binding.pager.setCurrentItem(2);
            }
        } else {
            if (menuItem.getItemId() == R.id.tabs_activity) {
                enableUpButton(false);
                setActionbarTitle(getString(R.string.tabs_activity));

                binding.pager.setCurrentItem(2);
            } else if (menuItem.getItemId() == R.id.tabs_settings) {
                enableUpButton(false);
                setActionbarTitle(getString(R.string.settings));

                binding.pager.setCurrentItem(3);
            }
        }
    }

    private void initViewPager() {
        List<Fragment> fragments = mainViewModel.getFragments();
        ViewPager2Adapter viewPager2Adapter = new ViewPager2Adapter(this);
        viewPager2Adapter.addFragments(fragments);
        binding.pager.setOffscreenPageLimit(fragments.size());
        binding.pager.setAdapter(viewPager2Adapter);
        binding.pager.setUserInputEnabled(false);
        binding.pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                onViewPageSelected(position);
            }
        });
    }

    private void onViewPageSelected(int position) {

        if (0 == position) {
            binding.navBottomView.setSelectedItemId(R.id.tabs_library);

            if (menuBinding != null && menuBinding.search != null) {
                menuBinding.search.setVisible(true);
            }

            if (menuBinding != null && menuBinding.sortGroup != null) {
                menuBinding.sortGroup.setVisible(true);
            }

            return;
        }

        if (menuBinding != null) {
            if (menuBinding.search != null) {
                if (menuBinding.search.isActionViewExpanded()) {
                    menuBinding.search.collapseActionView();
                }
                menuBinding.search.setVisible(false);
            }

            if (menuBinding.sortGroup != null) {
                menuBinding.sortGroup.setVisible(false);
            }
        }


        if (1 == position) {
            binding.navBottomView.setSelectedItemId(R.id.tabs_starred);
            return;
        }

        MenuItem activityMenuItem = binding.navBottomView.getMenu().findItem(R.id.tabs_activity);
        if (null == activityMenuItem) {
            //means the server is not pro edition
            if (2 == position) {
                binding.navBottomView.setSelectedItemId(R.id.tabs_settings);
            }
        } else {

            if (2 == position) {
                binding.navBottomView.setSelectedItemId(R.id.tabs_activity);
                return;
            }

            if (3 == position) {
                binding.navBottomView.setSelectedItemId(R.id.tabs_settings);
            }
        }

    }

    private void initViewModel() {
        mainViewModel.getOnForceRefreshRepoListLiveData().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (binding.pager.getCurrentItem() == 0) {
                    RepoQuickFragment fragment = (RepoQuickFragment) mainViewModel.getFragments().get(0);
                    fragment.clearExpireRefreshMap();
                    fragment.loadDataAsFirst();
                }
            }
        });

        mainViewModel.getServerInfoLiveData().observe(this, new Observer<ServerInfo>() {
            @Override
            public void onChanged(ServerInfo serverInfo) {
                requestServerInfo(false);
            }
        });

        mainViewModel.getOnNavContextChangeListenerLiveData().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                refreshActionbar();
            }
        });

        Settings.FILE_LIST_VIEW_TYPE.observe(this, new Observer<FileViewType>() {
            @Override
            public void onChanged(FileViewType fileViewType) {
                updateMenu();
            }
        });

        Settings.NIGHT_MODE.observe(this, new Observer<NightMode>() {
            @Override
            public void onChanged(NightMode nightMode) {
                applyNightMode(nightMode);
            }
        });
//        Settings.LANGUAGE_MODE.observe(this, new Observer<String>() {
//            @Override
//            public void onChanged(String l) {
//                Locale locale = new Locale(l);
//                Locale.setDefault(locale);
//                Configuration config = new Configuration();
//                config.setLocale(locale);
//                config.setLayoutDirection(locale);
//                getResources().updateConfiguration(config, getResources().getDisplayMetrics());
//                restartThis();
//            }
//        });
    }


    private void refreshToolbarTitle() {
        if (!getNavContext().inRepo()) {
            getActionBarToolbar().setTitle(R.string.libraries);
        } else if (getNavContext().inRepoRoot()) {
            getActionBarToolbar().setTitle(getNavContext().getRepoModel().repo_name);
        } else {
            String toolbarTitle = getNavContext().getLastPathName();
            getActionBarToolbar().setTitle(toolbarTitle);
        }
    }

    /////////////////////

    /// service
    /// //////////////////
    private void bindService() {
        Intent syncIntent = new Intent(this, FileSyncService.class);
        bindService(syncIntent, syncConnection, Context.BIND_AUTO_CREATE);
//        startService(syncIntent); //It doesn't need to run in the background
    }

    private final ServiceConnection syncConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FileSyncService.FileSyncBinder binder = (FileSyncService.FileSyncBinder) service;
            syncService = binder.getService();
            SLogs.d("bond FileSyncService");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            syncService = null;
            SLogs.d("FileSyncService disconnected");
        }
    };

    ////////////////////////////
    /// check server info
    ////////////////////////////
    private void requestServerInfo(boolean loadFromNet) {
        Optional<ServerInfo> optional = checkServerInfo();
        if (!optional.isPresent() || !optional.get().isProEdition()) {
            binding.navBottomView.getMenu().removeItem(R.id.tabs_activity);

            // hide Activity tab
            ViewPager2Adapter adapter = (ViewPager2Adapter) binding.pager.getAdapter();
            if (adapter != null) {
                int index = adapter.removeByClass(AllActivitiesFragment.class);
                if (index != -1) {
                    adapter.notifyItemRemoved(index);
                }
            }
        }

        if (loadFromNet) {
            //invalidate local account cache
            mainViewModel.getServerInfo();
        }
    }

    /**
     * @return 0: is pro edition, 1: is search enable
     */
    private Optional<ServerInfo> checkServerInfo() {
        if (curAccount == null)
            return Optional.empty();

        ServerInfo serverInfo = SupportAccountManager.getInstance().getServerInfo(curAccount);
        return Optional.of(serverInfo);
    }

    private void finishAndStartAccountsActivity() {
        Intent newIntent = new Intent(this, AccountsActivity.class);
//        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(newIntent);
        finish();
    }

    ////////////////////////////
    /// menu
    ////////////////////////////
    private void refreshActionbar() {
        if (getNavContext().inRepo()) {

            //refresh back btn state
            enableUpButton(true);

            setActionbarTitle(getNavContext().getLastPathName());
        } else {
            enableUpButton(false);

            if (binding.pager.getCurrentItem() == 0) {
                setActionbarTitle(getString(R.string.tabs_library));
            } else if (binding.pager.getCurrentItem() == 1) {
                setActionbarTitle(getString(R.string.tabs_starred));
            } else if (binding.pager.getCurrentItem() == 2) {
                setActionbarTitle(getString(R.string.tabs_activity));
            } else {
                setActionbarTitle(getString(R.string.settings));
            }
        }

        //refresh toolbar menu
        supportInvalidateOptionsMenu();
    }

    public void setActionbarTitle(String title) {
        if (getSupportActionBar() != null) {
            if (TextUtils.isEmpty(title)) {
                getSupportActionBar().setTitle(null);
            } else {
                getSupportActionBar().setTitle(title);
            }
        }

    }

    private void enableUpButton(boolean isEnable) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(isEnable);
        }
    }

    private MenuBinding menuBinding;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menuBinding = MenuBinding.inflate(menu, getMenuInflater());
        MenuCompat.setGroupDividerEnabled(menu, true);

        initSearchView();
        return true;
    }

    /**
     * to restore the state of Menu
     */
    private final HashMap<String, Boolean> menuIdState = new HashMap<>();

    private void initSearchView() {
        SearchView searchView = new SearchView(this);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mainViewModel.getOnSearchLiveData().setValue(newText);
                return false;
            }
        });

        menuBinding.search.collapseActionView();
        menuBinding.search.setActionView(searchView);
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (binding.pager.getCurrentItem() == INDEX_LIBRARY_TAB) {
            if (getNavContext().inRepo()) {
                menuBinding.createRepo.setVisible(false);

                checkCurrentPathHasWritePermission(new java.util.function.Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) {
                        menuBinding.add.setEnabled(aBoolean);
                    }
                });
            } else {
                menuBinding.createRepo.setVisible(true);
                menuBinding.add.setVisible(false);
            }

            menuBinding.select.setVisible(true);
            menuBinding.sortGroup.setVisible(true);
        } else {
            menuBinding.search.setVisible(false);
            menuBinding.sortGroup.setVisible(false);
            menuBinding.createRepo.setVisible(false);
            menuBinding.add.setVisible(false);
            menuBinding.select.setVisible(false);
        }

        updateMenu();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean isRet = true;
        if (item.getItemId() == android.R.id.home) {
            if (getNavContext().inRepo() && binding.pager.getCurrentItem() == INDEX_LIBRARY_TAB) {
                getOnBackPressedDispatcher().onBackPressed();
            }
        } else if (item.getItemId() == R.id.create_repo) {
            showNewRepoDialog();
        } else if (item.getItemId() == R.id.add) {
            showAddFileDialog();
        } else if (item.getItemId() == R.id.transfer_tasks) {
            Intent newIntent = new Intent(this, TransferActivity.class);
            newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(newIntent);
        } else if (item.getItemId() == R.id.select) {
            if (binding.pager.getCurrentItem() == INDEX_LIBRARY_TAB) {
                getReposFragment().startOrUpdateContextualActionBar();
            }
        } else if (item.getItemId() == R.id.menu_action_view_list) {
            Settings.FILE_LIST_VIEW_TYPE.putValue(FileViewType.LIST);
        } else if (item.getItemId() == R.id.menu_action_view_grid) {
            Settings.FILE_LIST_VIEW_TYPE.putValue(FileViewType.GRID);
        } else if (item.getItemId() == R.id.menu_action_view_gallery) {
            Settings.FILE_LIST_VIEW_TYPE.putValue(FileViewType.GALLERY);
        } else if (item.getItemId() == R.id.menu_action_sort_by_name) {
            Settings.FILE_LIST_SORT_BY.putValue(SortBy.NAME);
        } else if (item.getItemId() == R.id.menu_action_sort_by_type) {
            Settings.FILE_LIST_SORT_BY.putValue(SortBy.TYPE);
        } else if (item.getItemId() == R.id.menu_action_sort_by_size) {
            Settings.FILE_LIST_SORT_BY.putValue(SortBy.SIZE);
        } else if (item.getItemId() == R.id.menu_action_sort_by_last_modified) {
            Settings.FILE_LIST_SORT_BY.putValue(SortBy.LAST_MODIFIED);
        } else if (item.getItemId() == R.id.menu_action_sort_ascending) {
            boolean isChecked = !menuBinding.sortOrderAscending.isChecked();
            Settings.FILE_LIST_SORT_ASCENDING.putValue(isChecked);
        } else if (item.getItemId() == R.id.menu_action_sort_folder_first) {
            boolean isChecked = !menuBinding.sortFolderFirst.isChecked();
            Settings.FILE_LIST_SORT_FOLDER_FIRST.putValue(isChecked);
        } else {
            isRet = false;
        }

        if (isRet) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateMenu() {
        if (menuBinding == null) {
            return;
        }

        menuBinding.search.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(@NonNull MenuItem item) {

                menuIdState.put("sortGroup", menuBinding.sortGroup.isVisible());
                menuIdState.put("createRepo", menuBinding.createRepo.isVisible());
                menuIdState.put("add", menuBinding.add.isVisible());
                menuIdState.put("select", menuBinding.select.isVisible());
                menuIdState.put("transferList", menuBinding.transferList.isVisible());

                menuBinding.sortGroup.setVisible(false);
                menuBinding.createRepo.setVisible(false);
                menuBinding.add.setVisible(false);
                menuBinding.select.setVisible(false);
                menuBinding.transferList.setVisible(false);

                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(@NonNull MenuItem item) {

                menuBinding.sortGroup.setVisible(Boolean.TRUE.equals(menuIdState.get("sortGroup")));
                menuBinding.createRepo.setVisible(Boolean.TRUE.equals(menuIdState.get("createRepo")));
                menuBinding.add.setVisible(Boolean.TRUE.equals(menuIdState.get("add")));
                menuBinding.select.setVisible(Boolean.TRUE.equals(menuIdState.get("select")));
                menuBinding.transferList.setVisible(Boolean.TRUE.equals(menuIdState.get("transferList")));

                invalidateMenu();
                return true;
            }
        });

        //view type
        FileViewType viewType = Settings.FILE_LIST_VIEW_TYPE.queryValue();
        if (FileViewType.LIST == viewType) {
            menuBinding.viewList.setChecked(true);
        } else if (FileViewType.GRID == viewType) {
            menuBinding.viewGrid.setChecked(true);
        } else if (FileViewType.GALLERY == viewType) {
            menuBinding.viewGallery.setChecked(true);
        }

        //sort
        SortBy by = Settings.FILE_LIST_SORT_BY.queryValue();
        if (SortBy.NAME == by) {
            menuBinding.sortByName.setChecked(true);
        } else if (SortBy.TYPE == by) {
            menuBinding.sortByType.setChecked(true);
        } else if (SortBy.SIZE == by) {
            menuBinding.sortBySize.setChecked(true);
        } else if (SortBy.LAST_MODIFIED == by) {
            menuBinding.sortByLastModified.setChecked(true);
        }

        boolean ascendingChecked = Settings.FILE_LIST_SORT_ASCENDING.queryValue();
        menuBinding.sortOrderAscending.setChecked(ascendingChecked);

        boolean folderFirstChecked = Settings.FILE_LIST_SORT_FOLDER_FIRST.queryValue();
        menuBinding.sortFolderFirst.setChecked(folderFirstChecked);
    }

    private static class MenuBinding {
        public Menu menu;

        //sort
        public MenuItem sortGroup;
        public MenuItem sortByName;
        public MenuItem sortByType;
        public MenuItem sortBySize;
        public MenuItem sortByLastModified;
        public MenuItem sortOrderAscending;
        public MenuItem sortFolderFirst;

        //view
        public MenuItem viewList;
        public MenuItem viewGrid;
        public MenuItem viewGallery;

        public MenuItem search;

        public MenuItem createRepo;
        public MenuItem add;
        public MenuItem select;
        public MenuItem transferList;


        public static MenuBinding inflate(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.browser_menu, menu);
            MenuBinding binding1 = new MenuBinding();
            binding1.menu = menu;

            binding1.sortGroup = menu.findItem(R.id.menu_action_sort);

            binding1.sortByName = menu.findItem(R.id.menu_action_sort_by_name);
            binding1.sortByType = menu.findItem(R.id.menu_action_sort_by_type);
            binding1.sortBySize = menu.findItem(R.id.menu_action_sort_by_size);
            binding1.sortByLastModified = menu.findItem(R.id.menu_action_sort_by_last_modified);
            binding1.sortOrderAscending = menu.findItem(R.id.menu_action_sort_ascending);
            binding1.sortFolderFirst = menu.findItem(R.id.menu_action_sort_folder_first);

            binding1.viewList = menu.findItem(R.id.menu_action_view_list);
            binding1.viewGrid = menu.findItem(R.id.menu_action_view_grid);
            binding1.viewGallery = menu.findItem(R.id.menu_action_view_gallery);

            binding1.search = menu.findItem(R.id.menu_action_search);

            binding1.createRepo = menu.findItem(R.id.create_repo);
            binding1.add = menu.findItem(R.id.add);
            binding1.select = menu.findItem(R.id.select);
            binding1.transferList = menu.findItem(R.id.transfer_tasks);
            return binding1;
        }
    }

    /**
     * @see MainActivity -> android:configChanges="uiMode|orientation|screenSize"
     */
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (last_orientation != newConfig.orientation) {
//            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
//                ToastUtils.showLong("LANDSCAPE");
//            } else {
//                ToastUtils.showLong("PORTRAIT");
//            }
            last_orientation = newConfig.orientation;
        } else {
            NightMode nightMode = Settings.NIGHT_MODE.queryValue();
            if (nightMode == NightMode.FOLLOW_SYSTEM) {
                NightMode applyMode;
                int currentNightMode = newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK;
                applyMode = switch (currentNightMode) {
                    case Configuration.UI_MODE_NIGHT_YES -> NightMode.ON;
                    case Configuration.UI_MODE_NIGHT_NO -> NightMode.OFF;
                    default -> NightMode.FOLLOW_SYSTEM;
                };

                applyNightMode(applyMode);
            }
        }
    }


    private void applyNightMode(NightMode nightMode) {
        if (nightMode == null) {
            return;
        }

        NightModeHelper.syncTo(nightMode);

        restartThis();
    }

    private void restartThis() {
        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (intent == null) {
            ActivityUtils.finishToActivity(MainActivity.class, false);
            return;
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    private void showPasswordDialog(RepoModel repoModel, String path) {
        PasswordDialogFragment dialogFragment = PasswordDialogFragment.newInstance(repoModel.repo_id, repoModel.repo_name);
        dialogFragment.setRefreshListener(new OnRefreshDataListener() {
            @Override
            public void onActionStatus(boolean isDone) {
                if (isDone) {
                    getNavContext().switchToPath(repoModel, path);
                    binding.pager.setCurrentItem(0);
                    getReposFragment().loadDataAsFirst();
                    refreshToolbarTitle();
                }
            }
        });
        dialogFragment.show(getSupportFragmentManager(), PasswordDialogFragment.class.getSimpleName());
    }

    /**
     * create a new repo
     */
    private void showNewRepoDialog() {
        NewRepoDialogFragment dialogFragment = new NewRepoDialogFragment();
        dialogFragment.setRefreshListener(new OnRefreshDataListener() {
            @Override
            public void onActionStatus(boolean isDone) {
                if (isDone) {
                    mainViewModel.getOnForceRefreshRepoListLiveData().setValue(true);
                }
            }
        });
        dialogFragment.show(getSupportFragmentManager(), NewRepoDialogFragment.class.getSimpleName());
    }

    /**
     * add new file/files
     */
    private void showAddFileDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(getString(R.string.add_file));
        builder.setItems(R.array.add_file_options_array, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) // create file
                    showNewFileDialog();
                else if (which == 1) // create folder
                    showNewDirDialog();
                else if (which == 2) // upload file
                    pickFile();
                else if (which == 3) // take a photo
                    takePhoto();
            }
        }).show();
    }

    private void checkCurrentPathHasWritePermission(java.util.function.Consumer<Boolean> consumer) {
        BaseModel baseModel = getNavContext().getTopModel();
        if (null == baseModel) {
            return;
        }

        if (baseModel instanceof RepoModel m) {
            if (!m.isCustomPermission()) {
                consumer.accept(m.hasWritePermission());
            } else {
                mainViewModel.getPermissionFromLocal(m.repo_id, m.getCustomPermissionNum(), entity -> {
                    if (entity == null) {
                        consumer.accept(false);
                        return;
                    }
                    consumer.accept(entity.create);
                });
            }
        } else if (baseModel instanceof DirentModel m) {
            if (!m.isCustomPermission()) {
                consumer.accept(m.hasWritePermission());
            } else {
                mainViewModel.getPermissionFromLocal(m.repo_id, m.getCustomPermissionNum(), entity -> {
                    if (entity == null) {
                        consumer.accept(false);
                        return;
                    }
                    consumer.accept(entity.create);
                });
            }
        }
    }

    //
    private void showNewDirDialog() {
        checkCurrentPathHasWritePermission(new java.util.function.Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) {
                if (!aBoolean) {
                    ToastUtils.showLong(R.string.library_read_only);
                    return;
                }

                String rid = getNavContext().getRepoModel().repo_id;
                String parentPath = getNavContext().getNavPath();
                NewDirFileDialogFragment dialogFragment = NewDirFileDialogFragment.newInstance(rid, parentPath, true);
                dialogFragment.setRefreshListener(new OnRefreshDataListener() {
                    @Override
                    public void onActionStatus(boolean isDone) {
                        if (isDone) {
                            mainViewModel.getOnForceRefreshRepoListLiveData().setValue(true);
                        }
                    }
                });
                dialogFragment.show(getSupportFragmentManager(), NewDirFileDialogFragment.class.getSimpleName());
            }
        });
    }

    private void showNewFileDialog() {
        checkCurrentPathHasWritePermission(new java.util.function.Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) {
                if (!aBoolean) {
                    ToastUtils.showLong(R.string.library_read_only);
                    return;
                }


                String rid = getNavContext().getRepoModel().repo_id;
                String parentPath = getNavContext().getNavPath();
                NewDirFileDialogFragment dialogFragment = NewDirFileDialogFragment.newInstance(rid, parentPath, false);
                dialogFragment.setRefreshListener(new OnRefreshDataListener() {
                    @Override
                    public void onActionStatus(boolean isDone) {
                        if (isDone) {
                            mainViewModel.getOnForceRefreshRepoListLiveData().setValue(true);
                        }
                    }
                });
                dialogFragment.show(getSupportFragmentManager(), NewDirFileDialogFragment.class.getSimpleName());
            }
        });
    }

    private void pickFile() {
        checkCurrentPathHasWritePermission(new java.util.function.Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) {
                if (!aBoolean) {
                    ToastUtils.showLong(R.string.library_read_only);
                    return;
                }

                takeFile(false);
            }
        });

    }


    ////////////////////////////
    /// Launcher
    ////////////////////////////

    //0 camera
    //1 video
    private int permission_media_select_type = -1;

    private void takePhoto() {
        checkCurrentPathHasWritePermission(new java.util.function.Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) {
                if (!aBoolean) {
                    ToastUtils.showLong(R.string.library_read_only);
                    return;
                }

                permission_media_select_type = 0;
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

    }

    private void takeVideo() {
        checkCurrentPathHasWritePermission(new java.util.function.Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) {
                if (!aBoolean) {
                    ToastUtils.showLong(R.string.library_read_only);
                    return;
                }

                permission_media_select_type = 1;
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

    }


    private void takeFile(boolean isSingleSelect) {
        String[] mimeTypes = new String[]{"*/*"};
        if (isSingleSelect) {
            singleFileAndImageChooseLauncher.launch(mimeTypes);
        } else {
            multiFileAndImageChooserLauncher.launch(mimeTypes);
        }
    }

    private final ActivityResultLauncher<String[]> multiplePermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
        @Override
        public void onActivityResult(Map<String, Boolean> o) {
            if (o.isEmpty()) {
                return;
            }

            for (Map.Entry<String, Boolean> stringBooleanEntry : o.entrySet()) {
                if (Boolean.FALSE.equals(stringBooleanEntry.getValue())) {
                    ToastUtils.showLong(R.string.get_storage_permission_failed);
                    return;
                }
            }
        }
    });

    private final ActivityResultLauncher<Intent> manageStoragePermissionLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult o) {
            if (!PermissionUtil.checkExternalStoragePermission(MainActivity.this)) {
                ToastUtils.showLong(R.string.get_storage_permission_failed);
            }
        }
    });

    private final ActivityResultLauncher<String> notificationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
        @Override
        public void onActivityResult(Boolean result) {

            //if result is false, the app can't display notifications

            //continue
            PermissionUtil.requestExternalStoragePermission(MainActivity.this, multiplePermissionLauncher, manageStoragePermissionLauncher);
        }
    });

    private final ActivityResultLauncher<String> cameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
        @Override
        public void onActivityResult(Boolean result) {
            if (Boolean.FALSE.equals(result)) {
                ToastUtils.showLong(R.string.permission_camera);
                return;
            }

            if (permission_media_select_type == 0) {
                uriPair = TakeCameras.buildTakePhotoUri(MainActivity.this);
                takePhotoLauncher.launch(uriPair.getFirst());
            } else if (permission_media_select_type == 1) {
                uriPair = TakeCameras.buildTakeVideoUri(MainActivity.this);
                takePhotoLauncher.launch(uriPair.getFirst());
            }
        }
    });

    private final ActivityResultLauncher<String[]> singleFileAndImageChooseLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), new ActivityResultCallback<Uri>() {
        @Override
        public void onActivityResult(Uri o) {
            if (null == o) {
                return;
            }

            doSelectSingleFile(o);
        }
    });

    private final ActivityResultLauncher<String[]> multiFileAndImageChooserLauncher = registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(), new ActivityResultCallback<List<Uri>>() {
        @Override
        public void onActivityResult(List<Uri> o) {
            if (CollectionUtils.isEmpty(o)) {
                return;
            }

            for (Uri uri : o) {
                int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                getContentResolver().takePersistableUriPermission(uri, takeFlags);
            }

            if (o.size() == 1) {
                doSelectSingleFile(o.get(0));
            } else {
                doSelectedMultiFile(o);
            }
        }
    });


    //    private Uri tempTakePhotoUri;
    private Pair<Uri, File> uriPair;
    private final ActivityResultLauncher<Uri> takePhotoLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), new ActivityResultCallback<Boolean>() {
        @Override
        public void onActivityResult(Boolean result) {
            if (Boolean.FALSE.equals(result)) {
                return;
            }

            SLogs.d("take photo");

            if (uriPair == null) {
                return;
            }

            Uri uri = uriPair.getFirst();
            File file = uriPair.getSecond();

            RepoModel repoModel = getNavContext().getRepoModel();

            addUploadTask(repoModel, getNavContext().getNavPath(), file.getAbsolutePath());
        }
    });

    private final ActivityResultLauncher<Uri> takeVideoLauncher = registerForActivityResult(new ActivityResultContracts.CaptureVideo(), new ActivityResultCallback<Boolean>() {
        @Override
        public void onActivityResult(Boolean o) {
            if (!o) {
                return;
            }

            SLogs.d("take video");
        }
    });


    /////////////////////////////
    private void doSelectedMultiFile(List<Uri> uriList) {
        showLoadingDialog();

        try {
            RepoModel repoModel = getNavContext().getRepoModel();
            String parent_dir = getNavContext().getNavPath();

            mainViewModel.checkLocalDirent(curAccount, this, repoModel, parent_dir, uriList, new Consumer<List<Uri>>() {
                @Override
                public void accept(List<Uri> newUris) {

                    dismissLoadingDialog();

                    if (!CollectionUtils.isEmpty(newUris)) {
                        ToastUtils.showLong(R.string.added_to_upload_tasks);

                        //start worker
                        BackgroundJobManagerImpl.getInstance().startFileManualUploadWorker();
                    }
                }
            });

        } catch (Exception e) {
            SLogs.e("Could not open requested document", e);
        }
    }

    private void doSelectSingleFile(Uri uri) {
//        showLoadingDialog();
        try {
            String fileName = Utils.getFilenameFromUri(this, uri);
            String parent_dir = getNavContext().getNavPath();
            String destinationPath = Utils.pathJoin(parent_dir, fileName);

            RepoModel repoModel = getNavContext().getRepoModel();
            mainViewModel.checkRemoteDirent(repoModel.repo_id, destinationPath, new Consumer<DirentFileModel>() {
                @Override
                public void accept(DirentFileModel direntFileModel) throws Exception {
                    if (direntFileModel != null) {
                        showFileExistDialog(uri, fileName);
                    } else {
                        addUploadTask(repoModel, getNavContext().getNavPath(), uri, fileName, false);
                    }

                    dismissLoadingDialog();
                }
            });
        } catch (Exception e) {
            SLogs.e("Could not open requested document", e);
        }
    }

    private void showFileExistDialog(final Uri uri, String fileName) {

        RepoModel repoModel = getNavContext().getRepoModel();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(getString(R.string.upload_file_exist));
        builder.setMessage(String.format(getString(R.string.upload_duplicate_found), fileName));

        builder.setPositiveButton(getString(R.string.upload_replace), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                addUploadTask(repoModel, getNavContext().getNavPath(), uri, fileName, true);
            }
        });

        builder.setNeutralButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.setNegativeButton(getString(R.string.upload_keep_both), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                addUploadTask(repoModel, getNavContext().getNavPath(), uri, fileName, false);
            }
        });

        builder.show();
    }


    ////////////////////////////
    /// task
    ////////////////////////////

    private void addUploadTask(RepoModel repoModel, String targetDir, String localFile) {
        Account account = SupportAccountManager.getInstance().getCurrentAccount();
        mainViewModel.addUploadTask(account, repoModel, targetDir, localFile, false, new Consumer<FileTransferEntity>() {
            @Override
            public void accept(FileTransferEntity transferEntity) throws Exception {
//                if (StringUtils.startsWith(transferEntity.mime_type, "image/")) {
//                } else if (StringUtils.startsWith(transferEntity.mime_type, "video/")) {
//                }

                ToastUtils.showLong(R.string.added_to_upload_tasks);
            }
        });
    }

    private void addUploadTask(RepoModel repoModel, String targetDir, Uri sourceUri, String fileName, boolean isReplace) {
        Account account = SupportAccountManager.getInstance().getCurrentAccount();
        mainViewModel.addUploadTask(account, this, repoModel, targetDir, sourceUri, fileName, isReplace, new Consumer<FileTransferEntity>() {
            @Override
            public void accept(FileTransferEntity fileTransferEntity) {
                ToastUtils.showLong(R.string.added_to_upload_tasks);
            }
        });

    }
}
