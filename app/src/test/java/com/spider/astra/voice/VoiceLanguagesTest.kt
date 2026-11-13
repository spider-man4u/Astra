package com.spider.astra.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceLanguagesTest {
    @Test
    fun acceptsSupportedIndianLanguageTags() {
        assertEquals(VoiceLanguages.ENGLISH_INDIA, VoiceLanguages.normalize("en-IN"))
        assertEquals(VoiceLanguages.HINDI_INDIA, VoiceLanguages.normalize("hi-IN"))
    }

    @Test
    fun unknownTagsFallBackToEnglishIndia() {
        assertEquals(VoiceLanguages.ENGLISH_INDIA, VoiceLanguages.normalize("fr-FR"))
        assertEquals(VoiceLanguages.ENGLISH_INDIA, VoiceLanguages.normalize(null))
    }
}
