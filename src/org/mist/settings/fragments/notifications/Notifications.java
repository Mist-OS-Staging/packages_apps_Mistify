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
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    private static final String TAG = "MistNotifications";

    private static final String KEY_ALERT_SLIDER_PREF = "alert_slider_notifications";
    private static final String KEY_INTERFACE_CATEGORY = "notifications_interface_category";
    private static final String KEY_SPLIT_NOTIFICATION_PANEL = "split_notification_panel";

    private PreferenceCategory mInterfaceCategory;
    private Preference mAlertSlider;
    private SystemSettingSwitchPreference mSplitNotificationPanel;
    
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private boolean mIsDestroyed = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            addPreferencesFromResource(R.xml.mist_settings_notifications);
            initializePreferences();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
        }
    }
    
    private void initializePreferences() {
        final Context context = getActivity();
        if (context == null) {
            Log.e(TAG, "Context is null, cannot initialize preferences");
            return;
        }
        
        final ContentResolver resolver = context.getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();
        final Resources res = context.getResources();

        try {
            // Initialize preferences
            mAlertSlider = findPreference(KEY_ALERT_SLIDER_PREF);
            mInterfaceCategory = (PreferenceCategory) findPreference(KEY_INTERFACE_CATEGORY);
            mSplitNotificationPanel = (SystemSettingSwitchPreference) findPreference(KEY_SPLIT_NOTIFICATION_PANEL);
            
            // Handle alert slider availability
            boolean alertSliderAvailable = res.getBoolean(
                    com.android.internal.R.bool.config_hasAlertSlider);
            if (!alertSliderAvailable && mInterfaceCategory != null && mAlertSlider != null) {
                mInterfaceCategory.removePreference(mAlertSlider);
            }
            
            // Setup split notification panel preference
            setupSplitNotificationPanel(context);
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing preferences", e);
        }
    }
    
    private void setupSplitNotificationPanel(@NonNull Context context) {
        if (mSplitNotificationPanel == null) {
            Log.w(TAG, "Split notification panel preference not found");
            return;
        }
        
        try {
            mSplitNotificationPanel.setOnPreferenceChangeListener(this);
            
            // Update summary based on current state
            updateSplitPanelSummary();
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up split notification panel", e);
        }
    }
    
    private void updateSplitPanelSummary() {
        if (mSplitNotificationPanel == null || isDestroyed()) return;
        
        try {
            boolean enabled = Settings.System.getInt(getContentResolver(), 
                    KEY_SPLIT_NOTIFICATION_PANEL, 0) == 1;
            
            String summary = enabled ? 
                    getString(R.string.split_notification_panel_summary_enabled) :
                    getString(R.string.split_notification_panel_summary);
                    
            mSplitNotificationPanel.setSummary(summary);
        } catch (Exception e) {
            Log.e(TAG, "Error updating split panel summary", e);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (isDestroyed()) return false;
        
        final Context context = getContext();
        if (context == null) {
            Log.e(TAG, "Context is null in onPreferenceChange");
            return false;
        }
        
        try {
            if (preference == mSplitNotificationPanel) {
                boolean enabled = (Boolean) newValue;
                handleSplitPanelChange(enabled);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onPreferenceChange", e);
        }
        
        return false;
    }
    
    private void handleSplitPanelChange(boolean enabled) {
        try {
            // Update summary immediately
            mMainHandler.post(() -> {
                if (!isDestroyed() && mSplitNotificationPanel != null) {
                    String summary = enabled ? 
                            getString(R.string.split_notification_panel_summary_enabled) :
                            getString(R.string.split_notification_panel_summary);
                    mSplitNotificationPanel.setSummary(summary);
                }
            });
            
            // Show restart dialog with delay to allow setting to be saved
            mMainHandler.postDelayed(() -> {
                if (!isDestroyed()) {
                    showRestartDialog();
                }
            }, 500);
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling split panel change", e);
        }
    }
    
    private void showRestartDialog() {
        final Context context = getContext();
        if (context == null || isDestroyed()) {
            Log.w(TAG, "Cannot show restart dialog - context null or destroyed");
            return;
        }
        
        try {
            androidx.appcompat.app.AlertDialog.Builder builder = 
                    new androidx.appcompat.app.AlertDialog.Builder(context);
            
            builder.setTitle(R.string.restart_required_title);
            builder.setMessage(R.string.split_notification_restart_message);
            builder.setCancelable(false);
            
            builder.setPositiveButton(R.string.restart_now, (dialog, which) -> {
                try {
                    android.os.PowerManager pm = (android.os.PowerManager) 
                            context.getSystemService(Context.POWER_SERVICE);
                    if (pm != null) {
                        pm.reboot("Split notification panel setting changed");
                    } else {
                        Log.e(TAG, "PowerManager is null, cannot restart");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error restarting system", e);
                }
            });
            
            builder.setNegativeButton(R.string.restart_later, (dialog, which) -> {
                dialog.dismiss();
            });
            
            androidx.appcompat.app.AlertDialog dialog = builder.create();
            dialog.show();
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing restart dialog", e);
        }
    }
    
    private boolean isDestroyed() {
        return mIsDestroyed || getActivity() == null || getActivity().isDestroyed();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.MIST;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (isDestroyed()) return false;
        
        try {
            if (preference != null && preference.getKey() != null) {
                VibrationUtils.triggerVibration(getContext(), 3);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onPreferenceTreeClick", e);
        }
        
        return super.onPreferenceTreeClick(preference);
    }
    
    @Override
    public void onDestroy() {
        mIsDestroyed = true;
        
        try {
            // Remove any pending callbacks
            mMainHandler.removeCallbacksAndMessages(null);
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy", e);
        }
        
        super.onDestroy();
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.mist_settings_notifications) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    
                    try {
                        final Resources res = context.getResources();

                        // Hide alert slider if not available
                        boolean alertSliderAvailable = res.getBoolean(
                                com.android.internal.R.bool.config_hasAlertSlider);
                        if (!alertSliderAvailable) {
                            keys.add(KEY_ALERT_SLIDER_PREF);
                        }
                        
                    } catch (Exception e) {
                        Log.e(TAG, "Error in getNonIndexableKeys", e);
                    }

                    return keys;
                }
            };
}
