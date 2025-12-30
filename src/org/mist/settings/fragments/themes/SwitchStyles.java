/*
 * Copyright (C) 2024-2025 Mist OS
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
package org.mist.settings.fragments.themes;

import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.util.android.ThemeUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import java.util.List;

import org.mist.settings.preferences.SystemSettingListPreference;
import org.mist.settings.utils.SystemRestartUtils;

@SearchIndexable
public class SwitchStyles extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "SwitchStyles";
    private static final String KEY_SWITCH_STYLE = "switch_style";

    private static final String[] SWITCH_OVERLAYS = {
            "com.android.settingslib.smileswitch",
            "com.android.settingslib.md2switch", 
            "com.android.settingslib.oneplusswitch",
            "com.android.settingslib.telegramswitch"
    };

    private SystemSettingListPreference mSwitchStylePref;
    private ThemeUtils mThemeUtils;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.mist_settings_switch_styles);
        
        mThemeUtils = ThemeUtils.getInstance(getActivity());

        final Context context = getContext();
        final PreferenceScreen prefScreen = getPreferenceScreen();

        mSwitchStylePref = findPreference(KEY_SWITCH_STYLE);
        mSwitchStylePref.setOnPreferenceChangeListener(this);
        
        updateSwitchStyleSummary();
    }

    private void updateSwitchStyleSummary() {
        int currentStyle = Settings.System.getIntForUser(
                getContext().getContentResolver(),
                KEY_SWITCH_STYLE,
                0,
                UserHandle.USER_CURRENT
        );
        
        String[] entries = getResources().getStringArray(R.array.switch_style_entries);
        if (currentStyle >= 0 && currentStyle < entries.length) {
            mSwitchStylePref.setSummary(entries[currentStyle]);
        }
    }

    private void updateSwitchStyle(int style) {
        // Use ThemeUtils for proper overlay management
        mThemeUtils.setOverlayEnabled("android.theme.customization.switch_style", 
                "com.android.settingslib", "com.android.settingslib");
        
        if (style > 0 && style <= SWITCH_OVERLAYS.length) {
            mThemeUtils.setOverlayEnabled("android.theme.customization.switch_style", 
                    SWITCH_OVERLAYS[style - 1], "com.android.settingslib");
        }
        
        SystemRestartUtils.restartSystemUI(getContext());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mSwitchStylePref) {
            int value = Integer.parseInt((String) newValue);
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    KEY_SWITCH_STYLE, value, UserHandle.USER_CURRENT);
            updateSwitchStyle(value);
            updateSwitchStyleSummary();
            return true;
        }
        return false;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.MIST;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider(R.xml.mist_settings_switch_styles);
}
