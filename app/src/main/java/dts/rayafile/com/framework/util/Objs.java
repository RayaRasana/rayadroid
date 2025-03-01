package dts.rayafile.com.framework.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.text.TextUtils;

import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.blankj.utilcode.util.ClipboardUtils;
import com.blankj.utilcode.util.CollectionUtils;
import com.blankj.utilcode.util.TimeUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.google.common.collect.Lists;
import dts.rayafile.com.BuildConfig;
import dts.rayafile.com.R;
import dts.rayafile.com.SeadroidApplication;
import dts.rayafile.com.account.Account;
import dts.rayafile.com.config.RepoType;
import dts.rayafile.com.enums.SortBy;
import dts.rayafile.com.enums.TransferStatus;
import dts.rayafile.com.framework.data.db.AppDatabase;
import dts.rayafile.com.framework.data.db.entities.DirentModel;
import dts.rayafile.com.framework.data.db.entities.FileTransferEntity;
import dts.rayafile.com.framework.data.db.entities.RepoModel;
import dts.rayafile.com.framework.data.db.entities.StarredModel;
import dts.rayafile.com.framework.data.model.BaseModel;
import dts.rayafile.com.framework.data.model.GroupItemModel;
import dts.rayafile.com.framework.data.model.objs.DirentShareLinkModel;
import dts.rayafile.com.framework.data.model.repo.DirentWrapperModel;
import dts.rayafile.com.framework.data.model.repo.RepoWrapperModel;
import dts.rayafile.com.framework.data.model.star.StarredWrapperModel;
import dts.rayafile.com.framework.http.HttpIO;
import dts.rayafile.com.listener.OnCreateDirentShareLinkListener;
import dts.rayafile.com.preferences.Settings;
import dts.rayafile.com.ui.dialog_fragment.AppChoiceDialogFragment;
import dts.rayafile.com.ui.dialog_fragment.GetShareLinkPasswordDialogFragment;
import dts.rayafile.com.ui.repo.RepoService;
import dts.rayafile.com.ui.comparator.NaturalOrderComparator;
import dts.rayafile.com.ui.star.StarredService;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.SingleSource;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import kotlin.Pair;
import kotlin.Triple;

public class Objs {

    ////////////////////////////
    //////starred
    ////////////////////////////

    public static Single<List<StarredModel>> getStarredSingleFromServer(Account account) {
        Single<StarredWrapperModel> netSingle = HttpIO.getInstanceByAccount(account).execute(StarredService.class).getStarItems();
        Completable completable = AppDatabase.getInstance().starredDirentDAO().deleteAllByAccount(account.getSignature());
        Single<Integer> deleteSingle = completable.toSingleDefault(0);
        return Single.zip(netSingle, deleteSingle, new BiFunction<StarredWrapperModel, Integer, List<StarredModel>>() {
            @Override
            public List<StarredModel> apply(StarredWrapperModel starredWrapperModel, Integer integer) throws Exception {
                for (StarredModel starredModel : starredWrapperModel.starred_item_list) {
                    starredModel.related_account = account.getSignature();
                    if (!TextUtils.isEmpty(starredModel.mtime)) {
                        starredModel.mtime_long = Times.convertMtime2Long(starredModel.mtime);
                    }
                }

                return starredWrapperModel.starred_item_list;
            }
        }).flatMap(new Function<List<StarredModel>, SingleSource<List<StarredModel>>>() {
            @Override
            public SingleSource<List<StarredModel>> apply(List<StarredModel> starredModels) throws Exception {

                AppDatabase.getInstance().starredDirentDAO().insertAllSync(starredModels);
                return Single.just(starredModels);
            }
        });
    }

