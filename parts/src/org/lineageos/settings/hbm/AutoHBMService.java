package org.lineageos.settings.hbm;

import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import androidx.preference.PreferenceManager;
import org.lineageos.settings.utils.FileUtils;

public class AutoHBMService extends Service {
    private static final String HBM_NODE = "/sys/devices/platform/soc/soc:qcom,dsi-display-primary/hbm";
    private static final String BACKLIGHT_NODE = "/sys/class/backlight/panel0-backlight/brightness";
    private static final String DC_DIMMING_KEY = "dc_dimming";

    private static boolean mAutoHBMActive = false;
    
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mDisableRunnable;

    private SensorManager mSensorManager;
    private Sensor mLightSensor;

    private SharedPreferences mSharedPrefs;
    private boolean dcDimmingEnabled;

    private int mStoredBrightness = -1;
    private int mStoredBrightnessMode = -1;

    public void activateLightSensorRead() {
        if (mSensorManager != null && mLightSensor != null) {
            mSensorManager.registerListener(mSensorEventListener, mLightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void deactivateLightSensorRead() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(mSensorEventListener);
        }
        if (mDisableRunnable != null) {
            mHandler.removeCallbacks(mDisableRunnable);
        }
        mAutoHBMActive = false;
        enableHBM(false);
    }

    private void enableHBM(boolean enable) {
        if (enable) {
            if (mStoredBrightness == -1) {
                mStoredBrightness = Settings.System.getInt(getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS, 255);
            }
            if (mStoredBrightnessMode == -1) {
                mStoredBrightnessMode = Settings.System.getInt(getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            }

            Settings.System.putInt(getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);

            FileUtils.writeLine(HBM_NODE, "1");
            FileUtils.writeLine(BACKLIGHT_NODE, "2047");
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 255);
        } else {
            FileUtils.writeLine(HBM_NODE, "0");
            if (mStoredBrightness != -1) {
                FileUtils.writeLine(BACKLIGHT_NODE, String.valueOf(mStoredBrightness));
                Settings.System.putInt(getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS, mStoredBrightness);
                mStoredBrightness = -1;
            }
            if (mStoredBrightnessMode != -1) {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS_MODE, mStoredBrightnessMode);
                mStoredBrightnessMode = -1;
            }
        }
    }

    private boolean isCurrentlyEnabled() {
        return FileUtils.getFileValueAsBoolean(HBM_NODE, false);
    }

    private final SensorEventListener mSensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float lux = event.values[0];
            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            boolean keyguardShowing = km.inKeyguardRestrictedInputMode();
            
            float luxThreshold = Float.parseFloat(mSharedPrefs.getString(HBMFragment.AUTO_HBM_THRESHOLD_KEY, "20000"));
            long timeToDisableHBM = Long.parseLong(mSharedPrefs.getString(HBMFragment.HBM_DISABLE_TIME_KEY, "1"));

            dcDimmingEnabled = mSharedPrefs.getBoolean(DC_DIMMING_KEY, false);

            if (lux > luxThreshold) {
                if (mDisableRunnable != null) {
                    mHandler.removeCallbacks(mDisableRunnable);
                }
                if ((!mAutoHBMActive || !isCurrentlyEnabled()) && !keyguardShowing && !dcDimmingEnabled) {
                    mAutoHBMActive = true;
                    enableHBM(true);
                }
            } else { // lux < luxThreshold
                if (mAutoHBMActive) {
                    if (mDisableRunnable != null) {
                        mHandler.removeCallbacks(mDisableRunnable);
                    }
                    mDisableRunnable = () -> {
                        mAutoHBMActive = false;
                        enableHBM(false);
                    };
                    mHandler.postDelayed(mDisableRunnable, timeToDisableHBM * 1000);
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // do nothing
        }
    };

    private final BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                activateLightSensorRead();
            } else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                deactivateLightSensorRead();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mSensorManager = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        IntentFilter screenStateFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenStateReceiver, screenStateFilter);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm.isInteractive()) {
            activateLightSensorRead();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mScreenStateReceiver);
        deactivateLightSensorRead();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}