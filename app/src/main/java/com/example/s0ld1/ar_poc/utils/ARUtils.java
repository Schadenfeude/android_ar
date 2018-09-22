package com.example.s0ld1.ar_poc.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

public final class ARUtils {
    private ARUtils() {
    }

    public static boolean checkForARSupport(Activity activity, String TAG) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }

        final String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < 3.0) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();

            activity.finish();
            return false;
        }
        return true;
    }

    public static void requestARInstall(Activity activity, boolean installAlreadyRequested) {
        try {
            ArCoreApk.getInstance().requestInstall(activity, !installAlreadyRequested);
        } catch (UnavailableDeviceNotCompatibleException | UnavailableUserDeclinedInstallationException e) {
            e.printStackTrace();
        }
    }
}
