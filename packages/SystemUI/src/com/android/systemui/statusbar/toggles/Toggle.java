/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.systemui.statusbar.toggles;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.statusbar.IStatusBarService;
import com.android.systemui.R;

/**
 * TODO: Listen for changes to the setting.
 */
public abstract class Toggle implements OnCheckedChangeListener {

    protected static final String TAG = "Toggle";

    private static final int VIBRATE_DURATION = 10; // 10 ms, not intrusive time

    View mView;
    protected Context mContext;

    // widgets
    protected ImageView mIcon;
    protected TextView mText;
    protected CompoundButton mToggle;

    protected Vibrator mVibrator;

    protected boolean mSystemChange = false;
    final int mLayout;
    final int defaultColor;
    final int defaultOffColor;

    public Toggle(Context context) {
        mContext = context;

        mLayout = Settings.System.getInt(
                context.getContentResolver(),
                Settings.System.STATUSBAR_TOGGLES_USE_BUTTONS, TogglesView.LAYOUT_TOGGLE);

        defaultColor = context.getResources().getColor(
            com.android.internal.R.color.holo_blue_light);

        float[] hsv = new float[3];
        Color.colorToHSV(defaultColor, hsv);
        hsv[2] *= 0.5f;
        defaultOffColor = Color.HSVToColor(hsv);

        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);

        switch (mLayout) {
            case TogglesView.LAYOUT_SWITCH:
                mView = View.inflate(mContext, R.layout.toggle_switch, null);
                break;
            case TogglesView.LAYOUT_TOGGLE:
            case TogglesView.LAYOUT_BUTTON:
                mView = View.inflate(mContext, R.layout.toggle_toggle, null);
                break;
            case TogglesView.LAYOUT_MULTIROW:
                mView = View.inflate(mContext, R.layout.toggle_multirow, null);
                break;
        }

        mIcon = (ImageView) mView.findViewById(R.id.icon);
        mToggle = (CompoundButton) mView.findViewById(R.id.toggle);
        mText = (TextView) mView.findViewById(R.id.label);

        mToggle.setOnCheckedChangeListener(this);
        mToggle.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (onLongPress()) {
                    collapseStatusBar();
                    return true;
                } else {
                    return false;
                }
            }
        });
    }

    public void updateDrawable(boolean toggle) {
        Drawable bg = null;
        switch(mLayout){
            case TogglesView.LAYOUT_TOGGLE:
                bg = mContext.getResources().getDrawable(
                        R.drawable.btn_toggle_small);
                break;
            case TogglesView.LAYOUT_BUTTON:
                bg = mContext.getResources().getDrawable(
                        R.drawable.btn_toggle_fit);
                break;
            default:
                return;
        }

        if (toggle) {
            bg.setColorFilter(defaultColor, PorterDuff.Mode.SRC_ATOP);
        } else {
            bg.setColorFilter(defaultOffColor, PorterDuff.Mode.SRC_ATOP);
        }
        mToggle.setBackgroundDrawable(bg);
    }

    /**
     * this method is called when we need to update the state of the toggle due
     * to outside interactions.
     *
     * @return returns the on/off state of the toggle
     */
    protected abstract boolean updateInternalToggleState();

    /**
     * this method is called when the user manually toggles, update states as
     * needed
     */
    protected abstract void onCheckChanged(boolean isChecked);

    /**
     * this method is called when the user longpresses the toggle
     */
    protected abstract boolean onLongPress();

    public void updateState() {
        mSystemChange = true;
        updateDrawable(updateInternalToggleState());
        mSystemChange = false;
    }

    public void collapseStatusBar() {
        try {
            IStatusBarService sb = IStatusBarService.Stub
                    .asInterface(ServiceManager
                            .getService(Context.STATUS_BAR_SERVICE));
            sb.collapse();
        } catch (RemoteException e) {
        }
    }

    @Override
    public final void onCheckedChanged(CompoundButton buttonView,
            boolean isChecked) {
        if (mSystemChange) return;

        mView.playSoundEffect(SoundEffectConstants.CLICK);
        mVibrator.vibrate(VIBRATE_DURATION);

        onCheckChanged(isChecked);
    }

    public View getView() {
        return mView;
    }

    public void setLabel(String s) {
        if (mText != null)
            mText.setText(s);
    }

    public void setLabel(int res) {
        if (mText != null)
            mText.setText(res);
    }

    public void setIcon(int res) {
        if (mIcon != null) {
            mIcon.setImageResource(res);
        }
    }

    public void setupInfo(boolean showIcon, boolean showText) {
        mIcon.setVisibility(showIcon ? View.VISIBLE : View.GONE);
        mText.setVisibility(showText ? View.VISIBLE : View.GONE);
    }

    protected void onStatusbarExpanded() {
    }
}
