//package dts.rayafile.com.ui.file;
//
//import android.content.Context;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ImageView;
//import android.widget.ProgressBar;
//import android.widget.TextView;
//
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.google.android.material.button.MaterialButton;
//
//import java.util.List;
//
//import dts.rayafile.com.R;
//import dts.rayafile.com.framework.data.db.entities.DirentModel;
//import dts.rayafile.com.framework.util.Icons;
//
//public class MultiFileAdapter extends RecyclerView.Adapter<MultiFileAdapter.FileDownloadViewHolder> {
//
//    private List<DirentModel> direntModels;
//    private Context context;
//
//    public MultiFileAdapter(Context context, List<DirentModel> direntModels) {
//        this.context = context;
//        this.direntModels = direntModels;
//    }
//
//    @Override
//    public FileDownloadViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(context).inflate(R.layout.item_file_download, parent, false);
//        return new FileDownloadViewHolder(view);
//    }
//
//    @Override
//    public void onBindViewHolder(FileDownloadViewHolder holder, int position) {
//        DirentModel direntModel = direntModels.get(position);
//
//        holder.fileName.setText(direntModel.name);
//        holder.fileIcon.setImageResource(Icons.getFileIcon(direntModel.name));
//
//        // Handle progress bar and cancel button visibility here
//        holder.cancelButton.setVisibility(View.VISIBLE); // Show cancel if necessary
//        holder.progressBar.setProgress(0); // Reset progress before downloading
//
//        // Handle download progress updates
//        // For example, you can bind the download progress here if you have a listener or ViewModel
//
//        // You could also add logic to cancel the download on button click
//        holder.cancelButton.setOnClickListener(view -> {
//            // Add logic to cancel the download
//        });
//    }
//
//    @Override
//    public int getItemCount() {
//        return direntModels.size();
//    }
//    public void updateFileProgress(int position, int progress, long transferredSize, long totalSize) {
//        if (position >= 0 && position < direntModels.size()) {
//            DirentModel direntModel = direntModels.get(position);
//            direntModel.setProgress(progress);  // Set progress in your model
//
//            // Find the corresponding view holder and update the progress bar
//            notifyItemChanged(position);
//        }
//    }
//
//    public void updateFileProgressText(int position, String progressText) {
//        if (position >= 0 && position < direntModels.size()) {
//            DirentModel direntModel = direntModels.get(position);
//            direntModel.pro(progressText);
//
//            // Find the corresponding view holder and update the progress text
//            notifyItemChanged(position);
//        }
//    }
//
//    public void updateFileStatus(int position, String status, String filePath) {
//        if (position >= 0 && position < direntModels.size()) {
//            DirentModel direntModel = direntModels.get(position);
//            direntModel.setDownloadStatus(status);
//            direntModel.setDownloadedFilePath(filePath);
//
//            // Find the corresponding view holder and update the status
//            notifyItemChanged(position);
//        }
//    }
//    public static class FileDownloadViewHolder extends RecyclerView.ViewHolder {
//        ImageView fileIcon;
//        TextView fileName;
//        TextView progressText;
//        ProgressBar progressBar;
//        MaterialButton cancelButton;
//
//        public FileDownloadViewHolder(View itemView) {
//            super(itemView);
//            fileIcon = itemView.findViewById(R.id.file_icon);
//            fileName = itemView.findViewById(R.id.file_name);
//            progressText = itemView.findViewById(R.id.progress_text);
//            progressBar = itemView.findViewById(R.id.progress_bar);
//            cancelButton = itemView.findViewById(R.id.op_cancel);
//        }
//    }
//}