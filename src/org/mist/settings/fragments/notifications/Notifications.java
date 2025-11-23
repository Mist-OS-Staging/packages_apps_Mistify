/*
 * Copyright (C) 2018-2022 crDroid Android Project
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

package org.mist.settings.fragments.notifications;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;

import com.android.internal.util.android.VibrationUtils;

import org.mist.settings.preferences.SystemSettingSwitchPreference;

import java.util.List;

@SearchIndexable
public class Notifications extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "Notifications";

    private static final String KEY_ALERT_SLIDER_PREF = "alert_slider_notifications";
    private static final String KEY_INTERFACE_CATEGORY = "notifications_interface_category";
    private static final String KEY_SPLIT_NOTIFICATION_PANEL = "split_notification_panel";

    private PreferenceCategory mInterfaceCategory;
    private Preference mAlertSlider;
    private SystemSettingSwitchPreference mSplitNotificationPanel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.mist_settings_notifications);

        final Context mContext = getActivity().getApplicationContext();
        final ContentResolver resolver = mContext.getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();
        final Resources res = mContext.getResources();

        mAlertSlider = (SystemSettingSwitchPreference) findPreference(KEY_ALERT_SLIDER_PREF);
        mInterfaceCategory = (PreferenceCategory) findPreference(KEY_INTERFACE_CATEGORY);
        mSplitNotificationPanel = (SystemSettingSwitchPreference) findPreference(KEY_SPLIT_NOTIFICATION_PANEL);
        
        boolean mAlertSliderAvailable = res.getBoolean(
                com.android.internal.R.bool.config_hasAlertSlider);
        if (!mAlertSliderAvailable) {
            mInterfaceCategory.removePreference(mAlertSlider);
        }
        
        // Set up split notification panel preference
        if (mSplitNotificationPanel != null) {
            mSplitNotificationPanel.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final Context context = getContext();
        final ContentResolver resolver = context.getContentResolver();
        
        if (preference == mSplitNotificationPanel) {
            boolean enabled = (Boolean) newValue;
            // Show restart dialog when enabling/disabling split notification panel
            showRestartDialog();
            return true;
        }
        
        return false;
    }
    
    private void showRestartDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
        builder.setTitle("Restart Required");
        builder.setMessage("System restart is required to apply split notification panel changes. Restart now?");
        builder.setPositiveButton("Restart", (dialog, which) -> {
            android.os.PowerManager pm = (android.os.PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
            pm.reboot("Split notification panel setting changed");
        });
        builder.setNegativeButton("Later", null);
        builder.show();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.MIST;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference != null && preference.getKey() != null) {
            VibrationUtils.triggerVibration(getContext(), 3);
        }
        return super.onPreferenceTreeClick(preference);
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.mist_settings_notifications) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    final Resources res = context.getResources();

                    boolean mAlertSliderAvailable = res.getBoolean(
                            com.android.internal.R.bool.config_hasAlertSlider);
                    if (!mAlertSliderAvailable)
                        keys.add(KEY_ALERT_SLIDER_PREF);

                    return keys;
                }
            };
}
