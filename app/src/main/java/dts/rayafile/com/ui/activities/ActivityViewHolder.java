package dts.rayafile.com.ui.activities;

import androidx.annotation.NonNull;

import dts.rayafile.com.ui.base.viewholder.BaseViewHolder;
import dts.rayafile.com.databinding.ItemActivityBinding;

public class ActivityViewHolder extends BaseViewHolder {
    public ItemActivityBinding binding;

    public ActivityViewHolder(@NonNull ItemActivityBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }
}
