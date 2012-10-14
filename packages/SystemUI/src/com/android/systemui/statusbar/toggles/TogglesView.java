package com.android.systemui.statusbar.toggles;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import com.android.systemui.R;
import com.android.systemui.statusbar.BaseStatusBar;

public class TogglesView extends LinearLayout {

    private static final String TAG = "TogglesView";

    private static final String TOGGLE_DELIMITER = "|";

    private static final String TOGGLE_AUTOROTATE = "ROTATE";
    private static final String TOGGLE_BLUETOOTH = "BLUETOOTH";
    private static final String TOGGLE_GPS = "GPS";
    private static final String TOGGLE_LTE = "LTE";
    private static final String TOGGLE_DATA = "DATA";
    private static final String TOGGLE_WIFI = "WIFI";
    private static final String TOGGLE_2G = "2G";
    private static final String TOGGLE_WIFI_AP = "AP";
    private static final String TOGGLE_AIRPLANE = "AIRPLANE_MODE";
    private static final String TOGGLE_VIBRATE = "VIBRATE";
    private static final String TOGGLE_SILENT = "SILENT";
    private static final String TOGGLE_TORCH = "TORCH";
    private static final String TOGGLE_SYNC = "SYNC";
    private static final String TOGGLE_TETHER = "TETHER";
    private static final String TOGGLE_NFC = "NFC";
    private static final String TOGGLE_DONOTDISTURB = "DONOTDISTURB";

    private static final String DEFAULT_TOGGLES = TOGGLE_WIFI + TOGGLE_DELIMITER
            + TOGGLE_BLUETOOTH + TOGGLE_DELIMITER + TOGGLE_GPS
            + TOGGLE_DELIMITER + TOGGLE_SYNC;

    protected static final int STYLE_NONE = 1;
    protected static final int STYLE_ICON = 2;
    protected static final int STYLE_TEXT = 3;
    protected static final int STYLE_ICON_TEXT = 4;

    public static final int LAYOUT_SWITCH = 0;
    public static final int LAYOUT_TOGGLE = 1;
    public static final int LAYOUT_BUTTON = 2;
    public static final int LAYOUT_MULTIROW = 3;

    private static final int WIDGETS_PER_ROW_UNLIMITED = 100; // 100 is big enough
    private static final int WIDGETS_PER_ROW_DEFAULT = 2;

    private static final LinearLayout.LayoutParams PARAMS_BRIGHTNESS = new LinearLayout.LayoutParams(
            LayoutParams.MATCH_PARENT, 90);

