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
import android.support.v7.preference.PreferenceViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.android.storagemanager.testing.TestingConstants;
import com.android.storagemanager.R;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=TestingConstants.MANIFEST, sdk=TestingConstants.SDK_VERSION)
public class PhotosDeletionPreferenceTest {
    private Context mContext;
    private PreferenceViewHolder mHolder;
    private PhotosDeletionPreference mPreference;

    @Before
    public void setUp() throws Exception {
        mContext = RuntimeEnvironment.application;
        mPreference = new PhotosDeletionPreference(mContext, null);

        // Inflate the preference and the widget.
        LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view = inflater.inflate(mPreference.getLayoutResource(),
                new LinearLayout(mContext), false);
        inflater.inflate(mPreference.getWidgetLayoutResource(),
                (ViewGroup) view.findViewById(android.R.id.widget_frame));

        mHolder = new PreferenceViewHolder(view);
    }

    @Test
    public void testConstructor() {
        assertEquals(0, mPreference.getFreeableBytes());
    }

    @Test
    public void testItemVisibilityBeforeLoaded() {
        mPreference.onBindViewHolder(mHolder);
        assertEquals(View.VISIBLE, mHolder.findViewById(R.id.progress_bar).getVisibility());
        assertEquals(View.GONE, mHolder.findViewById(android.R.id.icon).getVisibility());
        assertEquals(View.GONE, mHolder.findViewById(android.R.id.checkbox).getVisibility());
    }

    @Test
    public void testItemVisibilityAfterLoaded() {
        mPreference.onFreeableChanged(0, 0);
        Robolectric.flushBackgroundThreadScheduler();
        Robolectric.flushForegroundThreadScheduler();
        mPreference.onBindViewHolder(mHolder);

        // After onFreeableChanged is called, we're no longer loading.
        assertEquals(View.GONE, mHolder.findViewById(R.id.progress_bar).getVisibility());
        assertEquals(View.VISIBLE, mHolder.findViewById(android.R.id.icon).getVisibility());
        assertEquals(View.VISIBLE, mHolder.findViewById(android.R.id.checkbox).getVisibility());
    }

    @Test
    public void testTitleAndSummaryAfterLoaded() {
        mPreference.onFreeableChanged(10, 1024L);
        Robolectric.flushBackgroundThreadScheduler();
        Robolectric.flushForegroundThreadScheduler();
        mPreference.onBindViewHolder(mHolder);

        assertEquals("Photos & videos (10)", mPreference.getTitle());
        assertEquals("1.00KB • Older than 30 days", mPreference.getSummary().toString());
    }
}
