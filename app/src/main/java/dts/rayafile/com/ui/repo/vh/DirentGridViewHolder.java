package dts.rayafile.com.ui.repo.vh;

import androidx.annotation.NonNull;

import dts.rayafile.com.databinding.ItemDirentGridBinding;
import dts.rayafile.com.ui.base.viewholder.BaseViewHolder;

public class DirentGridViewHolder extends BaseViewHolder {
    public ItemDirentGridBinding binding;

    public DirentGridViewHolder(@NonNull ItemDirentGridBinding binding) {
        super(binding.getRoot());

        this.binding = binding;
    }
}
