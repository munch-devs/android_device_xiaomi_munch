/*
 * Copyright (C) 2016 The OmniROM Project
                 2023 The Evolution X Project
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.lineageos.settings.hbm;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import androidx.preference.PreferenceManager;

public class AutoHBMTimePreference extends CustomSeekBarPreference {

    private static final int mMinVal = 1;
    private static final int mMaxVal = 10;
    private static final int mDefVal = 1;

    public AutoHBMTimePreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mInterval = 1;
        mShowSign = false;
        mUnits = "";
        mContinuousUpdates = false;
        mMinValue = mMinVal;
        mMaxValue = mMaxVal;
        mDefaultValueExists = true;
        mDefaultValue = mDefVal;
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        mValue = Integer.parseInt(sharedPrefs.getString(HBMFragment.HBM_DISABLE_TIME_KEY, "1"));

        setPersistent(false);
    }

    @Override
    protected void changeValue(int newValue) {
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                .putString(HBMFragment.HBM_DISABLE_TIME_KEY, String.valueOf(newValue))
                .apply();
    }
}