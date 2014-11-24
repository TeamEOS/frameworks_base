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
 * NX indicator that soon will be more than eye candy
 *
 */

package com.android.systemui.nx;

import com.android.systemui.statusbar.policy.KeyButtonView;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class NxLogoView extends KeyButtonView {
    public static final String TAG = NxLogoView.class.getSimpleName();

	public NxLogoView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public NxLogoView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setBackground(null);
	}

	@Override
    public boolean onTouchEvent(MotionEvent ev) {
	    // TEMP: pass all events to NX, for now
        return false;
    }
}
