/*
 * Copyright (C) 2022 crDroid Android Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.mist.settings.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.SystemProperties;
import android.util.AttributeSet;
import android.util.Log;

import com.android.settingslib.development.SystemPropPoker;

import lineageos.preference.SelfRemovingSwitchPreference;

import com.android.settings.R;

/**
 * Merged & fixed SystemPropertySwitchPreference
 *
 * - Preserves original behaviour (uses SystemProperties and SystemPropPoker)
 * - Adds support for app:propKey (preferred) and app:propType (enum)
 * - Wraps writes in try/catch to prevent RuntimeException crashes
 * - Falls back to preference key if propKey is missing (preserves old behaviour)
 */
public class SystemPropertySwitchPreference extends SelfRemovingSwitchPreference {

    private static final String TAG = "SysPropSwitchPref";

    // property key read from XML (app:propKey). If null/empty, fallback to getKey()
    private String mPropKey = null;

    // optional type mapping (not used for boolean path but kept for parity)
    private String mPropType = "boolean";

    // optional default value read from XML (not strictly necessary here)
    private String mPropDefault = "false";

    public SystemPropertySwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initAttrs(attrs);
    }

    public SystemPropertySwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(attrs);
    }

    public SystemPropertySwitchPreference(Context context) {
        super(context);
        // no attrs available
    }

    /**
     * Read custom attrs (propKey / propType / propDefault) if provided.
     * Supports enum-based propType (type_boolean/type_string/type_int) declared in attrs.xml.
     */
    private void initAttrs(AttributeSet attrs) {
        if (attrs == null) return;

        TypedArray a = null;
        try {
            a = getContext().obtainStyledAttributes(attrs, R.styleable.SystemPropertySwitchPreference);

            String pk = a.getString(R.styleable.SystemPropertySwitchPreference_propKey);
            if (pk != null && !pk.isEmpty()) mPropKey = pk;

            // Read propType enum (if present) — map enum values to type string
            // If attrs.xml used enum names type_boolean/type_string/type_int with values 0/1/2
            int propTypeEnum = a.getInt(R.styleable.SystemPropertySwitchPreference_propType, -1);
            if (propTypeEnum >= 0) {
                switch (propTypeEnum) {
                    case 0: // type_boolean
                        mPropType = "boolean";
                        break;
                    case 1: // type_string
                        mPropType = "string";
                        break;
                    case 2: // type_int
                        mPropType = "int";
                        break;
                    default:
                        mPropType = "boolean";
                        break;
                }
            } else {
                // fallback to string if someone used string-based attr
                String t = a.getString(R.styleable.SystemPropertySwitchPreference_propType);
                if (t != null && !t.isEmpty()) mPropType = t;
            }

            String def = a.getString(R.styleable.SystemPropertySwitchPreference_propDefault);
            if (def != null) mPropDefault = def;
        } catch (Throwable t) {
            Log.w(TAG, "Failed to obtain attrs for SystemPropertySwitchPreference", t);
        } finally {
            if (a != null) a.recycle();
        }
    }

    /**
     * Helper to obtain the effective property key to use:
     * prefer mPropKey (from XML) else fallback to preference key (original behaviour).
     */
    private String effectivePropKey() {
        if (mPropKey != null && !mPropKey.isEmpty()) return mPropKey;
        // fallback to preference key (preserve old behavior)
        return getKey();
    }

    @Override
    protected boolean isPersisted() {
        String prop = effectivePropKey();
        String val = SystemProperties.get(prop, "");
        return !val.isEmpty();
    }

    @Override
    protected void putBoolean(String key, boolean value) {
        // Use effective prop key (ignore the key arg since SelfRemovingSwitchPreference passes pref key)
        String prop = effectivePropKey();
        try {
            SystemProperties.set(prop, Boolean.toString(value));
            // notify system property change - original behaviour preserved
            SystemPropPoker.getInstance().poke();
        } catch (Throwable t) {
            // log and avoid crashing Settings
            Log.w(TAG, "Failed to set system property " + prop + " to " + value, t);
            // No rethrow: we must not allow native_set exception to crash the process
        }
    }

    @Override
    protected boolean getBoolean(String key, boolean defaultValue) {
        String prop = effectivePropKey();
        try {
            return SystemProperties.getBoolean(prop, defaultValue);
        } catch (Throwable t) {
            Log.w(TAG, "Failed to read system property " + prop + " (getBoolean)", t);
            return defaultValue;
        }
    }

    /* --- If your original class had additional methods/fields beyond those above,
       paste them here. This merged version preserves original API behavior while
       preventing crashes due to missing propKey or permission issues. */
}
