/*
 * Copyright (C) 2014 The TeamEos Project
 * Author: Randall Rushing aka Bigrushdog
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
 * 
 * Mandated public methods for all navigation implementations. This allows
 * all status bar managers to be happy, including TabletStatusBar, which may
 * come back some day ;D
 * 
 */

package com.android.systemui.statusbar.phone;

import java.io.FileDescriptor;
import java.io.PrintWriter;

import com.android.systemui.statusbar.BaseStatusBar;

import android.view.View;

public interface Navigator {
    public BarTransitions getBarTransitions();
    public void setDelegateView(View view);
    public void setBar(BaseStatusBar phoneStatusBar);
    public void setNavigatorListeners(View.OnClickListener recentsClickListener,
            View.OnTouchListener recentsPreloadOnTouchListener,
            View.OnTouchListener homeSearchActionListener,
            View.OnTouchListener userAutoHideListener);
    public void updateResources();
    public void notifyScreenOn(boolean screenOn);
    public void setNavigationIconHints(int hints);
    public void setDisabledFlags(int disabledFlags);
    public void setSlippery(boolean newSlippery);
    public void setMenuVisibility(final boolean show);
    public void reorient();
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args);
    public View getViewForWindowManager();
}
