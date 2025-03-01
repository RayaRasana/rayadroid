package dts.rayafile.com.ui.sdoc.outline;

import static dts.rayafile.com.config.Constants.DP.DP_8;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dts.rayafile.com.databinding.ItemSdocOutlineBinding;
import dts.rayafile.com.framework.data.model.sdoc.OutlineItemModel;
import dts.rayafile.com.ui.base.adapter.BaseAdapter;
import dts.rayafile.com.ui.base.viewholder.BaseViewHolder;

public class SDocOutlineAdapter extends BaseAdapter<OutlineItemModel, SDocOutlineAdapter.SDocOutlineHolder> {

    private final int _paddingStart = DP_8;

    @NonNull
    @Override
    protected SDocOutlineHolder onCreateViewHolder(@NonNull Context context, @NonNull ViewGroup viewGroup, int i) {
        ItemSdocOutlineBinding binding = ItemSdocOutlineBinding.inflate(LayoutInflater.from(context), viewGroup, false);
        return new SDocOutlineHolder(binding);
    }

    @Override
    protected void onBindViewHolder(@NonNull SDocOutlineHolder holder, int i, @Nullable OutlineItemModel outlineItemModel) {
        if (outlineItemModel == null) {
            return;
        }

        int padding = 0;
        if ("header1".equals(outlineItemModel.type)) {
            padding = _paddingStart;
        } else if ("header2".equals(outlineItemModel.type)) {
            padding = _paddingStart * 3;
        } else if ("header3".equals(outlineItemModel.type)) {
            padding = _paddingStart * 6;
        }

        holder.binding.title.setPadding(padding, 0, 0, 0);
        holder.binding.title.setText(outlineItemModel.text);
    }


    public static class SDocOutlineHolder extends BaseViewHolder {
        ItemSdocOutlineBinding binding;

        public SDocOutlineHolder(@NonNull ItemSdocOutlineBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
