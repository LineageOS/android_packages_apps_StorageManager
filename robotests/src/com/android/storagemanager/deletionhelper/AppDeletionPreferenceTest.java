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

package com.android.storagemanager.deletionhelper;

import android.content.Context;
import com.android.storagemanager.deletionhelper.AppsAsyncLoader.PackageInfo;
import com.android.storagemanager.testing.TestingConstants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=TestingConstants.MANIFEST, sdk=TestingConstants.SDK_VERSION)
public class AppDeletionPreferenceTest {
    private static final String TEST_PACKAGE_LABEL = "App";
    private static final String TEST_PACKAGE_NAME = "com.package.mcpackageface";
    private Context mContext;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void testPreferenceSummary() {
        // Initialize the entry.
        PackageInfo app =
                new PackageInfo.Builder()
                        .setDaysSinceLastUse(30)
                        .setdaysSinceFirstInstall(30)
                        .setPackageName(TEST_PACKAGE_NAME)
                        .setSize(1024L)
                        .setLabel(TEST_PACKAGE_LABEL)
                        .build();
        AppDeletionPreference preference = new AppDeletionPreference(mContext, app);
        preference.updateSummary();

        assertThat(preference.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(preference.getTitle()).isEqualTo(TEST_PACKAGE_LABEL);
        assertThat(preference.getSummary().toString()).isEqualTo("1.00KB • 30 days ago");
    }

    @Test
    public void testNeverUsedPreferenceSummary() {
        // Initialize the entry.
        PackageInfo app =
                new PackageInfo.Builder()
                        .setDaysSinceLastUse(AppsAsyncLoader.NEVER_USED)
                        .setdaysSinceFirstInstall(30)
                        .setPackageName(TEST_PACKAGE_NAME)
                        .setSize(1024L)
                        .setLabel(TEST_PACKAGE_LABEL)
                        .build();
        AppDeletionPreference preference = new AppDeletionPreference(mContext, app);
        preference.updateSummary();

        assertThat(preference.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(preference.getTitle()).isEqualTo(TEST_PACKAGE_LABEL);
        assertThat(preference.getSummary().toString()).isEqualTo("1.00KB • Not used in last year");
    }

    @Test
    public void testUnknownLastUsePreferenceSummary() {
        // Initialize the entry.
        PackageInfo app =
                new PackageInfo.Builder()
                        .setDaysSinceLastUse(AppsAsyncLoader.UNKNOWN_LAST_USE)
                        .setdaysSinceFirstInstall(30)
                        .setPackageName(TEST_PACKAGE_NAME)
                        .setSize(1024L)
                        .setLabel(TEST_PACKAGE_LABEL)
                        .build();

        AppDeletionPreference preference = new AppDeletionPreference(mContext, app);
        preference.updateSummary();

        assertThat(preference.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(preference.getTitle()).isEqualTo(TEST_PACKAGE_LABEL);
        assertThat(preference.getSummary().toString())
                .isEqualTo("1.00KB • Not sure when last used");
    }

    @Test
    public void testSizeSummary() {
        // Initialize the entry.
        PackageInfo app =
                new PackageInfo.Builder()
                        .setDaysSinceLastUse(30)
                        .setdaysSinceFirstInstall(30)
                        .setPackageName(TEST_PACKAGE_NAME)
                        .setSize(100L)
                        .setLabel(TEST_PACKAGE_LABEL)
                        .build();

        AppDeletionPreference preference = new AppDeletionPreference(mContext, app);
        preference.updateSummary();

        assertThat(preference.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(preference.getTitle()).isEqualTo(TEST_PACKAGE_LABEL);
        assertThat(preference.getSummary().toString()).isEqualTo("100B • 30 days ago");
    }
}
