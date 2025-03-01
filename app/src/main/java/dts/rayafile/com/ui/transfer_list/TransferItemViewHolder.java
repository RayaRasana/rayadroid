package dts.rayafile.com.ui.transfer_list;

import androidx.annotation.NonNull;

import dts.rayafile.com.databinding.ItemTransferListBinding;
import dts.rayafile.com.ui.base.viewholder.BaseViewHolder;

public class TransferItemViewHolder extends BaseViewHolder {
    public ItemTransferListBinding binding;

    public TransferItemViewHolder(@NonNull ItemTransferListBinding binding) {
        super(binding.getRoot());

        this.binding = binding;
    }
}