    private static final LinearLayout.LayoutParams PARAMS_TOGGLE = new LinearLayout.LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1f);

    private static final LinearLayout.LayoutParams PARAMS_TOGGLE_SCROLL = new LinearLayout.LayoutParams(
            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f);

    private ArrayList<LinearLayout> rows = new ArrayList<LinearLayout>();
    private ArrayList<Toggle> toggles = new ArrayList<Toggle>();

    private int mWidgetsPerRow;

    private boolean mShowBrightness;

    private int mToggleStyle = STYLE_TEXT;

    private boolean mUseChainedLayout;

    private BaseStatusBar sb;

    View mBrightnessSlider;

    LinearLayout mToggleSpacer;

    public TogglesView(Context context) {
        this(context, null);
    }

    public TogglesView(Context context, AttributeSet attrs) {
        super(context, attrs);
        new SettingsObserver(new Handler()).observe();
    }

    private void addToggles(String userToggles) {
        String[] split = userToggles.split("\\" + TOGGLE_DELIMITER);
        toggles.clear();

        for (String splitToggle : split) {
            Toggle newToggle = null;

            if (splitToggle.equals(TOGGLE_AUTOROTATE)) {
                newToggle = new AutoRotateToggle(mContext);
            } else if (splitToggle.equals(TOGGLE_BLUETOOTH)) {
                newToggle = new BluetoothToggle(mContext);
            } else if (splitToggle.equals(TOGGLE_DATA)) {
                newToggle = new NetworkToggle(mContext);
            } else if (splitToggle.equals(TOGGLE_GPS)) {
                newToggle = new GpsToggle(mContext);
            } else if (splitToggle.equals(TOGGLE_LTE)) {
                newToggle = new LteToggle(mContext);
            } else if (splitToggle.equals(TOGGLE_WIFI)) {
                newToggle = new WifiToggle(mContext);
            } else if (splitToggle.equals(TOGGLE_2G)) {
                newToggle = new TwoGToggle(mContext);
            } else if (splitToggle.equals(TOGGLE_WIFI_AP)) {
                newToggle = new WifiAPToggle(mContext);
            } else if (splitToggle.equals(TOGGLE_AIRPLANE)) {
                newToggle = new AirplaneModeToggle(mContext);
            } else if (splitToggle.equals(TOGGLE_VIBRATE)) {
                newToggle = new VibrateToggle(mContext);
            } else if (splitToggle.equals(TOGGLE_SILENT)) {
                newToggle = new SilentToggle(mContext);
            } else if (splitToggle.equals(TOGGLE_TORCH)) {
                newToggle = new TorchToggle(mContext);
            } else if (splitToggle.equals(TOGGLE_SYNC)) {
                newToggle = new SyncToggle(mContext);
            } else if (splitToggle.equals(TOGGLE_TETHER)) {
                newToggle = new USBTetherToggle(mContext);
            } else if (splitToggle.equals(TOGGLE_NFC)) {
                newToggle = new NFCToggle(mContext);
            } else if (splitToggle.equals(TOGGLE_DONOTDISTURB)) {
                newToggle = new DoNotDisturbToggle(mContext);
            }

            if (newToggle != null) {
                toggles.add(newToggle);
            }
        }

    }

    private void addBrightness() {
        rows.add(new LinearLayout(mContext));
        rows.get(rows.size() - 1).addView(
                new BrightnessSlider(mContext).getView(), PARAMS_BRIGHTNESS);
    }

    private void addViews() {
        removeViews();
        boolean disableScroll = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUSBAR_TOGGLES_DISABLE_SCROLL,
                0) == 1;

        for (int i = 0; i < toggles.size(); i++) {
            if (i % mWidgetsPerRow == 0) {
                // new row
                rows.add(new LinearLayout(mContext));
            }

            rows.get(rows.size() - 1).addView(toggles.get(i).getView(),
                    (!mUseChainedLayout || disableScroll ? PARAMS_TOGGLE : PARAMS_TOGGLE_SCROLL));
        }

        if (!mUseChainedLayout && (toggles.size() % 2 != 0)) {
            // We are using switches, and have an uneven number - let's add a
            // spacer
            mToggleSpacer = new LinearLayout(mContext);
            rows.get(rows.size() - 1).addView(mToggleSpacer, PARAMS_TOGGLE);
        }

        if (mUseChainedLayout && disableScroll == false) {
            LinearLayout togglesRowLayout;
            HorizontalScrollView toggleScrollView = new HorizontalScrollView(
                    mContext);
            try {
                togglesRowLayout = rows.get(rows.size() - 1);
            } catch (ArrayIndexOutOfBoundsException e) {
                // Happens when brightness bar is below buttons
                togglesRowLayout = new LinearLayout(mContext);
                rows.add(togglesRowLayout);
            }

            togglesRowLayout.setGravity(Gravity.LEFT);
            toggleScrollView.setHorizontalFadingEdgeEnabled(true);
            toggleScrollView.setHorizontalScrollBarEnabled(false);
            toggleScrollView.addView(togglesRowLayout, PARAMS_TOGGLE);
            LinearLayout ll = new LinearLayout(mContext);
            ll.setOrientation(LinearLayout.VERTICAL);
            ll.setGravity(Gravity.LEFT);
            ll.addView(toggleScrollView, PARAMS_TOGGLE_SCROLL);
            rows.remove(rows.size() - 1);
            rows.add(ll);
        }

        if (mShowBrightness){
            addBrightness();
        }

        if (sb != null && !sb.isTablet()){
            addSeparator();
        }

        for (LinearLayout row : rows) {
            this.addView(row);
        }
    }

    private void removeViews() {
        for (LinearLayout row : rows) {
            row.removeAllViews();
            this.removeView(row);
        }

        rows.clear();
    }

    private void addSeparator() {
        View sep = new View(mContext);
        sep.setBackgroundResource(R.drawable.status_bar_hr);

        DisplayMetrics metrics = getContext().getResources()
                .getDisplayMetrics();
        float dp = 2f;
        int pixels = (int) (metrics.density * dp + 0.5f);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, pixels);

        sep.setLayoutParams(params);

        rows.add(new LinearLayout(mContext));
        rows.get(rows.size() - 1).addView(sep);
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUSBAR_TOGGLES_ENABLE), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUSBAR_TOGGLES), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUSBAR_TOGGLES_STYLE), false,
                    this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUSBAR_TOGGLES_DISABLE_SCROLL), false,
                    this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUSBAR_TOGGLES_USE_BUTTONS),false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUSBAR_TOGGLES_SHOW_BRIGHTNESS),
                    false, this);
            updateSettings();
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    protected void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();

        mShowBrightness = Settings.System.getInt(resolver,
                Settings.System.STATUSBAR_TOGGLES_SHOW_BRIGHTNESS,
                0) == 1;

        String selectedToggles = Settings.System.getString(resolver,
                Settings.System.STATUSBAR_TOGGLES);

        boolean enableToggles = Settings.System.getInt(resolver,
                Settings.System.STATUSBAR_TOGGLES_ENABLE,
                0) == 1;

        // So you don't like toggles?, bad for you!
        if(!enableToggles) {
            toggles.clear();
        } else {
            addToggles(selectedToggles != null ? selectedToggles :
                    DEFAULT_TOGGLES);
        }

        mToggleStyle = Settings.System.getInt(resolver,
                Settings.System.STATUSBAR_TOGGLES_STYLE, STYLE_ICON);

        int layout = Settings.System.getInt(
                mContext.getContentResolver(),
                Settings.System.STATUSBAR_TOGGLES_USE_BUTTONS, LAYOUT_TOGGLE);

        if (layout == LAYOUT_BUTTON && mToggleStyle != STYLE_ICON) {
            mToggleStyle = STYLE_ICON;
        }
        else if (layout == LAYOUT_MULTIROW) {
            mToggleStyle = STYLE_ICON_TEXT;
        }

        mUseChainedLayout = (layout == LAYOUT_TOGGLE || layout == LAYOUT_BUTTON);

        mWidgetsPerRow = !mUseChainedLayout ? WIDGETS_PER_ROW_DEFAULT :
                WIDGETS_PER_ROW_UNLIMITED;

        boolean addText = false;
        boolean addIcon = false;

        switch (mToggleStyle) {
            case STYLE_NONE:
                break;
            case STYLE_ICON:
                addIcon = true;
                break;
            case STYLE_TEXT:
                addText = true;
                break;
            case STYLE_ICON_TEXT:
                addIcon = true;
                addText = true;
                mWidgetsPerRow = 1;
                break;
        }

        for (Toggle t : toggles) {
            t.setupInfo(addIcon, addText);
        }

        addViews();
    }

    public void onStatusbarExpanded() {
        for (Toggle t : toggles) {
            t.onStatusbarExpanded();
        }
    }

    public void setBar(BaseStatusBar statusBar) {
        sb = statusBar;

        SettingsObserver settingsObserver = new SettingsObserver(new Handler());
        settingsObserver.observe();
    }
}
