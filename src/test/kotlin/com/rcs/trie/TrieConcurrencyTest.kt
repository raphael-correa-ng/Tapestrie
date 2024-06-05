package com.rcs.trie

import org.assertj.core.api.Assertions.assertThat
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.Executors
import kotlin.test.Test

class TrieConcurrencyTest {

    @Test
    fun testConcurrency() {
        // Arrange
        val trie = Trie<Unit>()
        val executorService = Executors.newVirtualThreadPerTaskExecutor()
        val randomStrings = (0..10_000).map { getRandomString() }.distinct()

        // Act
        randomStrings
            .map { executorService.submit { trie.put(it, Unit) } }
            .forEach { it.get() }

        // Assert
        assertThat(trie.matchByPrefix("").size).isEqualTo(randomStrings.size)

        // we will add new strings as we remove the old ones
        val randomStringsAgain = (0..10_000).map { getRandomString() }.distinct()

        // run both add and remove operations concurrently
        val addFutures = randomStringsAgain
            .map { executorService.submit { trie.put(it, Unit) } }

        val removeFutures = randomStrings
            .map {
                executorService.submit {
                    assertThat(trie.getExactly(it)).isNotNull()
                    assertThat(trie.remove(it)).isNotNull()
                    assertThat(trie.getExactly(it)).isNull()
                }
            }

        // wait for all tasks
        addFutures.map { it.get() }
        removeFutures.map { it.get() }

        // assert that the Trie still contains only the new strings added
        val actualStringsRemaining = trie.iterator()
            .asSequence()
            .map { it.string }
            .toSet()
        assertThat(actualStringsRemaining)
            .containsExactlyInAnyOrderElementsOf(randomStringsAgain)
    }

    private fun getRandomString(): String {
        val array = ByteArray(20)
        Random().nextBytes(array)
        return String(array, StandardCharsets.UTF_8)
    }
}