    ////////////////////////////
    //////repo
    ////////////////////////////
    public static Single<List<BaseModel>> getReposSingleFromServer(Account account) {
        Single<RepoWrapperModel> netSingle = HttpIO.getInstanceByAccount(account).execute(RepoService.class).getReposAsync();
        Single<List<RepoModel>> dbListSingle = AppDatabase.getInstance().repoDao().getListByAccount(account.getSignature());

        return Single.zip(netSingle, dbListSingle, new BiFunction<RepoWrapperModel, List<RepoModel>, List<RepoModel>>() {
            @Override
            public List<RepoModel> apply(RepoWrapperModel repoWrapperModel, List<RepoModel> dbModels) throws Exception {
                //get data from server and convert to local data

                return Objs.convertRemoteListToLocalList(repoWrapperModel.repos, account.getSignature());
            }
        }).flatMap(new Function<List<RepoModel>, SingleSource<List<RepoModel>>>() {
            @Override
            public SingleSource<List<RepoModel>> apply(List<RepoModel> localList) throws Exception {
                // delete local db
                Completable deleteCompletable = AppDatabase.getInstance().repoDao().deleteAll();
                Single<Long> deleteSingle = deleteCompletable.toSingleDefault(0L);

                return deleteSingle.flatMap(new Function<Long, SingleSource<List<RepoModel>>>() {
                    @Override
                    public SingleSource<List<RepoModel>> apply(Long aLong) throws Exception {
                        return Single.just(localList);
                    }
                });
            }
        }).flatMap(new Function<List<RepoModel>, SingleSource<List<RepoModel>>>() {
            @Override
            public SingleSource<List<RepoModel>> apply(List<RepoModel> localList) throws Exception {
                //insert into db

                if (CollectionUtils.isEmpty(localList)) {
                    return Single.just(localList);
                }

                Completable insertCompletable = AppDatabase.getInstance().repoDao().insertAll(localList);
                Single<Long> longSingle = insertCompletable.toSingleDefault(0L);
                return longSingle.flatMap(new Function<Long, SingleSource<List<RepoModel>>>() {
                    @Override
                    public SingleSource<List<RepoModel>> apply(Long aLong) throws Exception {
                        return Single.just(localList);
                    }
                });
            }
        }).flatMap(new Function<List<RepoModel>, SingleSource<List<BaseModel>>>() {
            @Override
            public SingleSource<List<BaseModel>> apply(List<RepoModel> localList) throws Exception {
                //parse to adapter list data

                List<BaseModel> models = Objs.convertToAdapterList(localList, false);
                return Single.just(models);
            }
        });
    }

    public static Single<List<BaseModel>> getReposSingleFromServerOld(Account account) {
        Single<RepoWrapperModel> netSingle = HttpIO.getInstanceByAccount(account).execute(RepoService.class).getReposAsync();
        Single<List<RepoModel>> dbListSingle = AppDatabase.getInstance().repoDao().getListByAccount(account.getSignature());

        return Single.zip(netSingle, dbListSingle, new BiFunction<RepoWrapperModel, List<RepoModel>, Triple<RepoWrapperModel, List<RepoModel>, List<RepoModel>>>() {
            @Override
            public Triple<RepoWrapperModel, List<RepoModel>, List<RepoModel>> apply(RepoWrapperModel repoWrapperModel, List<RepoModel> dbModels) throws Exception {
                //get data from server and local

                List<RepoModel> net2dbList = Objs.parseRepoListForDbOld(repoWrapperModel.repos, account.getSignature());

                //diffs.first = delete list
                //diffs.second = insert db list
                Pair<List<RepoModel>, List<RepoModel>> diffs = Objs.diffRepos(net2dbList, dbModels);
                if (diffs == null) {
                    return new Triple<>(repoWrapperModel, null, null);
                }

                return new Triple<>(repoWrapperModel, diffs.getFirst(), diffs.getSecond());
            }
        }).flatMap(new Function<Triple<RepoWrapperModel, List<RepoModel>, List<RepoModel>>, SingleSource<Pair<RepoWrapperModel, List<RepoModel>>>>() {
            @Override
            public SingleSource<Pair<RepoWrapperModel, List<RepoModel>>> apply(Triple<RepoWrapperModel, List<RepoModel>, List<RepoModel>> triple) throws Exception {
                // delete local db

                if (CollectionUtils.isEmpty(triple.getSecond())) {
                    return Single.just(new Pair<>(triple.getFirst(), triple.getThird()));
                }

                List<String> ids = triple.getSecond().stream().map(m -> m.repo_id).collect(Collectors.toList());
                Completable deleteCompletable = AppDatabase.getInstance().repoDao().deleteAllByIds(ids);
                Single<Long> deleteSingle = deleteCompletable.toSingleDefault(0L);

                return deleteSingle.flatMap(new Function<Long, SingleSource<Pair<RepoWrapperModel, List<RepoModel>>>>() {
                    @Override
                    public SingleSource<Pair<RepoWrapperModel, List<RepoModel>>> apply(Long aLong) throws Exception {

                        return Single.just(new Pair<>(triple.getFirst(), triple.getThird()));
                    }
                });
            }
        }).flatMap(new Function<Pair<RepoWrapperModel, List<RepoModel>>, SingleSource<RepoWrapperModel>>() {
            @Override
            public SingleSource<RepoWrapperModel> apply(Pair<RepoWrapperModel, List<RepoModel>> pair) throws Exception {
                //insert into db

                if (CollectionUtils.isEmpty(pair.getSecond())) {
                    return Single.just(pair.getFirst());
                }

                Completable insertCompletable = AppDatabase.getInstance().repoDao().insertAll(pair.getSecond());
                Single<Long> longSingle = insertCompletable.toSingleDefault(0L);
                return longSingle.flatMap(new Function<Long, SingleSource<RepoWrapperModel>>() {
                    @Override
                    public SingleSource<RepoWrapperModel> apply(Long aLong) throws Exception {
                        return Single.just(pair.getFirst());
                    }
                });
            }
        }).flatMap(new Function<RepoWrapperModel, SingleSource<List<BaseModel>>>() {
            @Override
            public SingleSource<List<BaseModel>> apply(RepoWrapperModel repoWrapperModel) throws Exception {
                //parse to adapter list data

                List<BaseModel> models = Objs.parseRepoListForAdapterOld(repoWrapperModel.repos, account.getSignature(), false);
                return Single.just(models);
            }
        });
    }

