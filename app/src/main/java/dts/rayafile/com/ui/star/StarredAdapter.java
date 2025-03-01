package dts.rayafile.com.ui.star;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;

import com.blankj.utilcode.util.CollectionUtils;
import dts.rayafile.com.R;
import dts.rayafile.com.config.GlideLoadConfig;
import dts.rayafile.com.databinding.ItemStarredBinding;
import dts.rayafile.com.framework.data.db.entities.StarredModel;
import dts.rayafile.com.framework.http.HttpIO;
import dts.rayafile.com.framework.util.GlideApp;
import dts.rayafile.com.framework.util.Icons;
import dts.rayafile.com.ui.base.adapter.BaseAdapter;

import java.util.List;

public class StarredAdapter extends BaseAdapter<StarredModel, StarredViewHolder> {
    private final String SERVER = HttpIO.getCurrentInstance().getServerUrl();

    @NonNull
    @Override
    protected StarredViewHolder onCreateViewHolder(@NonNull Context context, @NonNull ViewGroup viewGroup, int i) {
        ItemStarredBinding binding = ItemStarredBinding.inflate(LayoutInflater.from(context), viewGroup, false);
        return new StarredViewHolder(binding);
    }

    @Override
    protected void onBindViewHolder(@NonNull StarredViewHolder holder, int i, @Nullable StarredModel model) {
        if (null == model) {
            return;
        }

        holder.binding.itemTitle.setText(model.obj_name);
        if (model.deleted) {
            holder.binding.itemSubtitle.setTextColor(ContextCompat.getColor(getContext(), R.color.red));
            holder.binding.itemSubtitle.setText(R.string.deleted);
        } else {
            holder.binding.itemSubtitle.setTextColor(ContextCompat.getColor(getContext(), R.color.fancy_black));
            holder.binding.itemSubtitle.setText(model.getSubtitle());
        }

//        MiniatureReasoning
//        ffmpeg -re -i C:\Users\asus\Videos\xiyangyang.mp4 -c copy -f flv "rtmp://live-push.bilivideo.com/live-bvc/?streamname=xxx"

        //set item_icon
        if (model.is_dir) {
            if (TextUtils.equals(model.path, "/")) {
                if (model.repo_encrypted) {
                    holder.binding.itemIcon.setImageResource(R.drawable.baseline_repo_encrypted_24);
                } else {
                    holder.binding.itemIcon.setImageResource(R.drawable.baseline_repo_24);
                }
            } else {
                holder.binding.itemIcon.setImageResource(R.drawable.baseline_folder_24);
            }
        } else {
            if (model.deleted || TextUtils.isEmpty(model.encoded_thumbnail_src) || model.repo_encrypted) {
                holder.binding.itemIcon.setImageResource(Icons.getFileIcon(model.obj_name));
            } else {
                String url = convertThumbnailUrl(model.repo_id, model.path);
                GlideApp.with(getContext())
                        .load(url)
                        .apply(GlideLoadConfig.getOptions())
                        .into(holder.binding.itemIcon);
            }
        }
    }

    private String convertThumbnailUrl(String repoId, String filePath) {
        return String.format("%sapi2/repos/%s/thumbnail/?p=%s&size=%d", SERVER, repoId, filePath, 128);
    }

    public void notifyDataChanged(List<StarredModel> list) {
        if (CollectionUtils.isEmpty(list)) {
            submitList(list);
            return;
        }

        if (CollectionUtils.isEmpty(getItems())) {
            submitList(list);
            return;
        }

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return getItems().size();
            }

            @Override
            public int getNewListSize() {
                return list.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                StarredModel oldModel = getItems().get(oldItemPosition);
                StarredModel newModel = list.get(newItemPosition);
                String oldFullPath = oldModel.path + oldModel.obj_name;
                String newFullPath = newModel.path + newModel.obj_name;

                return TextUtils.equals(oldFullPath, newFullPath);
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                StarredModel oldModel = getItems().get(oldItemPosition);
                StarredModel newModel = list.get(newItemPosition);


                return TextUtils.equals(oldModel.repo_id, newModel.repo_id)
                        && TextUtils.equals(oldModel.repo_name, newModel.repo_name)
                        && TextUtils.equals(oldModel.mtime, newModel.mtime)
                        && TextUtils.equals(oldModel.path, newModel.path)
                        && TextUtils.equals(oldModel.obj_name, newModel.obj_name)
                        && TextUtils.equals(oldModel.user_email, newModel.user_email)
                        && TextUtils.equals(oldModel.user_name, newModel.user_name)
                        && TextUtils.equals(oldModel.user_contact_email, newModel.user_contact_email)
                        && oldModel.repo_encrypted == newModel.repo_encrypted
                        && oldModel.is_dir == newModel.is_dir;
            }
        });

        setItems(list);
        diffResult.dispatchUpdatesTo(this);
    }
}