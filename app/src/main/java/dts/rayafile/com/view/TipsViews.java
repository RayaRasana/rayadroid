package dts.rayafile.com.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import dts.rayafile.com.R;

public class TipsViews {
    public static TextView getTipTextView(Context context) {
        return (TextView) LayoutInflater.from(context).inflate(R.layout.view_tip_textview, null);
    }
}
