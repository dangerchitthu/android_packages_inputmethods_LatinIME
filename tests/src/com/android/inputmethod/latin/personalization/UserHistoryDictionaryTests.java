/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.inputmethod.latin.personalization;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;

import com.android.inputmethod.latin.ExpandableBinaryDictionary;
import com.android.inputmethod.latin.NgramContext;
import com.android.inputmethod.latin.NgramContext.WordInfo;
import com.android.inputmethod.latin.utils.BinaryDictionaryUtils;
import com.android.inputmethod.latin.utils.DistracterFilter;
import com.android.inputmethod.latin.utils.FileUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Unit tests for UserHistoryDictionary
 */
@LargeTest
public class UserHistoryDictionaryTests extends AndroidTestCase {
    private static final String TAG = UserHistoryDictionaryTests.class.getSimpleName();
    private static final int WAIT_FOR_WRITING_FILE_IN_MILLISECONDS = 3000;
    private static final String TEST_LOCALE_PREFIX = "test_";

    private static final String[] CHARACTERS = {
        "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m",
        "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"
    };

    private int mCurrentTime = 0;

    private void removeAllTestDictFiles() {
        final Locale dummyLocale = new Locale(TEST_LOCALE_PREFIX);
        final String dictName = ExpandableBinaryDictionary.getDictName(
                UserHistoryDictionary.NAME, dummyLocale, null /* dictFile */);
        final File dictFile = ExpandableBinaryDictionary.getDictFile(
                mContext, dictName, null /* dictFile */);
        final FilenameFilter filenameFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.startsWith(UserHistoryDictionary.NAME + "." + TEST_LOCALE_PREFIX);
            }
        };
        FileUtils.deleteFilteredFiles(dictFile.getParentFile(), filenameFilter);
    }

    private static void printAllFiles(final File dir) {
        Log.d(TAG, dir.getAbsolutePath());
        for (final File file : dir.listFiles()) {
            Log.d(TAG, "  " + file.getName());
        }
    }

    private static void checkExistenceAndRemoveDictFile(final UserHistoryDictionary dict,
            final File dictFile) {
        Log.d(TAG, "waiting for writing ...");
        dict.waitAllTasksForTests();
        if (!dictFile.exists()) {
            try {
                Log.d(TAG, dictFile + " is not existing. Wait "
                        + WAIT_FOR_WRITING_FILE_IN_MILLISECONDS + " ms for writing.");
                printAllFiles(dictFile.getParentFile());
                Thread.sleep(WAIT_FOR_WRITING_FILE_IN_MILLISECONDS);
            } catch (final InterruptedException e) {
                Log.e(TAG, "Interrupted during waiting for writing the dict file.");
            }
        }
        assertTrue("check exisiting of " + dictFile, dictFile.exists());
        FileUtils.deleteRecursively(dictFile);
    }

    private static Locale getDummyLocale(final String name) {
        return new Locale(TEST_LOCALE_PREFIX + name + System.currentTimeMillis());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        resetCurrentTimeForTestMode();
        removeAllTestDictFiles();
    }

    @Override
    protected void tearDown() throws Exception {
        removeAllTestDictFiles();
        stopTestModeInNativeCode();
        super.tearDown();
    }

    private void resetCurrentTimeForTestMode() {
        mCurrentTime = 0;
        setCurrentTimeForTestMode(mCurrentTime);
    }

    private void forcePassingShortTime() {
        // 3 days.
        final int timeToElapse = (int)TimeUnit.DAYS.toSeconds(3);
        mCurrentTime += timeToElapse;
        setCurrentTimeForTestMode(mCurrentTime);
    }

    private void forcePassingLongTime() {
        // 365 days.
        final int timeToElapse = (int)TimeUnit.DAYS.toSeconds(365);
        mCurrentTime += timeToElapse;
        setCurrentTimeForTestMode(mCurrentTime);
    }

    private static int setCurrentTimeForTestMode(final int currentTime) {
        return BinaryDictionaryUtils.setCurrentTimeForTest(currentTime);
    }

    private static int stopTestModeInNativeCode() {
        return BinaryDictionaryUtils.setCurrentTimeForTest(-1);
    }

    /**
     * Generates a random word.
     */
    private static String generateWord(final int value) {
        final int lengthOfChars = CHARACTERS.length;
        StringBuilder builder = new StringBuilder();
        long lvalue = Math.abs((long)value);
        while (lvalue > 0) {
            builder.append(CHARACTERS[(int)(lvalue % lengthOfChars)]);
            lvalue /= lengthOfChars;
        }
        return builder.toString();
    }

    private static List<String> generateWords(final int number, final Random random) {
        final HashSet<String> wordSet = new HashSet<>();
        while (wordSet.size() < number) {
            wordSet.add(generateWord(random.nextInt()));
        }
        return new ArrayList<>(wordSet);
    }

    private static void addToDict(final UserHistoryDictionary dict, final List<String> words,
            final int timestamp) {
        NgramContext ngramContext = NgramContext.EMPTY_PREV_WORDS_INFO;
        for (String word : words) {
            UserHistoryDictionary.addToDictionary(dict, ngramContext, word, true, timestamp,
                    DistracterFilter.EMPTY_DISTRACTER_FILTER);
            ngramContext = ngramContext.getNextNgramContext(new WordInfo(word));
        }
    }

    /**
     * @param checkContents if true, checks whether written words are actually in the dictionary
     * or not.
     */
    private void addAndWriteRandomWords(final UserHistoryDictionary dict,
            final int numberOfWords, final Random random, final boolean checkContents) {
        final List<String> words = generateWords(numberOfWords, random);
        // Add random words to the user history dictionary.
        addToDict(dict, words, mCurrentTime);
        if (checkContents) {
            dict.waitAllTasksForTests();
            for (int i = 0; i < numberOfWords; ++i) {
                final String word = words.get(i);
                assertTrue(dict.isInDictionary(word));
            }
        }
        // write to file.
        dict.close();
    }

    /**
     * Clear all entries in the user history dictionary.
     * @param dict the user history dictionary.
     */
    private static void clearHistory(final UserHistoryDictionary dict) {
        dict.waitAllTasksForTests();
        dict.clear();
        dict.close();
        dict.waitAllTasksForTests();
    }

    public void testRandomWords() {
        Log.d(TAG, "This test can be used for profiling.");
        Log.d(TAG, "Usage: please set UserHistoryDictionary.PROFILE_SAVE_RESTORE to true.");
        final Locale dummyLocale = getDummyLocale("random_words");
        final String dictName = ExpandableBinaryDictionary.getDictName(
                UserHistoryDictionary.NAME, dummyLocale, null /* dictFile */);
        final File dictFile = ExpandableBinaryDictionary.getDictFile(
                mContext, dictName, null /* dictFile */);
        final UserHistoryDictionary dict = PersonalizationHelper.getUserHistoryDictionary(
                getContext(), dummyLocale);

        final int numberOfWords = 1000;
        final Random random = new Random(123456);
        clearHistory(dict);
        addAndWriteRandomWords(dict, numberOfWords, random, true /* checksContents */);
        checkExistenceAndRemoveDictFile(dict, dictFile);
    }

    public void testStressTestForSwitchingLanguagesAndAddingWords() {
        final int numberOfLanguages = 2;
        final int numberOfLanguageSwitching = 80;
        final int numberOfWordsInsertedForEachLanguageSwitch = 100;

        final File dictFiles[] = new File[numberOfLanguages];
        final UserHistoryDictionary dicts[] = new UserHistoryDictionary[numberOfLanguages];

        try {
            final Random random = new Random(123456);

            // Create filename suffixes for this test.
            for (int i = 0; i < numberOfLanguages; i++) {
                final Locale dummyLocale = getDummyLocale("switching_languages" + i);
                final String dictName = ExpandableBinaryDictionary.getDictName(
                        UserHistoryDictionary.NAME, dummyLocale, null /* dictFile */);
                dictFiles[i] = ExpandableBinaryDictionary.getDictFile(
                        mContext, dictName, null /* dictFile */);
                dicts[i] = PersonalizationHelper.getUserHistoryDictionary(getContext(),
                        dummyLocale);
                clearHistory(dicts[i]);
            }

            final long start = System.currentTimeMillis();

            for (int i = 0; i < numberOfLanguageSwitching; i++) {
                final int index = i % numberOfLanguages;
                // Switch to dicts[index].
                addAndWriteRandomWords(dicts[index], numberOfWordsInsertedForEachLanguageSwitch,
                        random, false /* checksContents */);
            }

            final long end = System.currentTimeMillis();
            Log.d(TAG, "testStressTestForSwitchingLanguageAndAddingWords took "
                    + (end - start) + " ms");
        } finally {
            for (int i = 0; i < numberOfLanguages; i++) {
                checkExistenceAndRemoveDictFile(dicts[i], dictFiles[i]);
            }
        }
    }

    public void testAddManyWords() {
        final Locale dummyLocale = getDummyLocale("many_random_words");
        final String dictName = ExpandableBinaryDictionary.getDictName(
                UserHistoryDictionary.NAME, dummyLocale, null /* dictFile */);
        final File dictFile = ExpandableBinaryDictionary.getDictFile(
                mContext, dictName, null /* dictFile */);
        final int numberOfWords = 10000;
        final Random random = new Random(123456);
        final UserHistoryDictionary dict = PersonalizationHelper.getUserHistoryDictionary(
                getContext(), dummyLocale);
        clearHistory(dict);
        try {
            addAndWriteRandomWords(dict, numberOfWords, random, true /* checksContents */);
        } finally {
            checkExistenceAndRemoveDictFile(dict, dictFile);
        }
    }

    public void testDecaying() {
        final Locale dummyLocale = getDummyLocale("decaying");
        final UserHistoryDictionary dict = PersonalizationHelper.getUserHistoryDictionary(
                getContext(), dummyLocale);
        final int numberOfWords = 5000;
        final Random random = new Random(123456);
        resetCurrentTimeForTestMode();
        clearHistory(dict);
        final List<String> words = generateWords(numberOfWords, random);
        dict.waitAllTasksForTests();
        NgramContext ngramContext = NgramContext.EMPTY_PREV_WORDS_INFO;
        for (final String word : words) {
            UserHistoryDictionary.addToDictionary(dict, ngramContext, word, true, mCurrentTime,
                    DistracterFilter.EMPTY_DISTRACTER_FILTER);
            ngramContext = ngramContext.getNextNgramContext(new WordInfo(word));
            dict.waitAllTasksForTests();
            assertTrue(dict.isInDictionary(word));
        }
        forcePassingShortTime();
        dict.runGCIfRequired();
        dict.waitAllTasksForTests();
        for (final String word : words) {
            assertTrue(dict.isInDictionary(word));
        }
        forcePassingLongTime();
        dict.runGCIfRequired();
        dict.waitAllTasksForTests();
        for (final String word : words) {
            assertFalse(dict.isInDictionary(word));
        }
    }
}