    public static List<BaseModel> convertToAdapterList(List<RepoModel> list, boolean isFilterUnavailable) {
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }

        if (isFilterUnavailable) {
            list = list.stream().filter(f -> !f.encrypted && f.hasWritePermission()).collect(Collectors.toList());
        }

        List<BaseModel> newRvList = CollectionUtils.newArrayList();

        TreeMap<String, List<RepoModel>> treeMap = groupRepos(list);

        //mine
        List<RepoModel> mineList = treeMap.get(RepoType.TYPE_MINE);
        if (!CollectionUtils.isEmpty(mineList)) {
            List<RepoModel> sortedList = sortRepos(mineList);
            newRvList.add(new GroupItemModel(R.string.personal, sortedList));
            newRvList.addAll(sortedList);
        }

        //shared
        List<RepoModel> sharedList = treeMap.get(RepoType.TYPE_SHARED);
        if (!CollectionUtils.isEmpty(sharedList)) {
            List<RepoModel> sortedList = sortRepos(sharedList);

            newRvList.add(new GroupItemModel(R.string.shared, sortedList));
            newRvList.addAll(sortedList);
        }

        for (String key : treeMap.keySet()) {
            if (TextUtils.equals(key, RepoType.TYPE_MINE)) {
            } else if (TextUtils.equals(key, RepoType.TYPE_SHARED)) {
            } else {
                List<RepoModel> groupList = treeMap.get(key);
                if (!CollectionUtils.isEmpty(groupList)) {
                    List<RepoModel> sortedList = sortRepos(groupList);
                    newRvList.add(new GroupItemModel(key, sortedList));
                    newRvList.addAll(sortedList);
                }
            }
        }

