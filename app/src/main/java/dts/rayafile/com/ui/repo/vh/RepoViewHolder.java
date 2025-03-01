package dts.rayafile.com.ui.repo.vh;

import androidx.annotation.NonNull;

import dts.rayafile.com.ui.base.viewholder.BaseViewHolder;
import dts.rayafile.com.databinding.ItemRepoBinding;

public class RepoViewHolder extends BaseViewHolder {
    public ItemRepoBinding binding;

    public RepoViewHolder(@NonNull ItemRepoBinding binding) {
        super(binding.getRoot());

        this.binding = binding;
    }
}
