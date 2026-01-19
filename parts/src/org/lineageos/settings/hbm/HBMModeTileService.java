package org.lineageos.settings.hbm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import androidx.preference.PreferenceManager;
import android.widget.Toast;
import org.lineageos.settings.R;

public class HBMModeTileService extends TileService {

    private static final String DC_DIMMING_KEY = "dc_dimming";
    private static final String HBM_KEY = "hbm";

    private final SharedPreferences.OnSharedPreferenceChangeListener mPrefsListener =
            (prefs, key) -> {
                if (HBM_KEY.equals(key)) {
                    updateUI(prefs.getBoolean(HBM_KEY, false));
                }
            };

    private final BroadcastReceiver mScreenOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                boolean turnOffOnScreenOff = prefs.getBoolean(
                        HBMFragment.HBM_TURN_OFF_ON_SCREEN_OFF_KEY, false);
                boolean hbmEnabled = prefs.getBoolean(HBM_KEY, false);

                if (turnOffOnScreenOff && hbmEnabled) {
                    HBMModeSwitch.executeHBMAction(context, prefs, false);

                    prefs.edit()
                            .putBoolean(HBM_KEY, false)
                            .remove("last_brightness")
                            .remove("last_brightness_mode")
                            .apply();
                }
            }
        }
    };

    private void updateUI(boolean enabled) {
        final Tile tile = getQsTile();
        if (tile != null) {
            tile.setState(enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            tile.updateTile();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerReceiver(mScreenOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(mPrefsListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mScreenOffReceiver);
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(mPrefsListener);
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        updateUI(sharedPrefs.getBoolean(HBM_KEY, false));
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
    }

    @Override
    public void onClick() {
        super.onClick();

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean dcDimmingEnabled = sharedPrefs.getBoolean(DC_DIMMING_KEY, false);

        if (dcDimmingEnabled) {
            Toast.makeText(
                    this,
                    R.string.hbm_disable_dc_dimming_first,
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        final boolean enabled = !sharedPrefs.getBoolean(HBM_KEY, false);

        HBMModeSwitch.executeHBMAction(this, sharedPrefs, enabled);

        sharedPrefs.edit().putBoolean(HBM_KEY, enabled).apply();
    }
}