package dts.rayafile.com.ui.repo.vh;

import androidx.annotation.NonNull;

import dts.rayafile.com.databinding.ItemDirentBinding;
import dts.rayafile.com.ui.base.viewholder.BaseViewHolder;

public class DirentViewHolder extends BaseViewHolder {
    public ItemDirentBinding binding;

    public DirentViewHolder(@NonNull ItemDirentBinding binding) {
        super(binding.getRoot());

        this.binding = binding;
    }
}
