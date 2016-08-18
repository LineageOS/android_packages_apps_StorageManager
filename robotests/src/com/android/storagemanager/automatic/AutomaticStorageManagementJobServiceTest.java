/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.storagemanager.automatic;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.job.JobParameters;
import android.content.ContentResolver;
import android.content.Context;
import android.os.BatteryManager;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.provider.Settings;
import com.android.storagemanager.overlay.FeatureFactory;
import com.android.storagemanager.overlay.StorageManagementJobProvider;
import com.android.storagemanager.testing.TestingConstants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.util.ReflectionHelpers;

import java.io.File;

import static junit.framework.Assert.assertFalse;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest= TestingConstants.MANIFEST, sdk=23)
public class AutomaticStorageManagementJobServiceTest {
    @Mock private BatteryManager mBatteryManager;
    @Mock private StorageManager mStorageManager;
    @Mock private NotificationManager mNotificationManager;
    @Mock private VolumeInfo mVolumeInfo;
    @Mock private File mFile;
    @Mock private JobParameters mJobParameters;
    @Mock private StorageManagementJobProvider mStorageManagementJobProvider;
    @Mock private FeatureFactory mFeatureFactory;
    private AutomaticStorageManagementJobService mJobService;
    private ShadowApplication mApplication;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(mJobParameters.getJobId()).thenReturn(0);

        // Let's set up our system services to act like a device that has the following conditions:
        // 1. We're plugged in and charging.
        // 2. We have a completely full device.
        // 3. ASM is disabled.
        when(mBatteryManager.isCharging()).thenReturn(true);
        when(mStorageManager.findVolumeById(VolumeInfo.ID_PRIVATE_INTERNAL))
                .thenReturn(mVolumeInfo);
        when(mVolumeInfo.getPath()).thenReturn(mFile);
        when(mFile.getTotalSpace()).thenReturn(100L);
        when(mFile.getFreeSpace()).thenReturn(0L);

        mApplication = ShadowApplication.getInstance();
        mApplication.setSystemService(Context.BATTERY_SERVICE, mBatteryManager);
        mApplication.setSystemService(Context.STORAGE_SERVICE, mStorageManager);
        mApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNotificationManager);

        // This is a hack-y injection of our own FeatureFactory.
        // By default, the Storage Manager has a FeatureFactory which returns null for all features.
        // Using reflection, we can inject our own FeatureFactory which returns a mock for the
        // StorageManagementJobProvider feature. This lets us observe when the ASMJobService
        // actually tries to run the job.
        when(mFeatureFactory.getStorageManagementJobProvider())
                .thenReturn(mStorageManagementJobProvider);
        when(mStorageManagementJobProvider.onStartJob(any(Context.class),
                any(JobParameters.class), any(Integer.class))).thenReturn(false);
        ReflectionHelpers.setStaticField(FeatureFactory.class, "sFactory", mFeatureFactory);

        // And we can't forget to initialize the actual job service.
        mJobService = spy(Robolectric.setupService(AutomaticStorageManagementJobService.class));
    }

    @Test
    public void testJobRequiresCharging() {
        when(mBatteryManager.isCharging()).thenReturn(false);
        assertFalse(mJobService.onStartJob(mJobParameters));
        // The job should report that it needs to be retried, if not charging.
        assertJobFinished(true);

        when(mBatteryManager.isCharging()).thenReturn(true);
        assertFalse(mJobService.onStartJob(mJobParameters));
        assertJobFinished(false);
    }

    @Test
    public void testStartJobTriesUpsellWhenASMDisabled() {
        assertFalse(mJobService.onStartJob(mJobParameters));
        assertJobFinished(false);
        verify(mNotificationManager).notify(eq(0), any(Notification.class));
        assertStorageManagerJobDidNotRun();
    }

    @Test
    public void testASMJobRunsWithValidConditions() {
        activateASM();
        assertFalse(mJobService.onStartJob(mJobParameters));
        assertStorageManagerJobRan();
    }

    @Test
    public void testJobDoesntRunIfStorageNotFull() {
        activateASM();
        when(mFile.getFreeSpace()).thenReturn(100L);
        assertFalse(mJobService.onStartJob(mJobParameters));
        assertStorageManagerJobDidNotRun();
    }

    @Test
    public void testJobOnlyRunsIfFreeStorageIsUnder15Percent() {
        activateASM();
        when(mFile.getFreeSpace()).thenReturn(15L);
        assertFalse(mJobService.onStartJob(mJobParameters));
        assertStorageManagerJobDidNotRun();

        when(mFile.getFreeSpace()).thenReturn(14L);
        assertFalse(mJobService.onStartJob(mJobParameters));
        assertStorageManagerJobRan();
    }

    @Test
    public void testNonDefaultDaysToRetain() {
        ContentResolver resolver = mApplication.getApplicationContext().getContentResolver();
        Settings.Secure.putInt(resolver, Settings.Secure.AUTOMATIC_STORAGE_MANAGER_DAYS_TO_RETAIN,
                30);
        activateASM();
        assertFalse(mJobService.onStartJob(mJobParameters));
        assertStorageManagerJobRan(30);
    }

    private void assertJobFinished(boolean retryNeeded) {
        verify(mJobService).jobFinished(any(JobParameters.class), eq(retryNeeded));
    }

    private void assertStorageManagerJobRan() {
        assertStorageManagerJobRan(
                Settings.Secure.AUTOMATIC_STORAGE_MANAGER_DAYS_TO_RETAIN_DEFAULT);
    }

    private void assertStorageManagerJobRan(int daysToRetain) {
        verify(mStorageManagementJobProvider).onStartJob(eq(mJobService), eq(mJobParameters),
                eq(daysToRetain));
    }

    private void assertStorageManagerJobDidNotRun() {
        verifyNoMoreInteractions(mStorageManagementJobProvider);
    }

    private void activateASM() {
        ContentResolver resolver = mApplication.getApplicationContext().getContentResolver();
        Settings.Secure.putInt(resolver, Settings.Secure.AUTOMATIC_STORAGE_MANAGER_ENABLED, 1);
    }
}
