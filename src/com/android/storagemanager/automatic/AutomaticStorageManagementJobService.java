/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.storagemanager.automatic;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.provider.Settings;
import android.util.Log;
import com.android.storagemanager.overlay.FeatureFactory;
import com.android.storagemanager.overlay.StorageManagementJobProvider;

import java.io.File;

/**
 * {@link JobService} class to start automatic storage clearing jobs to free up space. The job only
 * starts if the device is under a certain percent of free storage.
 */
public class AutomaticStorageManagementJobService extends JobService {
    private static final String TAG = "AsmJobService";

    private static final long DEFAULT_LOW_FREE_PERCENT = 15;

    private StorageManagementJobProvider mProvider;

    @Override
    public boolean onStartJob(JobParameters args) {
        StorageManager manager = getSystemService(StorageManager.class);
        VolumeInfo internalVolume = manager.findVolumeById(VolumeInfo.ID_PRIVATE_INTERNAL);
        final File dataPath = internalVolume.getPath();
        if (!volumeNeedsManagement(dataPath)) {
            Log.i(TAG, "Skipping automatic storage management.");
            Settings.Secure.putLong(getContentResolver(),
                    Settings.Secure.AUTOMATIC_STORAGE_MANAGER_LAST_RUN,
                    System.currentTimeMillis());
            return false;
        }

        boolean isEnabled =
                Settings.Secure.getInt(getContentResolver(),
                        Settings.Secure.AUTOMATIC_STORAGE_MANAGER_ENABLED, 0) != 0;
        if (!isEnabled) {
            NotificationController.maybeShowNotification(getApplicationContext());
            return false;
        }

        mProvider = FeatureFactory.getFactory(this).getStorageManagementJobProvider();
        if (mProvider != null) {
            return mProvider.onStartJob(this, args, getDaysToRetain());
        }

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters args) {
        if (mProvider != null) {
            return mProvider.onStopJob(this, args);
        }

        return false;
    }

    private int getDaysToRetain() {
        return Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.AUTOMATIC_STORAGE_MANAGER_DAYS_TO_RETAIN,
                Settings.Secure.AUTOMATIC_STORAGE_MANAGER_DAYS_TO_RETAIN_DEFAULT);
    }

    private boolean volumeNeedsManagement(final File dataPath) {
        long lowStorageThreshold = (dataPath.getTotalSpace() * DEFAULT_LOW_FREE_PERCENT) / 100;
        return dataPath.getFreeSpace() < lowStorageThreshold;
    }
}