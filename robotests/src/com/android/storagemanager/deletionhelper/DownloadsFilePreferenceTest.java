/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.text.format.DateUtils;
import com.android.storagemanager.testing.TestingConstants;
import com.android.storagemanager.utils.IconProvider;
import java.io.File;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestingConstants.MANIFEST, sdk = TestingConstants.SDK_VERSION)
public class DownloadsFilePreferenceTest {

    //23-01-2100
    private static final long TEST_FILE_TIME = 4104374400000L;

    private String mReadableDate;
    private Context mContext;
    @Mock private IconProvider mIconProvider;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mReadableDate =
                DateUtils.formatDateTime(mContext, TEST_FILE_TIME, DateUtils.FORMAT_SHOW_DATE);
    }

    @Test
    public void testSizeSummary() {
        // Initialize the entry.
        File file = mock(File.class);
        when(file.getName()).thenReturn("FakeFile");
        when(file.lastModified()).thenReturn(TEST_FILE_TIME);
        when(file.length()).thenReturn(100L);
        when(mIconProvider.loadMimeIcon(any())).thenReturn(null);

        DownloadsFilePreference preference =
                new DownloadsFilePreference(mContext, file, mIconProvider);

        assertThat(preference.getTitle()).isEqualTo("FakeFile");
        assertThat(preference.getSummary().toString()).isEqualTo(mReadableDate);
        assertThat(preference.getItemSize()).isEqualTo("100B");
    }
}
