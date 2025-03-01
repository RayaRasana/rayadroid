package dts.rayafile.com.ui.base.adapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import dts.rayafile.com.databinding.ViewLoadMoreBinding;

public class LoadMoreViewHolder extends RecyclerView.ViewHolder {
    public ViewLoadMoreBinding viewBinding;

    public LoadMoreViewHolder(@NonNull ViewLoadMoreBinding binding) {
        super(binding.getRoot());
        viewBinding = binding;
    }
}
