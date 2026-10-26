package com.cosyhome.tv;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ScrollView;

/** A ScrollView that scrolls a focused tile fully into view with room left for its focus zoom. */
public class RoomyScrollView extends ScrollView {

    private final int room;

    public RoomyScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        room = Math.round(12 * getResources().getDisplayMetrics().density);
    }

    @Override
    protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
        Rect padded = new Rect(rect);
        padded.inset(0, -room);
        return super.computeScrollDeltaToGetChildRectOnScreen(padded);
    }
}
