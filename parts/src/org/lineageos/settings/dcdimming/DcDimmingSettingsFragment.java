/*
 * Copyright (C) 2018 The LineageOS Project
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

package org.lineageos.settings.dcdimming;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;
import org.lineageos.settings.R;
import org.lineageos.settings.utils.FileUtils;

public class DcDimmingSettingsFragment extends SettingsBasePreferenceFragment implements
        OnPreferenceChangeListener {

    private SwitchPreferenceCompat mDcDimmingPreference;
    private static final String DC_DIMMING_KEY = "dc_dimming";
    private static final String DC_DISPLAY_NODE = "/sys/devices/platform/soc/soc:qcom,dsi-display-primary/dimlayer_exposure";
    private static final String HBM_KEY = "hbm";

    private final SharedPreferences.OnSharedPreferenceChangeListener mPrefsListener =
            (prefs, key) -> {
                if (DC_DIMMING_KEY.equals(key)) {
                    if (mDcDimmingPreference != null) {
                        mDcDimmingPreference.setChecked(prefs.getBoolean(DC_DIMMING_KEY, false));
                    }
                }
            };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.dcdimming_settings, rootKey);
        mDcDimmingPreference = findPreference(DC_DIMMING_KEY);
        if (mDcDimmingPreference != null) {
            if (FileUtils.fileExists(DC_DISPLAY_NODE)) {
                mDcDimmingPreference.setEnabled(true);
                mDcDimmingPreference.setOnPreferenceChangeListener(this);
            } else {
                mDcDimmingPreference.setSummary(R.string.dc_dimming_enable_summary_not_supported);
                mDcDimmingPreference.setEnabled(false);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.registerOnSharedPreferenceChangeListener(mPrefsListener);
        if (mDcDimmingPreference != null) {
            mDcDimmingPreference.setChecked(prefs.getBoolean(DC_DIMMING_KEY, false));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getContext() != null) {
            PreferenceManager.getDefaultSharedPreferences(getContext())
                    .unregisterOnSharedPreferenceChangeListener(mPrefsListener);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (DC_DIMMING_KEY.equals(preference.getKey())) {
            boolean enabled = (boolean) newValue;

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            boolean hbmEnabled = prefs.getBoolean(HBM_KEY, false);

            if (enabled && hbmEnabled) {
                Toast.makeText(
                        getContext(),
                        R.string.dc_dimming_disable_hbm_first,
                        Toast.LENGTH_SHORT
                ).show();
                return false;
            }

            FileUtils.writeLine(DC_DISPLAY_NODE, enabled ? "1" : "0");
        }
        return true;
    }
}