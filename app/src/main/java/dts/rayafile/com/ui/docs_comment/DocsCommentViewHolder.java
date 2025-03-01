package dts.rayafile.com.ui.docs_comment;

import androidx.annotation.NonNull;

import dts.rayafile.com.databinding.ItemFileCommentBinding;
import dts.rayafile.com.ui.base.viewholder.BaseViewHolder;

public class DocsCommentViewHolder extends BaseViewHolder {

    public ItemFileCommentBinding binding;

    public DocsCommentViewHolder(@NonNull ItemFileCommentBinding binding) {
        super(binding.getRoot());

        this.binding = binding;
    }
}
