package dts.rayafile.com.ui.repo.vh;

import androidx.annotation.NonNull;

import dts.rayafile.com.ui.base.viewholder.BaseViewHolder;
import dts.rayafile.com.databinding.ItemUnsupportedBinding;

public class UnsupportedViewHolder extends BaseViewHolder {
    public ItemUnsupportedBinding binding;

    public UnsupportedViewHolder(@NonNull ItemUnsupportedBinding binding) {
        super(binding.getRoot());

        this.binding = binding;
    }
}
