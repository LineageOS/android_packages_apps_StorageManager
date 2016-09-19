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
import android.content.pm.ApplicationInfo;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.storagemanager.deletionhelper.AppStateUsageStatsBridge.UsageStatsState;
import com.android.storagemanager.testing.TestingConstants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=TestingConstants.MANIFEST, sdk=TestingConstants.SDK_VERSION)
public class AppDeletionPreferenceTest {
    private static final String TEST_PACKAGE_LABEL = "App";
    private static final String TEST_PACKAGE_NAME = "com.package.mcpackageface";
    @Mock private AppEntry mEntry;
    @Mock private ApplicationInfo mInfo;
    private Context mContext;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;

        // Initialize the entry.
        mEntry.extraInfo = new UsageStatsState(30, 30, 0);
        mEntry.icon = null;
        mEntry.label = "App";
        mEntry.size = 1024L;
        mEntry.info = mInfo;
        mInfo.packageName = TEST_PACKAGE_NAME;
    }

    @Test
    public void testPreferenceSummary() {
        AppDeletionPreference preference = new AppDeletionPreference(mContext, mEntry);
        preference.updateSummary();

        assertThat(preference.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(preference.getTitle()).isEqualTo(TEST_PACKAGE_LABEL);
        assertThat(preference.getSummary().toString()).isEqualTo("1.00KB • 30 days ago");
    }

    @Test
    public void testNeverUsedPreferenceSummary() {
        mEntry.extraInfo = new UsageStatsState(AppStateUsageStatsBridge.NEVER_USED, 30, 0);

        AppDeletionPreference preference = new AppDeletionPreference(mContext, mEntry);
        preference.updateSummary();

        assertThat(preference.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(preference.getTitle()).isEqualTo(TEST_PACKAGE_LABEL);
        assertThat(preference.getSummary().toString()).isEqualTo("1.00KB • Never used before");
    }

    @Test
    public void testUnknownLastUsePreferenceSummary() {
        mEntry.extraInfo = new UsageStatsState(AppStateUsageStatsBridge.UNKNOWN_LAST_USE, 30, 0);

        AppDeletionPreference preference = new AppDeletionPreference(mContext, mEntry);
        preference.updateSummary();

        assertThat(preference.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(preference.getTitle()).isEqualTo(TEST_PACKAGE_LABEL);
        assertThat(preference.getSummary().toString())
                .isEqualTo("1.00KB • Not sure when last used");
    }

    @Test
    public void testSizeSummary() {
        mEntry.size = 100L;

        AppDeletionPreference preference = new AppDeletionPreference(mContext, mEntry);
        preference.updateSummary();

        assertThat(preference.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(preference.getTitle()).isEqualTo(TEST_PACKAGE_LABEL);
        assertThat(preference.getSummary().toString()).isEqualTo("100B • 30 days ago");
    }
}
