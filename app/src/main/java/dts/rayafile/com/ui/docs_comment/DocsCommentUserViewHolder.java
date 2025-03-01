package dts.rayafile.com.ui.docs_comment;

import androidx.annotation.NonNull;

import dts.rayafile.com.annotation.Todo;
import dts.rayafile.com.databinding.ItemUserAvatarBinding;
import dts.rayafile.com.ui.base.viewholder.BaseViewHolder;

@Todo
public class DocsCommentUserViewHolder extends BaseViewHolder {

    public ItemUserAvatarBinding binding;

    public DocsCommentUserViewHolder(@NonNull ItemUserAvatarBinding binding) {
        super(binding.getRoot());

        this.binding = binding;
    }
}
