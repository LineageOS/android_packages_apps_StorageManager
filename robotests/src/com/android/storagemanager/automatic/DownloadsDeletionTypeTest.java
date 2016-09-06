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

import android.os.Bundle;
import android.os.Environment;
import com.android.storagemanager.deletionhelper.DeletionType;
import com.android.storagemanager.deletionhelper.DownloadsDeletionType;
import com.android.storagemanager.deletionhelper.FetchDownloadsLoader.DownloadsResult;
import com.android.storagemanager.testing.TestingConstants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileWriter;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@Config(manifest= TestingConstants.MANIFEST, sdk=23)
public class DownloadsDeletionTypeTest {
    private DownloadsDeletionType mDeletion;
    private File mDownloadsDirectory;

    @Before
    public void setUp() {
        mDeletion = new DownloadsDeletionType(RuntimeEnvironment.application, null);
        mDownloadsDirectory =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    }

    @Test
    public void testInitializeWithUncheckedFiles() throws Exception {
        File temp = new File(mDownloadsDirectory, "temp");
        File temp2 = new File(mDownloadsDirectory, "temp2");
        String[] filePaths = new String[2];
        filePaths[0] = temp.getPath();
        filePaths[1] = temp2.getPath();
        mDeletion = new DownloadsDeletionType(RuntimeEnvironment.application, filePaths);

        assertFalse(mDeletion.isChecked(temp));
        assertFalse(mDeletion.isChecked(temp2));
    }

    @Test
    public void testFetchDownloads() throws Exception {
        File temp = new File(mDownloadsDirectory, "temp");
        File temp2 = new File(mDownloadsDirectory, "temp2");
        DownloadsResult result = new DownloadsResult();
        result.files.add(temp);
        result.files.add(temp2);

        mDeletion.onLoadFinished(null, result);
        Set<File> fileSet = mDeletion.getFiles();

        assertTrue(fileSet.contains(temp));
        assertTrue(fileSet.contains(temp2));
    }

    @Test
    public void testSetChecked() throws Exception {
        File temp = new File(mDownloadsDirectory, "temp");
        DownloadsResult result = new DownloadsResult();
        result.files.add(temp);

        mDeletion.onLoadFinished(null, result);

        // Downloads files are default checked.
        assertTrue(mDeletion.isChecked(temp));
        mDeletion.setFileChecked(temp, false);

        assertFalse(mDeletion.isChecked(temp));
        mDeletion.setFileChecked(temp, true);

        assertTrue(mDeletion.isChecked(temp));
    }

    @Test
    public void testUncheckedFilesDoNotCountForSize() throws Exception {
        File temp = new File(mDownloadsDirectory, "temp");
        FileWriter fileWriter = new FileWriter(temp);
        fileWriter.write("test");
        fileWriter.close();
        DownloadsResult result = new DownloadsResult();
        result.files.add(temp);

        mDeletion.onLoadFinished(null, result);

        // Downloads files are default checked.
        assertTrue(mDeletion.isChecked(temp));
        assertEquals(4, mDeletion.getFreeableBytes());

        mDeletion.setFileChecked(temp, false);
        assertEquals(0, mDeletion.getFreeableBytes());
    }

    @Test
    public void testSaveAndRestoreRemembersUncheckedFiles() throws Exception {
        File temp = new File(mDownloadsDirectory, "temp");
        File temp2 = new File(mDownloadsDirectory, "temp2");
        DownloadsResult result = new DownloadsResult();
        result.files.add(temp);
        result.files.add(temp2);
        mDeletion.onLoadFinished(null, result);

        mDeletion.setFileChecked(temp, false);
        Bundle savedBundle = new Bundle();
        mDeletion.onSaveInstanceStateBundle(savedBundle);
        mDeletion = new DownloadsDeletionType(RuntimeEnvironment.application,
                savedBundle.getStringArray(DownloadsDeletionType.EXTRA_UNCHECKED_DOWNLOADS));

        assertFalse(mDeletion.isChecked(temp));
        assertTrue(mDeletion.isChecked(temp2));
    }

    @Test
    public void testCallbackOnFileLoad() throws Exception {
        File temp = new File(mDownloadsDirectory, "temp");
        File temp2 = new File(mDownloadsDirectory, "temp2");
        DownloadsResult result = new DownloadsResult();
        result.files.add(temp);
        result.files.add(temp2);
        result.totalSize = 101L;

        DeletionType.FreeableChangedListener mockListener =
                mock(DeletionType.FreeableChangedListener.class);
        mDeletion.registerFreeableChangedListener(mockListener);

        // Calls back immediately when we add a listener with its most current info.
        verify(mockListener).onFreeableChanged(eq(0), eq(0L));

        // Callback when the load finishes.
        mDeletion.onLoadFinished(null, result);
        verify(mockListener).onFreeableChanged(eq(2), eq(101L));
    }

}
