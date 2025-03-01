package dts.rayafile.com.ui.viewholder;

import androidx.annotation.NonNull;

import dts.rayafile.com.ui.base.viewholder.BaseViewHolder;
import dts.rayafile.com.databinding.ItemGroupItemBinding;

public class GroupItemViewHolder extends BaseViewHolder {
    public ItemGroupItemBinding binding;

    public GroupItemViewHolder(@NonNull ItemGroupItemBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }
}
