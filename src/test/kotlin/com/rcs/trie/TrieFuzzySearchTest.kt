package com.rcs.trie

import org.assertj.core.api.Assertions.*
import kotlin.test.Test

class TrieFuzzySearchTest {

    @Test
    fun testDoesNotMatchEdgeCase() {
        // Arrange
        val trie = Trie<Unit>()
        trie.put("ionice", Unit)

        // Act
        val result = trie.matchBySubstringFuzzy("indices", 2, FuzzySubstringMatchingStrategy.LIBERAL)

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun testMatchBySubstringFuzzyAnchorToPrefix() {
        // Arrange
        val trie = Trie<Unit>()
        trie.put("index", Unit)
        trie.put("ondex", Unit) // will match because it has 1 wrong first letter
        trie.put("oldex", Unit) // will match because it has 2 wrong first letter
        trie.put("omtex", Unit) // will match because it has 2 wrong first letter
        trie.put("lalala index", Unit)
        trie.put("lalala ondex", Unit) // will match because it has 1 wrong first letter
        trie.put("lalala oldex", Unit) // will match because it has 2 wrong first letter
        trie.put("lalala omtex", Unit) // will not match because it has 3 wrong first letter

        // Act
        val result = trie.matchBySubstringFuzzy("index", 2, FuzzySubstringMatchingStrategy.ANCHOR_TO_PREFIX)

        // Assert
        assertThat(result).containsExactly(
            TrieSearchResult("index", Unit, "index", "index", 5, 0, 0, true, true),
            TrieSearchResult("lalala index", Unit, "index", "index", 5, 0, 0, false, true),
            TrieSearchResult("ondex", Unit, "ndex", "ondex", 4, 1, 1, false, false),
            TrieSearchResult("lalala ondex", Unit, "ndex", "ondex", 4, 1, 1, false, false),
            TrieSearchResult("oldex", Unit, "dex", "oldex", 3, 2, 2, false, false),
            TrieSearchResult("lalala oldex", Unit, "dex", "oldex", 3, 2, 2, false, false)
        )
    }

    @Test
    fun testMatchBySubstringFuzzyMatchPrefix() {
        // Arrange
        val trie = Trie<Unit>()
        trie.put("lalala index", Unit)
        trie.put("lalala indix", Unit)
        trie.put("lalala ondex", Unit) // will not match because it doesn't match first letter in keyword

        // Act
        val result = trie.matchBySubstringFuzzy("index", 1, FuzzySubstringMatchingStrategy.MATCH_PREFIX)

        // Assert
        assertThat(result).containsExactly(
            TrieSearchResult("lalala index", Unit, "index", "index", 5, 0, 0, false, true),
            TrieSearchResult("lalala indix", Unit, "indix", "indix", 4, 1, 0, false, false)
        )
    }

    @Test
    fun testMatchBySubstringFuzzyCommonCase() {
        // Arrange
        val trie = Trie<Unit>()
        trie.put("indexes", Unit)
        trie.put("indices", Unit)

        // Act
        val resultIndexes = trie.matchBySubstringFuzzy("indexes", 2, FuzzySubstringMatchingStrategy.LIBERAL)
        val resultIndices = trie.matchBySubstringFuzzy("indices", 2, FuzzySubstringMatchingStrategy.LIBERAL)

        // Assert
        assertThat(resultIndexes).containsExactly(
            TrieSearchResult("indexes", Unit, "indexes", "indexes", 7, 0, 0, true, true),
            TrieSearchResult("indices", Unit, "indices", "indices", 5, 2, 0, false, false)
        )
        assertThat(resultIndices).containsExactly(
            TrieSearchResult("indices", Unit, "indices", "indices", 7, 0, 0, true, true),
            TrieSearchResult("indexes", Unit, "indexes", "indexes", 5, 2, 0, false, false)
        )
    }

    @Test
    fun testMatchBySubstringFuzzyWithSort() {
        // Arrange
        val trie = Trie<Unit>()
        trie.put("man", Unit)
        trie.put("manu", Unit)
        trie.put("many", Unit)
        trie.put("manual", Unit)
        trie.put("emanuel", Unit)
        trie.put("lemanuel", Unit)
        trie.put("lemanuell", Unit)
        trie.put("manually", Unit)
        trie.put("manuals", Unit)
        trie.put("linux manual", Unit)

        // Act
        val result = trie.matchBySubstringFuzzy("manual", 3, FuzzySubstringMatchingStrategy.LIBERAL)

        // Assert
        assertThat(result).containsExactly(
            // matches whole sequence is highest ranking
            TrieSearchResult("manual", Unit, "manual", "manual", 6, 0, 0, true, true),
            // matches a whole word is second highest ranking
            TrieSearchResult("linux manual", Unit, "manual", "manual", 6, 0, 0, false, true),
            // matches the highest possible number of characters, but it's neither the whole sequence nor a whole word
            TrieSearchResult("manuals", Unit, "manual", "manuals", 6, 0, 0, false, false),
            // same as above, but the string is longer, so is ranked lower
            TrieSearchResult("manually", Unit, "manual", "manually", 6, 0, 0, false, false),
            // partial match, with two errors
            TrieSearchResult("manu", Unit, "manu", "manu", 4, 2, 0, false, false),
            // partial match, with three errors
            TrieSearchResult("man", Unit, "man", "man", 3, 3, 0, false, false),
            // partial match, with three errors but a longer string
            TrieSearchResult("many", Unit, "man", "many", 3, 3, 0, false, false),
            // prefix match = 1
            TrieSearchResult("emanuel", Unit, "manuel", "emanuel", 5, 1, 1, false, false),
            // prefix match = 2
            TrieSearchResult("lemanuel", Unit, "manuel", "lemanuel", 5, 1, 2, false, false),
            // prefix match = 2 but word is longer
            TrieSearchResult("lemanuell", Unit, "manuel", "lemanuell", 5, 1, 2, false, false)
        )
    }

    @Test
    fun testMatchBySubstringFuzzy() {
        // Arrange
        val trie = Trie<Unit>()
        trie.put("goggle", Unit)
        trie.put("google", Unit)
        trie.put("googly", Unit)
        trie.put("giegly", Unit) // will not match any
        trie.put("gogle", Unit)  // missing letter
        trie.put("blah google blah", Unit) // good match with chars before and after

        // Act
        val resultOne = trie.matchBySubstringFuzzy("goggle", 1, FuzzySubstringMatchingStrategy.LIBERAL)
        val resultTwo = trie.matchBySubstringFuzzy("goggle", 2, FuzzySubstringMatchingStrategy.LIBERAL)

        // Assert
        assertThat(resultOne).containsExactly(
            TrieSearchResult("goggle", Unit, "goggle", "goggle", 6, 0, 0, true, true),
            TrieSearchResult("gogle", Unit, "gogle", "gogle", 5, 1, 0, false, false),
            TrieSearchResult("google", Unit, "google", "google", 5, 1, 0, false, false),
            TrieSearchResult("blah google blah", Unit, "google", "google", 5, 1, 0, false, false)
        )
        assertThat(resultTwo).containsExactly(
            TrieSearchResult("goggle", Unit, "goggle", "goggle", 6, 0, 0, true, true),
            TrieSearchResult("gogle", Unit, "gogle", "gogle", 5, 1, 0, false, false),
            TrieSearchResult("google", Unit, "google", "google", 5, 1, 0, false, false),
            TrieSearchResult("blah google blah", Unit, "google", "google", 5, 1, 0, false, false),
            TrieSearchResult("googly", Unit, "googly", "googly", 4, 2, 0, false, false),
        )
    }

    @Test
    fun testFuzzySubstringSearchLiberal() {
        // Arrange
        val trie = Trie<Unit>()
        trie.put("this is raphael", Unit)
        trie.put("this is raphaël", Unit)
        trie.put("this is rafael", Unit)
        trie.put("this is rafaela", Unit)
        trie.put("this is raffaello", Unit)
        trie.put("this is raffaella", Unit)

        // Act
        val r1 = trie.matchBySubstringFuzzy("raphael", 2, FuzzySubstringMatchingStrategy.LIBERAL)
        val r2 = trie.matchBySubstringFuzzy("rafael", 2, FuzzySubstringMatchingStrategy.LIBERAL)
        val r3 = trie.matchBySubstringFuzzy("raffaello", 2, FuzzySubstringMatchingStrategy.LIBERAL)
        val r4 = trie.matchBySubstringFuzzy("raffaello", 3, FuzzySubstringMatchingStrategy.LIBERAL)
        val r5 = trie.matchBySubstringFuzzy("raffaello", 4, FuzzySubstringMatchingStrategy.LIBERAL)

        // Assert
        assertThat(r1).containsExactly(
            TrieSearchResult("this is raphael", Unit, "raphael", "raphael", 7, 0, 0, false, true),
            TrieSearchResult("this is raphaël", Unit, "raphaël", "raphaël", 6, 1, 0, false, false),
            TrieSearchResult("this is rafael", Unit, "rafael", "rafael", 5, 2, 0, false, false),
            TrieSearchResult("this is rafaela", Unit, "rafael", "rafaela", 5, 2, 0, false, false),
            TrieSearchResult("this is raffaello", Unit, "raffael", "raffaello", 5, 2, 0, false, false),
            TrieSearchResult("this is raffaella", Unit, "raffael", "raffaella", 5, 2, 0, false, false))
        assertThat(r2).containsExactly(
            TrieSearchResult("this is rafael", Unit, "rafael", "rafael", 6, 0, 0, false, true),
            TrieSearchResult("this is rafaela", Unit, "rafael", "rafaela", 6, 0, 0, false, false),
            TrieSearchResult("this is raffaello", Unit, "raffael", "raffaello", 6, 1, 0, false, false),
            TrieSearchResult("this is raffaella", Unit, "raffael", "raffaella", 6, 1, 0, false, false),
            TrieSearchResult("this is raphael", Unit, "raphael", "raphael", 5, 2, 0, false, false))
        assertThat(r3).containsExactly(
            TrieSearchResult("this is raffaello", Unit, "raffaello", "raffaello", 9, 0, 0, false, true),
            TrieSearchResult("this is raffaella", Unit, "raffaella", "raffaella", 8, 1, 0, false, false))
        assertThat(r4).containsExactly(
            TrieSearchResult("this is raffaello", Unit, "raffaello", "raffaello", 9, 0, 0, false, true),
            TrieSearchResult("this is raffaella", Unit, "raffaella", "raffaella", 8, 1, 0, false, false),
            TrieSearchResult("this is rafael", Unit, "rafael", "rafael", 6, 3, 0, false, false),
            TrieSearchResult("this is rafaela", Unit, "rafael", "rafaela", 6, 3, 0, false, false))
        assertThat(r5).containsExactly(
            TrieSearchResult("this is raffaello", Unit, "raffaello", "raffaello", 9, 0, 0, false, true),
            TrieSearchResult("this is raffaella", Unit, "raffaella", "raffaella", 8, 1, 0, false, false),
            TrieSearchResult("this is rafael", Unit, "rafael", "rafael", 6, 3, 0, false, false),
            TrieSearchResult("this is rafaela", Unit, "rafael", "rafaela", 6, 3, 0, false, false),
            TrieSearchResult("this is raphael", Unit, "raphael", "raphael", 5, 4, 0, false, false))
    }

    @Test
    fun testMatchesLongWord() {
        // Arrange
        val trie = Trie<Unit>()
        trie.put("indistinguishable", Unit)

        // Act
        val result = trie.matchBySubstringFuzzy("indices", 2, FuzzySubstringMatchingStrategy.MATCH_PREFIX)

        // Assert
        assertThat(result).containsExactly(
            TrieSearchResult("indistinguishable", Unit, "indis", "indistinguishable", 5, 2, 0, false, false)
        )
    }

    @Test
    fun testMatchBySubstringFuzzyErrorsInBeginning() {
        // Arrange
        val trie = Trie<Unit>()
        trie.put("lala 000x23456789000 hehe", Unit)
        trie.put("lala 000xx3456789000 hehe", Unit)
        trie.put("lala 000xxx456789000 hehe", Unit)

        // Act
        val resultA = trie.matchBySubstringFuzzy("123456789", 0, FuzzySubstringMatchingStrategy.LIBERAL)
        val resultB = trie.matchBySubstringFuzzy("123456789", 1, FuzzySubstringMatchingStrategy.LIBERAL)
        val resultC = trie.matchBySubstringFuzzy("123456789", 2, FuzzySubstringMatchingStrategy.LIBERAL)

        // Assert
        assertThat(resultA).isEmpty()

        assertThat(resultB).containsExactly(
            TrieSearchResult("lala 000x23456789000 hehe", Unit, "23456789", "000x23456789000", 8, 1, 4, false, false)
        )

        assertThat(resultC).containsExactly(
            TrieSearchResult("lala 000x23456789000 hehe", Unit, "23456789", "000x23456789000", 8, 1, 4, false, false),
            TrieSearchResult("lala 000xx3456789000 hehe", Unit, "3456789", "000xx3456789000", 7, 2, 5, false, false)
        )
    }
}