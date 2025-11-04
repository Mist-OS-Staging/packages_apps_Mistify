/*
 * Copyright (C) 2016-2025 crDroid Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.crdroid.settings.fragments;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.util.crdroid.ThemeUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import com.crdroid.settings.fragments.ui.DozeSettings;
import com.crdroid.settings.fragments.ui.EdgeLightSettings;
import com.crdroid.settings.fragments.ui.SmartPixels;
import com.crdroid.settings.fragments.ui.MonetSettings;

import com.crdroid.settings.preferences.SystemSettingListPreference;
import com.crdroid.settings.utils.SystemRestartUtils;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable
public class UserInterface extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    public static final String TAG = "UserInterface";

    private static final String KEY_FORCE_FULL_SCREEN = "display_cutout_force_fullscreen_settings";
    private static final String SMART_PIXELS = "smart_pixels";

    // === New overlay keys ===
    private static final String KEY_NOTIFICATION_OVERLAY = "notification_overlay_style";
    private static final String KEY_POWERMENU_OVERLAY = "powermenu_overlay_style";
    private static final String KEY_PROGRESSBAR_OVERLAY = "progressbar_overlay_style";

    private Preference mShowCutoutForce;
    private Preference mSmartPixels;

    // Overlay Preferences
    private ListPreference mNotifOverlayPref;
    private ListPreference mPowerMenuOverlayPref;
    private ListPreference mProgressOverlayPref;

    private ThemeUtils mThemeUtils;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.crdroid_settings_ui);

        Context mContext = getActivity().getApplicationContext();
        final PreferenceScreen prefScreen = getPreferenceScreen();

        final String displayCutout =
                mContext.getResources().getString(com.android.internal.R.string.config_mainBuiltInDisplayCutout);

        if (TextUtils.isEmpty(displayCutout)) {
            mShowCutoutForce = findPreference(KEY_FORCE_FULL_SCREEN);
            prefScreen.removePreference(mShowCutoutForce);
        }

        mSmartPixels = prefScreen.findPreference(SMART_PIXELS);
        boolean mSmartPixelsSupported = getResources().getBoolean(
                com.android.internal.R.bool.config_supportSmartPixels);
        if (!mSmartPixelsSupported)
            prefScreen.removePreference(mSmartPixels);

        // === Initialize overlay manager ===
        mThemeUtils = new ThemeUtils(mContext);

        // === Initialize overlay preferences ===
        mNotifOverlayPref = findPreference(KEY_NOTIFICATION_OVERLAY);
        mPowerMenuOverlayPref = findPreference(KEY_POWERMENU_OVERLAY);
        mProgressOverlayPref = findPreference(KEY_PROGRESSBAR_OVERLAY);

        initOverlayList(mNotifOverlayPref, "com.android.systemui.notifications");
        initOverlayList(mPowerMenuOverlayPref, "com.android.systemui.powermenu");
        initOverlayList(mProgressOverlayPref, "com.android.systemui.progressbar");
    }

    private void initOverlayList(ListPreference pref, String category) {
        if (pref == null) return;

        List<String> overlayPackages = mThemeUtils.getOverlayPackagesForCategory(category);
        List<CharSequence> entries = new ArrayList<>();
        List<CharSequence> values = new ArrayList<>();

        entries.add("Default");
        values.add("default");

        for (String pkg : overlayPackages) {
            try {
                CharSequence label = getContext().getPackageManager().getApplicationLabel(
                        getContext().getPackageManager().getApplicationInfo(pkg, 0));
                entries.add(label);
                values.add(pkg);
            } catch (Exception e) {
                Log.e(TAG, "Error loading overlay label for " + pkg, e);
            }
        }

        pref.setEntries(entries.toArray(new CharSequence[0]));
        pref.setEntryValues(values.toArray(new CharSequence[0]));
        pref.setValue("default");
        pref.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String pkg = (String) newValue;

        if (preference == mNotifOverlayPref) {
            applyOverlayChange("com.android.systemui.notifications", pkg);
            return true;
        } else if (preference == mPowerMenuOverlayPref) {
            applyOverlayChange("com.android.systemui.powermenu", pkg);
            return true;
        } else if (preference == mProgressOverlayPref) {
            applyOverlayChange("com.android.systemui.progressbar", pkg);
            return true;
        }

        return false;
    }

    private void applyOverlayChange(String category, String packageName) {
        mThemeUtils.setOverlayEnabled(category, "default".equals(packageName) ? null : packageName);
    }

    public static void reset(Context mContext) {
        ContentResolver resolver = mContext.getContentResolver();
        Settings.System.putIntForUser(resolver,
                Settings.System.CHARGING_ANIMATION, 1, UserHandle.USER_CURRENT);
        Settings.Secure.putIntForUser(resolver,
                Settings.Secure.PULSE_ON_NEW_TRACKS, 0, UserHandle.USER_CURRENT);
        Settings.Secure.putIntForUser(resolver,
                Settings.Secure.DOZE_ALWAYS_ON_WALLPAPER_ENABLED,
                mContext.getResources().getBoolean(
                        com.android.internal.R.bool.config_dozeSupportsAodWallpaper) ? 1 : 0,
                UserHandle.USER_CURRENT);

        DozeSettings.reset(mContext);
        EdgeLightSettings.reset(mContext);
        MonetSettings.reset(mContext);
        SmartPixels.reset(mContext);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.CRDROID_SETTINGS;
    }

    /**
     * For search
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.crdroid_settings_ui) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);

                    final String displayCutout =
                            context.getResources().getString(com.android.internal.R.string.config_mainBuiltInDisplayCutout);

                    if (TextUtils.isEmpty(displayCutout)) {
                        keys.add(KEY_FORCE_FULL_SCREEN);
                    }

                    boolean mSmartPixelsSupported = context.getResources().getBoolean(
                            com.android.internal.R.bool.config_supportSmartPixels);
                    if (!mSmartPixelsSupported)
                        keys.add(SMART_PIXELS);

                    // Exclude overlay keys if not defined in XML
                    keys.add(KEY_NOTIFICATION_OVERLAY);
                    keys.add(KEY_POWERMENU_OVERLAY);
                    keys.add(KEY_PROGRESSBAR_OVERLAY);

                    return keys;
                }
            };
}
