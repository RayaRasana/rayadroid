package dts.rayafile.com.ui.repo.vh;

import androidx.annotation.NonNull;

import dts.rayafile.com.ui.base.viewholder.BaseViewHolder;
import dts.rayafile.com.databinding.ItemAccountBinding;

public class AccountViewHolder extends BaseViewHolder {
    public ItemAccountBinding binding;

    public AccountViewHolder(@NonNull ItemAccountBinding binding) {
        super(binding.getRoot());

        this.binding = binding;
    }
}
