/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.inputmethod.latin;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.android.inputmethod.latin.SuggestedWords.SuggestedWordInfo;

import java.util.ArrayList;
import java.util.Locale;

@SmallTest
public class SuggestedWordsTests extends AndroidTestCase {

    /**
     * Helper method to create a dummy {@link SuggestedWordInfo} with specifying
     * {@link SuggestedWordInfo#KIND_TYPED}.
     *
     * @param word the word to be used to create {@link SuggestedWordInfo}.
     * @return a new instance of {@link SuggestedWordInfo}.
     */
    private static SuggestedWordInfo createTypedWordInfo(final String word) {
        // Use 100 as the frequency because the numerical value does not matter as
        // long as it's > 1 and < INT_MAX.
        return new SuggestedWordInfo(word, 100 /* score */,
                SuggestedWordInfo.KIND_TYPED,
                null /* sourceDict */,
                SuggestedWordInfo.NOT_AN_INDEX /* indexOfTouchPointOfSecondWord */,
                1 /* autoCommitFirstWordConfidence */);
    }

    /**
     * Helper method to create a dummy {@link SuggestedWordInfo} with specifying
     * {@link SuggestedWordInfo#KIND_CORRECTION}.
     *
     * @param word the word to be used to create {@link SuggestedWordInfo}.
     * @return a new instance of {@link SuggestedWordInfo}.
     */
    private static SuggestedWordInfo createCorrectionWordInfo(final String word) {
        return new SuggestedWordInfo(word, 1 /* score */,
                SuggestedWordInfo.KIND_CORRECTION,
                null /* sourceDict */,
                SuggestedWordInfo.NOT_AN_INDEX /* indexOfTouchPointOfSecondWord */,
                SuggestedWordInfo.NOT_A_CONFIDENCE /* autoCommitFirstWordConfidence */);
    }

    // Helper for testGetTransformedWordInfo
    private static SuggestedWordInfo transformWordInfo(final String info,
            final int trailingSingleQuotesCount) {
        final SuggestedWordInfo suggestedWordInfo = createTypedWordInfo(info);
        final SuggestedWordInfo returnedWordInfo =
                Suggest.getTransformedSuggestedWordInfo(suggestedWordInfo,
                Locale.ENGLISH, false /* isAllUpperCase */, false /* isFirstCharCapitalized */,
                trailingSingleQuotesCount);
        assertEquals(suggestedWordInfo.mAutoCommitFirstWordConfidence,
                returnedWordInfo.mAutoCommitFirstWordConfidence);
        return returnedWordInfo;
    }

    public void testGetTransformedSuggestedWordInfo() {
        SuggestedWordInfo result = transformWordInfo("word", 0);
        assertEquals(result.mWord, "word");
        result = transformWordInfo("word", 1);
        assertEquals(result.mWord, "word'");
        result = transformWordInfo("word", 3);
        assertEquals(result.mWord, "word'''");
        result = transformWordInfo("didn't", 0);
        assertEquals(result.mWord, "didn't");
        result = transformWordInfo("didn't", 1);
        assertEquals(result.mWord, "didn't");
        result = transformWordInfo("didn't", 3);
        assertEquals(result.mWord, "didn't''");
    }

    public void testGetTypedWordInfoOrNull() {
        final String TYPED_WORD = "typed";
        final int NUMBER_OF_ADDED_SUGGESTIONS = 5;
        final ArrayList<SuggestedWordInfo> list = new ArrayList<>();
        list.add(createTypedWordInfo(TYPED_WORD));
        for (int i = 0; i < NUMBER_OF_ADDED_SUGGESTIONS; ++i) {
            list.add(createCorrectionWordInfo(Integer.toString(i)));
        }

        // Make sure getTypedWordInfoOrNull() returns non-null object.
        final SuggestedWords wordsWithTypedWord = new SuggestedWords(
                list, null /* rawSuggestions */,
                false /* typedWordValid */,
                false /* willAutoCorrect */,
                false /* isObsoleteSuggestions */,
                SuggestedWords.INPUT_STYLE_NONE);
        final SuggestedWordInfo typedWord = wordsWithTypedWord.getTypedWordInfoOrNull();
        assertNotNull(typedWord);
        assertEquals(TYPED_WORD, typedWord.mWord);

        // Make sure getTypedWordInfoOrNull() returns null when no typed word.
        list.remove(0);
        final SuggestedWords wordsWithoutTypedWord = new SuggestedWords(
                list, null /* rawSuggestions */,
                false /* typedWordValid */,
                false /* willAutoCorrect */,
                false /* isObsoleteSuggestions */,
                SuggestedWords.INPUT_STYLE_NONE);
        assertNull(wordsWithoutTypedWord.getTypedWordInfoOrNull());

        // Make sure getTypedWordInfoOrNull() returns null.
        assertNull(SuggestedWords.getEmptyInstance().getTypedWordInfoOrNull());

        final SuggestedWords emptySuggestedWords = new SuggestedWords(
                new ArrayList<SuggestedWordInfo>(), null /* rawSuggestions */,
                false /* typedWordValid */,
                false /* willAutoCorrect */,
                false /* isObsoleteSuggestions */,
                SuggestedWords.INPUT_STYLE_NONE);
        assertNull(emptySuggestedWords.getTypedWordInfoOrNull());

        assertNull(SuggestedWords.getEmptyInstance().getTypedWordInfoOrNull());
    }
}
