package com.arjanvlek.oxygenupdater.domain;


import android.os.Build;
import android.support.annotation.NonNull;

import com.arjanvlek.oxygenupdater.BuildConfig;
import com.arjanvlek.oxygenupdater.internal.logger.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import static com.arjanvlek.oxygenupdater.ApplicationData.NO_OXYGEN_OS;

public class SystemVersionProperties {

    private final String oxygenDeviceName;
    private final String oxygenOSVersion;
    private final String oxygenOSOTAVersion;
    private final String securityPatchDate;
    private final String oemFingerprint;
    private static final String TAG = "SystemVersionProperties";

    public SystemVersionProperties(boolean uploadLog) {
        String oxygenOSVersion1 = NO_OXYGEN_OS;
        String oxygenOSVersion2 = NO_OXYGEN_OS;
        String oxygenOSOTAVersion = NO_OXYGEN_OS;
        String oxygenDeviceName = NO_OXYGEN_OS;
        String oemFingerprint = NO_OXYGEN_OS;
        String securityPatchDate = NO_OXYGEN_OS;
        try {
            Process getBuildPropProcess = Runtime.getRuntime().exec("getprop");

            Logger.logVerbose(TAG, "Started fetching device properties using 'getprop' command...");

            BufferedReader in = new BufferedReader(new InputStreamReader(getBuildPropProcess.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                oxygenDeviceName = readBuildPropItem(oxygenDeviceName, BuildConfig.DEVICE_NAME_LOOKUP_KEY, inputLine, "Detected Oxygen OS Device: %s ...");
                oxygenOSVersion1 = readBuildPropItem(oxygenOSVersion1, BuildConfig.OS_VERSION_NUMBER_LOOKUP_KEY_1, inputLine, "Detected Oxygen OS ROM with version %s ...");
                oxygenOSVersion2 = readBuildPropItem(oxygenOSVersion2, BuildConfig.OS_VERSION_NUMBER_LOOKUP_KEY_2, inputLine, "Detected Oxygen OS ROM with version %s (legacy method)...");
                oxygenOSOTAVersion = readBuildPropItem(oxygenOSOTAVersion, BuildConfig.OS_OTA_VERSION_NUMBER_LOOKUP_KEY, inputLine, "Detected Oxygen OS ROM with OTA version %s ...");
                oemFingerprint = readBuildPropItem(oemFingerprint, BuildConfig.BUILD_FINGERPRINT_LOOKUP_KEY, inputLine, "Detected build fingerprint: %s ...");

                if(securityPatchDate.equals(NO_OXYGEN_OS)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        securityPatchDate = Build.VERSION.SECURITY_PATCH;
                    } else {
                        securityPatchDate = readBuildPropItem(securityPatchDate, "ro.build.version.security_patch", inputLine, "Detected security patch level of %s ...");
                    }
                }
            }
            getBuildPropProcess.destroy();
            Logger.logVerbose(TAG, "Finished fetching device properties using 'getprop' command...");

        } catch (Exception e) {
            Logger.logError(uploadLog, TAG, e.getLocalizedMessage());
        }
        this.oxygenDeviceName = oxygenDeviceName;
        if(!oxygenOSVersion1.equals(NO_OXYGEN_OS)) {
            this.oxygenOSVersion = oxygenOSVersion1;
        } else {
            this.oxygenOSVersion = oxygenOSVersion2;
        }
        this.oxygenOSOTAVersion = oxygenOSOTAVersion;
        this.oemFingerprint = oemFingerprint;
        this.securityPatchDate = securityPatchDate;
    }

    private String readBuildPropItem(@NonNull String result, String itemKey, String inputLine, String logText) {
        if (inputLine == null) return result;

        if (inputLine.contains(itemKey)) {
            result = inputLine.replace("[" + itemKey + "]: ", "");
            result = result.replace("[", "");
            result = result.replace("]", "");
            if(logText != null) Logger.logVerbose(TAG, String.format(logText, result));
        }

        return result;
    }

    public String getOxygenDeviceName() {
        return oxygenDeviceName;
    }

    public String getOxygenOSVersion() {
        return oxygenOSVersion;
    }

    public String getOxygenOSOTAVersion() {
        return oxygenOSOTAVersion;
    }

    public String getSecurityPatchDate() {
        return securityPatchDate;
    }

    public String getOemFingerprint() {
        return oemFingerprint;
    }
}