        return newRvList;
    }

    public static List<BaseModel> parseRepoListForAdapterOld(List<RepoModel> list, String related_account, boolean isFilterUnavailable) {
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }

        for (int i = 0; i < list.size(); i++) {
            list.get(i).related_account = related_account;
        }

        if (isFilterUnavailable) {
            list = list.stream().filter(f -> !f.encrypted && f.hasWritePermission()).collect(Collectors.toList());
        }

        List<BaseModel> newRvList = CollectionUtils.newArrayList();

        TreeMap<String, List<RepoModel>> treeMap = groupRepos(list);

        //mine
        List<RepoModel> mineList = treeMap.get(RepoType.TYPE_MINE);
        if (!CollectionUtils.isEmpty(mineList)) {
            newRvList.add(new GroupItemModel(R.string.personal));
            for (RepoModel repoModel : mineList) {
                repoModel.last_modified_long = Times.convertMtime2Long(repoModel.last_modified);
            }

            List<RepoModel> sortedList = sortRepos(mineList);
            newRvList.addAll(sortedList);
        }

        //shared
        List<RepoModel> sharedList = treeMap.get(RepoType.TYPE_SHARED);
        if (!CollectionUtils.isEmpty(sharedList)) {
            newRvList.add(new GroupItemModel(R.string.shared));
            for (RepoModel repoModel : sharedList) {
                repoModel.last_modified_long = Times.convertMtime2Long(repoModel.last_modified);
            }

            List<RepoModel> sortedList = sortRepos(sharedList);
            newRvList.addAll(sortedList);
        }

        for (String key : treeMap.keySet()) {
            if (TextUtils.equals(key, RepoType.TYPE_MINE)) {
            } else if (TextUtils.equals(key, RepoType.TYPE_SHARED)) {
            } else {
                List<RepoModel> groupList = treeMap.get(key);
                if (!CollectionUtils.isEmpty(groupList)) {
                    newRvList.add(new GroupItemModel(key));
                    for (RepoModel repoModel : groupList) {
                        repoModel.last_modified_long = Times.convertMtime2Long(repoModel.last_modified);
                    }

                    List<RepoModel> sortedList = sortRepos(groupList);
                    newRvList.addAll(sortedList);
                }
            }
        }

        return newRvList;
    }

    public static List<RepoModel> convertRemoteListToLocalList(List<RepoModel> remoteList, String related_account) {
        if (CollectionUtils.isEmpty(remoteList)) {
            return Collections.emptyList();
        }

        for (RepoModel repoModel : remoteList) {
            repoModel.related_account = related_account;
            repoModel.last_modified_long = Times.convertMtime2Long(repoModel.last_modified);
        }

        return remoteList;
    }

    public static List<RepoModel> parseRepoListForDbOld(List<RepoModel> list, String related_account) {
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }

        for (int i = 0; i < list.size(); i++) {
            list.get(i).related_account = related_account;
        }

        List<RepoModel> newDbList = CollectionUtils.newArrayList();

        TreeMap<String, List<RepoModel>> treeMap = groupRepos(list);

        //mine
        List<RepoModel> mineList = treeMap.get(RepoType.TYPE_MINE);
        if (!CollectionUtils.isEmpty(mineList)) {
            for (RepoModel repoModel : mineList) {
                repoModel.last_modified_long = Times.convertMtime2Long(repoModel.last_modified);
            }

            List<RepoModel> sortedList = sortRepos(mineList);
            newDbList.addAll(sortedList);
        }

        //shared
        List<RepoModel> sharedList = treeMap.get(RepoType.TYPE_SHARED);
        if (!CollectionUtils.isEmpty(sharedList)) {
            for (RepoModel repoModel : sharedList) {
                repoModel.last_modified_long = Times.convertMtime2Long(repoModel.last_modified);
            }

            List<RepoModel> sortedList = sortRepos(sharedList);
            newDbList.addAll(sortedList);
        }

        for (String key : treeMap.keySet()) {
            if (TextUtils.equals(key, RepoType.TYPE_MINE)) {
            } else if (TextUtils.equals(key, RepoType.TYPE_SHARED)) {
            } else {
                List<RepoModel> groupList = treeMap.get(key);
                if (!CollectionUtils.isEmpty(groupList)) {
                    for (RepoModel repoModel : groupList) {
                        repoModel.last_modified_long = Times.convertMtime2Long(repoModel.last_modified);
                    }

                    List<RepoModel> sortedList = sortRepos(groupList);
                    newDbList.addAll(sortedList);
                }
            }
        }
        return newDbList;
    }

    /**
     * Whether the dbList is included in the netList.<br>
     * pair.first is need to delete.<br>
     * pair.second is need to add.<br>
     */
    public static Pair<List<RepoModel>, List<RepoModel>> diffRepos(List<RepoModel> netList, List<RepoModel> dbList) {

        if (CollectionUtils.isEmpty(netList) && CollectionUtils.isEmpty(dbList)) {
            return null;
        }

        //if netList is empty, delete all local data.
        if (CollectionUtils.isEmpty(netList)) {
            return new Pair<>(dbList, null);
        }

        //if dbList is empty, insert all net data into DB.
        if (CollectionUtils.isEmpty(dbList)) {
            return new Pair<>(null, netList);
        }

        List<String> repoIds = netList.stream().map(m -> m.repo_id).collect(Collectors.toList());
        List<RepoModel> deleteList = dbList.stream().filter(f -> !repoIds.contains(f.repo_id)).collect(Collectors.toList());
        List<RepoModel> addList = dbList.stream().filter(f -> repoIds.contains(f.repo_id)).collect(Collectors.toList());

        for (RepoModel nModel : netList) {
            for (RepoModel repoModel : addList) {
                if (TextUtils.equals(nModel.repo_id, repoModel.repo_id)) {
                    nModel.root = repoModel.root;
                    nModel.magic = repoModel.magic;
                    nModel.random_key = repoModel.random_key;
                    nModel.enc_version = repoModel.enc_version;
                    nModel.file_count = repoModel.file_count;
                    break;
                }
            }
        }

        return new Pair<>(deleteList, netList);
    }

    private static List<RepoModel> sortRepos(List<RepoModel> repos) {
        List<RepoModel> newRepos = new ArrayList<>();

        SortBy by = Settings.FILE_LIST_SORT_BY.queryValue();
        boolean isAscending = Settings.FILE_LIST_SORT_ASCENDING.queryValue();

        if (SortBy.NAME == by) {
            if (isAscending) {
                newRepos = repos.stream().sorted(new NaturalOrderComparator()).collect(Collectors.toList());
            } else {
                newRepos = repos.stream().sorted(new NaturalOrderComparator().reversed()).collect(Collectors.toList());
            }
        } else if (SortBy.TYPE == by) {
            //todo not supported
        } else if (SortBy.SIZE == by) {
            newRepos = repos.stream().sorted(new Comparator<RepoModel>() {
                @Override
                public int compare(RepoModel o1, RepoModel o2) {
                    if (isAscending) {
                        return Long.compare(o1.size, o2.size);
                    }
                    return -Long.compare(o1.size, o2.size);
                }
            }).collect(Collectors.toList());
        } else if (SortBy.LAST_MODIFIED == by) {
            newRepos = repos.stream().sorted(new Comparator<RepoModel>() {
                @Override
                public int compare(RepoModel o1, RepoModel o2) {

                    if (isAscending) {
                        return Long.compare(o1.last_modified_long, o2.last_modified_long);
                    }
                    return -Long.compare(o1.last_modified_long, o2.last_modified_long);
                }
            }).collect(Collectors.toList());
        }
        return newRepos;
    }

    private static TreeMap<String, List<RepoModel>> groupRepos(List<RepoModel> repos) {
        TreeMap<String, List<RepoModel>> map = new TreeMap<String, List<RepoModel>>();
        for (RepoModel repo : repos) {
            if (TextUtils.equals(repo.type, RepoType.TYPE_GROUP)) {
                List<RepoModel> l = map.computeIfAbsent(repo.group_name, k -> Lists.newArrayList());
                l.add(repo);
            } else {
                List<RepoModel> l = map.computeIfAbsent(repo.type, k -> Lists.newArrayList());
                l.add(repo);
            }
        }
        return map;
    }

    ////////////////////////////
    //////dirent
    ////////////////////////////
    public static Single<List<DirentModel>> getDirentsSingleFromServer(Account account, String repoId, String repoName, String parentDir) {

        Single<DirentWrapperModel> netSingle = HttpIO.getInstanceByAccount(account).execute(RepoService.class).getDirentsAsync(repoId, parentDir);
        return netSingle.flatMap(new Function<DirentWrapperModel, SingleSource<List<DirentModel>>>() {
            @Override
            public SingleSource<List<DirentModel>> apply(DirentWrapperModel direntWrapperModel) throws Exception {
                return Single.create(new SingleOnSubscribe<List<DirentModel>>() {
                    @Override
                    public void subscribe(SingleEmitter<List<DirentModel>> emitter) throws Exception {
                        List<DirentModel> list = Objs.parseDirentsForDB(
                                direntWrapperModel.dirent_list,
                                direntWrapperModel.dir_id,
                                account.getSignature(),
                                repoId,
                                repoName);

                        emitter.onSuccess(list);
                    }
                });
            }
        }).flatMap(new Function<List<DirentModel>, SingleSource<List<DirentModel>>>() {
            @Override
            public SingleSource<List<DirentModel>> apply(List<DirentModel> netModels) throws Exception {
                Completable deleted = AppDatabase.getInstance().direntDao().deleteAllByParentPath(repoId, parentDir);
                Single<Long> deleteAllByPathSingle = deleted.toSingleDefault(0L);
                return deleteAllByPathSingle.flatMap(new Function<Long, SingleSource<List<DirentModel>>>() {
                    @Override
                    public SingleSource<List<DirentModel>> apply(Long aLong) throws Exception {
                        return Single.just(netModels);
                    }
                });
            }
        }).flatMap(new Function<List<DirentModel>, SingleSource<List<DirentModel>>>() {
            @Override
            public SingleSource<List<DirentModel>> apply(List<DirentModel> direntModels) throws Exception {
                if (CollectionUtils.isEmpty(direntModels)) {
                    return Single.just(direntModels);
                }

                Completable insertCompletable = AppDatabase.getInstance().direntDao().insertAll(direntModels);
                Single<Long> insertAllSingle = insertCompletable.toSingleDefault(0L);
                return insertAllSingle.flatMap(new Function<Long, SingleSource<List<DirentModel>>>() {
                    @Override
                    public SingleSource<List<DirentModel>> apply(Long aLong) throws Exception {
                        SLogs.d("The list has been inserted into the local database");
                        return Single.just(direntModels);
                    }
                });
            }
        }).flatMap(new Function<List<DirentModel>, SingleSource<List<DirentModel>>>() {
            @Override
            public SingleSource<List<DirentModel>> apply(List<DirentModel> direntModels) throws Exception {
                if (CollectionUtils.isEmpty(direntModels)) {
                    return Single.just(direntModels);
                }

                Single<List<FileTransferEntity>> curParentDownloadedList = AppDatabase.getInstance().fileTransferDAO().getDownloadedListByParentAsync(repoId, parentDir);

                return curParentDownloadedList.flatMap(new Function<List<FileTransferEntity>, SingleSource<List<DirentModel>>>() {
                    @Override
                    public SingleSource<List<DirentModel>> apply(List<FileTransferEntity> fileTransferEntities) throws Exception {

                        Map<String, FileTransferEntity> transferMap = new HashMap<>(fileTransferEntities.size());
                        for (FileTransferEntity fileTransferEntity : fileTransferEntities) {
                            transferMap.put(fileTransferEntity.full_path, fileTransferEntity);
                        }

                        for (DirentModel direntModel : direntModels) {
                            String fullPath = direntModel.parent_dir + direntModel.name;

                            if (!transferMap.containsKey(fullPath)) {
                                continue;
                            }

                            FileTransferEntity entity = transferMap.get(fullPath);
                            if (entity.transfer_status == TransferStatus.SUCCEEDED) {
                                direntModel.transfer_status = entity.transfer_status;
                                direntModel.local_file_path = entity.target_path;
                            }
                        }

                        return Single.just(direntModels);
                    }
                });
            }
        });
    }

    /**
     * Resolve the dirents of the local database
     */
    public static List<BaseModel> parseLocalDirents(List<DirentModel> list) {
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }

        TreeMap<String, List<DirentModel>> treeMap = groupDirents(list);
        List<DirentModel> dirModels = treeMap.get("dir");
        List<DirentModel> fileModels = treeMap.get("file");

        List<DirentModel> newList = new ArrayList<>();

        boolean isFolderFirst = Settings.FILE_LIST_SORT_FOLDER_FIRST.queryValue();
        if (isFolderFirst) {
            if (!CollectionUtils.isEmpty(dirModels)) {
                newList.addAll(sortDirents(dirModels));
            }

            if (!CollectionUtils.isEmpty(fileModels)) {
                newList.addAll(sortDirents(fileModels));
            }
        } else {
            if (!CollectionUtils.isEmpty(fileModels)) {
                newList.addAll(sortDirents(fileModels));
            }

            if (!CollectionUtils.isEmpty(dirModels)) {
                newList.addAll(sortDirents(dirModels));
            }
        }


        return new ArrayList<>(newList);
    }

    public static List<DirentModel> parseDirentsForDB(List<DirentModel> list,
                                                      String dir_id,
                                                      String related_account,
                                                      String repo_id,
                                                      String repo_name) {

        boolean isFolderFirst = Settings.FILE_LIST_SORT_FOLDER_FIRST.queryValue();
        return parseDirentsForDB(list, dir_id, related_account, repo_id, repo_name, isFolderFirst);
    }

    /**
     * Resolve to a list of local databases
     */
    public static List<DirentModel> parseDirentsForDB(List<DirentModel> list,
                                                      String dir_id,
                                                      String related_account,
                                                      String repo_id,
                                                      String repo_name,
                                                      boolean isFolderFirst) {
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }

        TreeMap<String, List<DirentModel>> treeMap = groupDirents(list);
        List<DirentModel> dirModels = treeMap.get("dir");
        List<DirentModel> fileModels = treeMap.get("file");

        List<DirentModel> newDbList = new ArrayList<>();


        List<DirentModel> dirList = null;
        List<DirentModel> fileList = null;


        long now = TimeUtils.getNowMills();
        if (!CollectionUtils.isEmpty(dirModels)) {
            for (int i = 0; i < dirModels.size(); i++) {
                //
                dirModels.get(i).last_modified_at = now;
                dirModels.get(i).dir_id = dir_id;
                dirModels.get(i).related_account = related_account;
                dirModels.get(i).repo_id = repo_id;
                dirModels.get(i).repo_name = repo_name;
                dirModels.get(i).full_path = dirModels.get(i).parent_dir + dirModels.get(i).name;
                dirModels.get(i).uid = dirModels.get(i).getUID();
            }
            dirList = sortDirents(dirModels);
        }

        if (!CollectionUtils.isEmpty(fileModels)) {
            for (int i = 0; i < fileModels.size(); i++) {
                //
                fileModels.get(i).repo_id = repo_id;
                fileModels.get(i).repo_name = repo_name;
                fileModels.get(i).last_modified_at = now;
                fileModels.get(i).dir_id = dir_id;
                fileModels.get(i).related_account = related_account;
                fileModels.get(i).full_path = fileModels.get(i).parent_dir + fileModels.get(i).name;
                fileModels.get(i).uid = fileModels.get(i).getUID();
            }
            fileList = sortDirents(fileModels);
        }

        if (isFolderFirst) {
            if (!CollectionUtils.isEmpty(dirList)) {
                newDbList.addAll(dirList);
            }
            if (!CollectionUtils.isEmpty(fileList)) {
                newDbList.addAll(fileList);
            }

        } else {
            if (!CollectionUtils.isEmpty(fileList)) {
                newDbList.addAll(fileList);
            }
            if (!CollectionUtils.isEmpty(dirList)) {
                newDbList.addAll(dirList);
            }

        }
        return newDbList;
    }

    private static TreeMap<String, List<DirentModel>> groupDirents(List<DirentModel> list) {
        TreeMap<String, List<DirentModel>> map = new TreeMap<String, List<DirentModel>>();
        for (DirentModel repo : list) {
            List<DirentModel> l = map.computeIfAbsent(repo.type, k -> Lists.newArrayList());
            l.add(repo);
        }
        return map;
    }

    private static List<DirentModel> sortDirents(List<DirentModel> list) {
        List<DirentModel> newList = new ArrayList<>();

        SortBy by = Settings.FILE_LIST_SORT_BY.queryValue();
        boolean isAscending = Settings.FILE_LIST_SORT_ASCENDING.queryValue();

        if (SortBy.NAME == by) {
            if (isAscending) {
                newList = list.stream().sorted(new NaturalOrderComparator()).collect(Collectors.toList());
            } else {
                newList = list.stream().sorted(new NaturalOrderComparator().reversed()).collect(Collectors.toList());
            }
        } else if (SortBy.TYPE == by) {
            //todo not supported
        } else if (SortBy.SIZE == by) {
            newList = list.stream().sorted(new Comparator<DirentModel>() {
                @Override
                public int compare(DirentModel o1, DirentModel o2) {
                    if (isAscending) {
                        return Long.compare(o1.size, o2.size);
                    }
                    return -Long.compare(o1.size, o2.size);
                }
            }).collect(Collectors.toList());
        } else if (SortBy.LAST_MODIFIED == by) {
            newList = list.stream().sorted(new Comparator<DirentModel>() {
                @Override
                public int compare(DirentModel o1, DirentModel o2) {
                    if (isAscending) {
                        return Long.compare(o1.mtime, o2.mtime);
                    }
                    return -Long.compare(o1.mtime, o2.mtime);
                }
            }).collect(Collectors.toList());
        }
        return newList;
    }


    public static List<ResolveInfo> getAppsByIntent(Intent intent) {
        PackageManager pm = SeadroidApplication.getAppContext().getPackageManager();
        List<ResolveInfo> infos = pm.queryIntentActivities(intent, 0);

        // Remove seafile app from the list
        String seadroidPackageName = SeadroidApplication.getAppContext().getPackageName();
        ResolveInfo info;
        Iterator<ResolveInfo> iter = infos.iterator();
        while (iter.hasNext()) {
            info = iter.next();
            if (info.activityInfo.packageName.equals(seadroidPackageName)) {
                iter.remove();
            }
        }

        return infos;
    }

    public static void showChooseAppDialog(Context context, FragmentManager fragmentManager, DirentShareLinkModel shareLinkModel, boolean isDir) {
        String title = context.getString(isDir ? R.string.share_dir_link : R.string.share_file_link);

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        List<ResolveInfo> infos = getAppsByIntent(shareIntent);

        AppChoiceDialogFragment dialog = new AppChoiceDialogFragment();
        dialog.addCustomAction(0,
                ContextCompat.getDrawable(context, R.drawable.copy_link),
                context.getString(R.string.copy_link));
        dialog.init(title, infos, new AppChoiceDialogFragment.OnItemSelectedListener() {
            @Override
            public void onAppSelected(ResolveInfo appInfo) {
                String className = appInfo.activityInfo.name;
                String packageName = appInfo.activityInfo.packageName;
                shareIntent.setClassName(packageName, className);
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareLinkModel.link);
                context.startActivity(shareIntent);
                dialog.dismiss();
            }

            @Override
            public void onCustomActionSelected(AppChoiceDialogFragment.CustomAction action) {
                ClipboardUtils.copyText(shareLinkModel.link);
                ToastUtils.showLong(R.string.link_ready_to_be_pasted);
                dialog.dismiss();
            }
        });
        dialog.show(fragmentManager, AppChoiceDialogFragment.class.getSimpleName());
    }

    public static void showCreateShareLinkDialog(Context context, FragmentManager fragmentManager, DirentModel direntModel, boolean isAdvance) {
        GetShareLinkPasswordDialogFragment dialogFragment = new GetShareLinkPasswordDialogFragment();
        dialogFragment.init(direntModel.repo_id, direntModel.full_path, isAdvance);
        dialogFragment.setOnCreateDirentShareLinkListener(new OnCreateDirentShareLinkListener() {
            @Override
            public void onCreateDirentShareLink(DirentShareLinkModel linkModel) {
                if (linkModel == null) {
                    dialogFragment.dismiss();
                    return;
                }
                showChooseAppDialog(context, fragmentManager, linkModel, direntModel.isDir());
                dialogFragment.dismiss();
            }
        });
        dialogFragment.show(fragmentManager, GetShareLinkPasswordDialogFragment.class.getSimpleName());
    }

    private static ResolveInfo getWeChatIntent(Intent intent) {
        PackageManager pm = SeadroidApplication.getAppContext().getPackageManager();
        List<ResolveInfo> infos = pm.queryIntentActivities(intent, 0);
        for (ResolveInfo info : infos) {
            if (info.activityInfo.packageName.equals("com.tencent.mm")) {
                return info;
            }
        }

        return null;
    }

    /**
     * share link to wechat
     */
    public static void shareDirToWeChat(Fragment context, String repo_id, String full_path) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");

        ResolveInfo weChatInfo = getWeChatIntent(shareIntent);
        if (weChatInfo == null) {
            ToastUtils.showLong(R.string.no_app_available);
            return;
        }

        String className = weChatInfo.activityInfo.name;
        String packageName = weChatInfo.activityInfo.packageName;
        shareIntent.setClassName(packageName, className);

        GetShareLinkPasswordDialogFragment dialogFragment = new GetShareLinkPasswordDialogFragment();
        dialogFragment.init(repo_id, full_path, false);
        dialogFragment.setOnCreateDirentShareLinkListener(new OnCreateDirentShareLinkListener() {
            @Override
            public void onCreateDirentShareLink(DirentShareLinkModel linkModel) {
                if (linkModel == null) {
                    dialogFragment.dismiss();
                    return;
                }

                shareIntent.putExtra(Intent.EXTRA_TEXT, linkModel.link);
                context.startActivity(shareIntent);
                dialogFragment.dismiss();
            }
        });
        dialogFragment.show(context.getChildFragmentManager(), GetShareLinkPasswordDialogFragment.class.getSimpleName());
    }

    /**
     * share file to wachat
     */
    public static void shareFileToWeChat(Fragment context, File file) {

        Uri uri = FileProvider.getUriForFile(context.requireContext(), BuildConfig.FILE_PROVIDER_AUTHORITIES, file);

        final Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.setType(Utils.getFileMimeType(file));
        sendIntent.putExtra(Intent.EXTRA_STREAM, uri);

        ResolveInfo weChatInfo = getWeChatIntent(sendIntent);
        if (weChatInfo == null) {
            ToastUtils.showLong(R.string.no_app_available);
            return;
        }

        String className = weChatInfo.activityInfo.name;
        String packageName = weChatInfo.activityInfo.packageName;
        sendIntent.setClassName(packageName, className);
        context.startActivity(sendIntent);
    }

    /**
     * Export a file.
     * 1. first ask the user to choose an app
     * 2. then download the latest version of the file
     * 3. start the choosen app
     */
    public static void exportFile(Fragment context, File localFile) {

        Uri uri = FileProvider.getUriForFile(context.requireContext(), BuildConfig.FILE_PROVIDER_AUTHORITIES, localFile);

        final Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.setType(Utils.getFileMimeType(localFile));
        sendIntent.putExtra(Intent.EXTRA_STREAM, uri);

        // Get a list of apps
        List<ResolveInfo> infos = getAppsByIntent(sendIntent);
        if (infos.isEmpty()) {
            ToastUtils.showLong(R.string.no_app_available);
            return;
        }

        AppChoiceDialogFragment dialog = new AppChoiceDialogFragment();
        dialog.init(context.getString(R.string.export_file), infos, new AppChoiceDialogFragment.OnItemSelectedListener() {
            @Override
            public void onCustomActionSelected(AppChoiceDialogFragment.CustomAction action) {
            }

            @Override
            public void onAppSelected(ResolveInfo appInfo) {
                String className = appInfo.activityInfo.name;
                String packageName = appInfo.activityInfo.packageName;
                sendIntent.setClassName(packageName, className);

                context.startActivity(sendIntent);
            }
        });
        dialog.show(context.getChildFragmentManager(), AppChoiceDialogFragment.class.getSimpleName());
    }

    public static void exportFiles(Fragment context, List<File> localFiles) {
        if (localFiles == null || localFiles.isEmpty()) {
            ToastUtils.showLong(R.string.no_app_available);
            return;
        }

        ArrayList<Uri> fileUris = new ArrayList<>();
        String mimeType = "*/*"; // Default MIME type

        for (File file : localFiles) {
            Uri uri = FileProvider.getUriForFile(context.requireContext(), BuildConfig.FILE_PROVIDER_AUTHORITIES, file);
            fileUris.add(uri);
        }

        if (fileUris.isEmpty()) {
            ToastUtils.showLong(R.string.no_app_available);
            return;
        }

        if (fileUris.size() == 1) {
            // Single file: Use ACTION_SEND
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType(Utils.getFileMimeType(localFiles.get(0)));
            sendIntent.putExtra(Intent.EXTRA_STREAM, fileUris.get(0));
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            showAppChooser(context, sendIntent);
        } else {
            // Multiple files: Use ACTION_SEND_MULTIPLE
            Intent sendIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            sendIntent.setType(mimeType);
            sendIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris);
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            showAppChooser(context, sendIntent);
        }
    }

    private static void showAppChooser(Fragment context, Intent sendIntent) {
        List<ResolveInfo> infos = getAppsByIntent(sendIntent);
        if (infos.isEmpty()) {
            ToastUtils.showLong(R.string.no_app_available);
            return;
        }

        AppChoiceDialogFragment dialog = new AppChoiceDialogFragment();
        dialog.init(context.getString(R.string.export_file), infos, new AppChoiceDialogFragment.OnItemSelectedListener() {
            @Override
            public void onCustomActionSelected(AppChoiceDialogFragment.CustomAction action) {
            }

            @Override
            public void onAppSelected(ResolveInfo appInfo) {
                String className = appInfo.activityInfo.name;
                String packageName = appInfo.activityInfo.packageName;
                sendIntent.setClassName(packageName, className);
                context.startActivity(sendIntent);
            }
        });
        dialog.show(context.getChildFragmentManager(), AppChoiceDialogFragment.class.getSimpleName());
    }

}
