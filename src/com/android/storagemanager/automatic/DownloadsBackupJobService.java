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
import android.provider.Settings;

import com.android.storagemanager.overlay.DownloadsBackupJobProvider;
import com.android.storagemanager.overlay.FeatureFactory;

/**
 * This {@link JobService} starts an automatic job to backup the Downloads folder. It only runs when
 * backup is enabled.
 */
public class DownloadsBackupJobService extends JobService {
    private DownloadsBackupJobProvider mJobProvider;

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        // Check that conditions for task are met again. This is necessary because the JobScheduler
        // is forced to run even if the conditions are not met.
        if (!isBackupEnabled() || !preconditionsFulfilled()){
            return false;
        }

        mJobProvider = FeatureFactory.getFactory(this).getDownloadsBackupJobProvider();

        if (mJobProvider != null) {
            return mJobProvider.onStartJob(this);
        }

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        if (mJobProvider != null) {
            return mJobProvider.onStopJob(this);
        }
        return true;
    }

    private boolean preconditionsFulfilled() {
        Context context = getApplicationContext();
        boolean isNetworkMetered = JobPreconditions.isNetworkMetered(context);
        boolean isWifiConnected = JobPreconditions.isWifiConnected(context);
        boolean isCharging = JobPreconditions.isCharging(context);
        boolean isIdle = JobPreconditions.isIdle(context);

        return !isNetworkMetered && isWifiConnected && isCharging && isIdle;
    }

    public boolean isBackupEnabled() {
        return Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.DOWNLOADS_BACKUP_ENABLED, 0) != 0;
    }
}