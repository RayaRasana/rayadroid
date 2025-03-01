package dts.rayafile.com.ui.repo;

public class ScrollState {
    public int index;
    public int top;

    public ScrollState(int index, int top) {
        this.index = index;
        this.top = top;
    }

    @Override
    public String toString() {
        return "ScrollState{" +
                "index=" + index +
                ", top=" + top +
                '}';
    }
}
