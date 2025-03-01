package dts.rayafile.com.ui.repo.vh;

import androidx.annotation.NonNull;

import dts.rayafile.com.databinding.ItemDirentGalleryBinding;
import dts.rayafile.com.ui.base.viewholder.BaseViewHolder;

public class DirentGalleryViewHolder extends BaseViewHolder {
    public ItemDirentGalleryBinding binding;

    public DirentGalleryViewHolder(@NonNull ItemDirentGalleryBinding binding) {
        super(binding.getRoot());

        this.binding = binding;
    }
}
