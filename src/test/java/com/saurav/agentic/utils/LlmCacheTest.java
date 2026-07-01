package com.saurav.agentic.utils;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;

import static org.testng.Assert.*;

/**
 * Unit tests for LlmCache — disk-backed LLM response cache.
 * Uses temp files to avoid polluting the real cache.
 */
public class LlmCacheTest {

    private static final String TEST_CACHE_DIR = "test-output/cache-test";
    private LlmCache cache;

    @BeforeMethod
    public void setUp() {
        cache = new LlmCache(TEST_CACHE_DIR, 10); // small max for eviction testing
        cache.clear();
    }

    @AfterMethod
    public void tearDown() {
        cache.clear();
        new File(TEST_CACHE_DIR, "llm-cache.json").delete();
        new File(TEST_CACHE_DIR).delete();
    }

    // ── Basic get/put ────────────────────────────────────────────────────

    @Test
    public void testPutAndGet() {
        cache.put("sys1", "user1", "model1", 0.3, 4096, "response1");
        String result = cache.get("sys1", "user1", "model1", 0.3, 4096);
        assertEquals(result, "response1");
    }

    @Test
    public void testGetReturnsNullForMissingKey() {
        assertNull(cache.get("sys", "user", "model", 0.3, 4096));
    }

    @Test
    public void testGetReturnsNullForDifferentPrompt() {
        cache.put("sys1", "user1", "model1", 0.3, 4096, "response1");
        assertNull(cache.get("sys2", "user2", "model2", 0.3, 4096));
    }

    @Test
    public void testPutOverwritesExisting() {
        cache.put("sys", "user", "model", 0.3, 4096, "original");
        cache.put("sys", "user", "model", 0.3, 4096, "updated");
        assertEquals(cache.get("sys", "user", "model", 0.3, 4096), "updated");
    }

    // ── Eviction ─────────────────────────────────────────────────────────

    @Test
    public void testEvictsOldestWhenFull() {
        for (int i = 0; i < 12; i++) {
            cache.put("sys", "user" + i, "model", 0.3, 4096, "resp" + i);
        }
        // Should have evicted the oldest entries
        assertEquals(cache.size(), 10);
        // Oldest entry (user0) should be gone
        assertNull(cache.get("sys", "user0", "model", 0.3, 4096));
        // Newest entry (user11) should exist
        assertNotNull(cache.get("sys", "user11", "model", 0.3, 4096));
    }

    // ── Persistence ──────────────────────────────────────────────────────

    @Test
    public void testPersistsAcrossInstances() {
        cache.put("sys", "user", "model", 0.3, 4096, "persistent-response");

        // Create a new cache instance pointing to the same directory
        LlmCache cache2 = new LlmCache(TEST_CACHE_DIR, 100);
        try {
            String result = cache2.get("sys", "user", "model", 0.3, 4096);
            assertEquals(result, "persistent-response");
        } finally {
            cache2.clear();
        }
    }

    @Test
    public void testEmptyCacheFile() {
        // The cache file won't exist until we put something
        LlmCache freshCache = new LlmCache(TEST_CACHE_DIR, 100);
        assertEquals(freshCache.size(), 0);
        assertNull(freshCache.get("a", "b", "c", 0.3, 4096));
    }

    // ── Clear ────────────────────────────────────────────────────────────

    @Test
    public void testClearRemovesAllEntries() {
        cache.put("sys", "user", "model", 0.3, 4096, "resp");
        assertEquals(cache.size(), 1);
        cache.clear();
        assertEquals(cache.size(), 0);
        assertNull(cache.get("sys", "user", "model", 0.3, 4096));
    }

    @Test
    public void testClearDeletesFile() {
        cache.put("sys", "user", "model", 0.3, 4096, "resp");
        cache.clear();
        File cacheFile = new File(TEST_CACHE_DIR, "llm-cache.json");
        assertFalse(cacheFile.exists());
    }

    // ── Key hashing ──────────────────────────────────────────────────────

    @Test
    public void testHashKeyDeterministic() {
        String hash1 = LlmCache.hashKey("sys", "user", "model", 0.3, 4096);
        String hash2 = LlmCache.hashKey("sys", "user", "model", 0.3, 4096);
        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashKeyDifferentForDifferentInputs() {
        String hash1 = LlmCache.hashKey("sys1", "user1", "model1", 0.3, 4096);
        String hash2 = LlmCache.hashKey("sys2", "user2", "model2", 0.3, 4096);
        assertNotEquals(hash1, hash2);
    }

    // ── Temperature sensitivity ──────────────────────────────────────────

    @Test
    public void testDifferentTemperatureGivesDifferentCacheKey() {
        cache.put("sys", "user", "model", 0.1, 4096, "low-temp");
        cache.put("sys", "user", "model", 0.7, 4096, "high-temp");

        assertEquals(cache.get("sys", "user", "model", 0.1, 4096), "low-temp");
        assertEquals(cache.get("sys", "user", "model", 0.7, 4096), "high-temp");
        assertNull(cache.get("sys", "user", "model", 0.5, 4096));
    }
}
