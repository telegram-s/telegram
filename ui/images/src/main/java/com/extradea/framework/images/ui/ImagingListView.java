/*
 * Copyright (c) 2013 Extradea LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.extradea.framework.images.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import com.extradea.framework.images.ImageSupport;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 26.06.12
 * Time: 0:46
 */
public class ImagingListView extends ListView {

    ImageSupport imageSupport;

    private int minGap;
    private int maxGap;
    private boolean enabledDynamicPause = false;

    public ImagingListView(Context context) {
        super(context);
        init();
    }

    public ImagingListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ImagingListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    protected void onImageResume() {
        imageSupport.getImageController().doResume();
    }

    protected void onImagePause() {
        imageSupport.getImageController().doPause();
    }

    public boolean isEnabledDynamicPause() {
        return enabledDynamicPause;
    }

    public void setEnabledDynamicPause(boolean enabledDynamicPause) {
        this.enabledDynamicPause = enabledDynamicPause;
    }

    public int getMinGap() {
        return minGap;
    }

    public void setMinGap(int minGap) {
        this.minGap = minGap;
    }

    public int getMaxGap() {
        return maxGap;
    }

    public void setMaxGap(int maxGap) {
        this.maxGap = maxGap;
    }

    private void init() {

        maxGap = (int) (getResources().getDisplayMetrics().density * 3000);
        minGap = (int) (getResources().getDisplayMetrics().density * 1000);

        if (getContext() instanceof ImageSupport) {
            imageSupport = (ImageSupport) getContext();
        } else if (getContext().getApplicationContext() instanceof ImageSupport) {
            imageSupport = (ImageSupport) getContext().getApplicationContext();
        }

        if (imageSupport != null) {
            setOnScrollListener(new OnScrollListener() {

                boolean isScrolling = false;

                private long lastOffsetTime;
                private HashMap<Long, Integer> offsets = new HashMap<Long, Integer>();

                private int buildDelta() {
                    for (int i = 0; i < getChildCount(); i++) {
                        long id = getItemIdAtPosition(
                                getFirstVisiblePosition() + i);
                        View view = getChildAt(i);
                        int offsetTop = view.getTop();
                        if (offsets.containsKey(id)) {
                            return offsets.get(id) - offsetTop;
                        }
                    }
                    return -1;
                }

                private void saveStates() {
                    offsets.clear();
                    for (int i = 0; i < getChildCount(); i++) {
                        long id = getItemIdAtPosition(
                                getFirstVisiblePosition() + i);
                        View view = getChildAt(i);
                        int offsetTop = view.getTop();
                        offsets.put(id, offsetTop);
                    }
                }

                private void check() {
                    if ((SystemClock.uptimeMillis() - lastOffsetTime > 100)) {
                        if (lastOffsetTime != 0) {
                            int delta = buildDelta();
                            long timeDelta = SystemClock.uptimeMillis() - lastOffsetTime;
                            double speed = Math.abs(1000 * (delta / (double) timeDelta));
                            if (speed > maxGap) {
                                onImagePause();
                                Log.d("STELS_UI", "Speed: " + speed);
                            } else {
                                if (speed < minGap && speed > 0) {
                                    onImageResume();
                                }
                            }
                        }
                        lastOffsetTime = SystemClock.uptimeMillis();
                        saveStates();
                    }
                }

                @Override
                public void onScrollStateChanged(AbsListView absListView, int i) {
                    if (i == SCROLL_STATE_IDLE) {
                        onImageResume();
                        if (enabledDynamicPause) {
                            check();
                        }
                        isScrolling = false;
                    } else {
                        if (enabledDynamicPause) {
                            check();
                        } else {
                            onImagePause();
                        }
                        isScrolling = true;
                    }
                }

                @Override
                public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    if (enabledDynamicPause) {
                        check();
                    } else {
                        if (isScrolling) {
                            onImagePause();
                        }
                    }
                }
            });
        }
    }
}
