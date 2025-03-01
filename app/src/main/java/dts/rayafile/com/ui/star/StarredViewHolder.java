package dts.rayafile.com.ui.star;

import androidx.annotation.NonNull;

import dts.rayafile.com.ui.base.viewholder.BaseViewHolder;
import dts.rayafile.com.databinding.ItemStarredBinding;

public class StarredViewHolder extends BaseViewHolder {
    public ItemStarredBinding binding;

    public StarredViewHolder(@NonNull ItemStarredBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }
}
