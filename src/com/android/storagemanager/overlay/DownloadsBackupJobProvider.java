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

package com.android.storagemanager.overlay;

import android.app.job.JobParameters;
import android.content.Context;

/**
 * Feature provider for automatic backup jobs.
 */
public interface DownloadsBackupJobProvider {
    /**
     * Starts asynchronous task to backup the Downloads folder.
     * @return If the job needs to process work on a separate thread.
     */
    boolean onStartJob(Context context, JobParameters jobParameters);

    /**
     * Stops the execution of the task.
     * @return If the job should be rescheduled.
     */
    boolean onStopJob(Context context, JobParameters jobParameters);
}
