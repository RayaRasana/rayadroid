package dts.rayafile.com.ui.search;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import dts.rayafile.com.databinding.ItemSearchBinding;
import dts.rayafile.com.framework.data.model.search.SearchModel;
import dts.rayafile.com.ui.base.adapter.BaseAdapter;
import dts.rayafile.com.ui.base.viewholder.BaseViewHolder;

import org.jetbrains.annotations.NotNull;

public class SearchRecyclerViewAdapter extends BaseAdapter<SearchModel, SearchRecyclerViewAdapter.SearchItemViewHolder> {
    @Override
    protected void onBindViewHolder(@NotNull SearchItemViewHolder holder, int i, @Nullable SearchModel model) {
        if (model == null) {
            return;
        }

        holder.binding.icon.setImageResource(model.getIcon());
        holder.binding.title.setText(model.getTitle());
        holder.binding.subtitle.setText(model.getSubtitle());
    }

    @NotNull
    @Override
    protected SearchItemViewHolder onCreateViewHolder(@NotNull Context context, @NotNull ViewGroup viewGroup, int i) {
        ItemSearchBinding binding = ItemSearchBinding.inflate(LayoutInflater.from(context), viewGroup, false);
        return new SearchItemViewHolder(binding);
    }

    public static class SearchItemViewHolder extends BaseViewHolder {
        public ItemSearchBinding binding;

        public SearchItemViewHolder(ItemSearchBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
