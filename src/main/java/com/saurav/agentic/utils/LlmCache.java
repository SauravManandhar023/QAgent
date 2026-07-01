package com.saurav.agentic.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.saurav.agentic.config.FrameworkConfig;
import com.saurav.agentic.constants.FrameworkConstants;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * LlmCache — persistent, disk-backed cache for LLM responses.
 *
 * Maps a SHA-256 hash of the prompt + model parameters to the LLM response text.
 * Supports online mode (read-through, write-back) and offline mode (cache-only).
 * Evicts oldest entries when the cache exceeds the max size.
 *
 * Thread-safe: uses a ReadWriteLock for concurrent access.
 * File format: JSON array of {key, response, timestamp} objects.
 */
public class LlmCache {

    private static final int DEFAULT_MAX_ENTRIES = 500;
    private static final String CACHE_FILE_NAME = "llm-cache.json";

    private final File cacheFile;
    private final int maxEntries;
    private final Gson gson;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // LinkedHashMap with insertion-order iteration for LRU eviction
    // Guarded by lock
    private final LinkedHashMap<String, CacheEntry> entries;

    private boolean enabled;
    private boolean offlineMode;

    /**
     * A single cache entry stored on disk.
     */
    private static class CacheEntry {
        String key;
        String response;
        long timestamp;

        CacheEntry() {}

        CacheEntry(String key, String response, long timestamp) {
            this.key = key;
            this.response = response;
            this.timestamp = timestamp;
        }
    }

    public LlmCache() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        FrameworkConfig config = FrameworkConfig.getInstance();
        this.enabled = config.getConfigReader().getBoolean("cache.enabled", true);
        this.offlineMode = config.getConfigReader().getBoolean("cache.offline.mode", false);

        String cacheDirPath = config.getConfigReader().get("cache.dir", "test-output/cache");
        File cacheDir = new File(cacheDirPath);
        cacheDir.mkdirs();
        this.cacheFile = new File(cacheDir, CACHE_FILE_NAME);

        this.maxEntries = DEFAULT_MAX_ENTRIES;
        this.entries = new LinkedHashMap<>(16, 0.75f, false); // insertion-order

        loadFromDisk();
    }

    /**
     * Package-private constructor for testing — allows specifying cache directory
     * and max entries directly without reading config.
     */
    LlmCache(String cacheDirPath, int maxEntries) {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.maxEntries = maxEntries;
        this.enabled = true;
        this.offlineMode = false;

        File cacheDir = new File(cacheDirPath);
        cacheDir.mkdirs();
        this.cacheFile = new File(cacheDir, CACHE_FILE_NAME);

        this.entries = new LinkedHashMap<>(16, 0.75f, false);
        loadFromDisk();
    }

    // ── Public API ───────────────────────────────────────────────────────

    /**
     * Whether disk caching is enabled via config.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Whether we are in offline mode (no live LLM calls allowed).
     */
    public boolean isOfflineMode() {
        return offlineMode;
    }

    /**
     * Look up a cached response.
     * @return the cached response, or null if not found
     */
    public String get(String systemPrompt, String userPrompt,
                      String model, double temperature, int maxTokens) {
        if (!enabled) return null;

        String key = hashKey(systemPrompt, userPrompt, model, temperature, maxTokens);
        lock.readLock().lock();
        try {
            CacheEntry entry = entries.get(key);
            if (entry != null) {
                System.out.println(FrameworkConstants.LOG_INFO +
                        " Cache HIT for key=" + key.substring(0, Math.min(12, key.length())) + "...");
                return entry.response;
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Store a response in the cache.
     */
    public void put(String systemPrompt, String userPrompt,
                    String model, double temperature, int maxTokens,
                    String response) {
        if (!enabled) return;

        String key = hashKey(systemPrompt, userPrompt, model, temperature, maxTokens);
        lock.writeLock().lock();
        try {
            // If key already exists, update in place (keeps insertion order)
            if (entries.containsKey(key)) {
                entries.put(key, new CacheEntry(key, response, System.currentTimeMillis()));
            } else {
                // Evict oldest if at capacity
                if (entries.size() >= maxEntries) {
                    Iterator<String> it = entries.keySet().iterator();
                    if (it.hasNext()) {
                        it.next();
                        it.remove(); // removes oldest (insertion-order)
                    }
                }
                entries.put(key, new CacheEntry(key, response, System.currentTimeMillis()));
            }
            saveToDisk();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Get number of entries currently in the cache.
     */
    public int size() {
        lock.readLock().lock();
        try {
            return entries.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Clear all cached entries and delete the cache file.
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            entries.clear();
            if (cacheFile.exists()) {
                cacheFile.delete();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ── Disk I/O ─────────────────────────────────────────────────────────

    /**
     * Load cache entries from disk. If the file is missing or corrupt,
     * start with an empty cache.
     */
    private void loadFromDisk() {
        if (!cacheFile.exists()) {
            System.out.println(FrameworkConstants.LOG_INFO +
                    " LLM cache file not found at: " + cacheFile.getAbsolutePath() +
                    " — starting with empty cache");
            return;
        }

        lock.writeLock().lock();
        try {
            Type listType = new TypeToken<List<CacheEntry>>() {}.getType();
            try (Reader reader = new FileReader(cacheFile)) {
                List<CacheEntry> loaded = gson.fromJson(reader, listType);
                if (loaded != null) {
                    // Sort by timestamp ascending so that oldest entries are evicted first
                    loaded.sort(Comparator.comparingLong(e -> e.timestamp));
                    for (CacheEntry entry : loaded) {
                        if (entries.size() >= maxEntries) break;
                        entries.put(entry.key, entry);
                    }
                    System.out.println(FrameworkConstants.LOG_INFO +
                            " Loaded " + entries.size() + " LLM cache entries from: " +
                            cacheFile.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            // Corrupt cache file — start fresh
            System.out.println(FrameworkConstants.LOG_WARNING +
                    " Cache file corrupt (" + e.getMessage() +
                    "), starting with empty cache");
            entries.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Persist all entries to disk as a JSON array.
     */
    private void saveToDisk() {
        try {
            cacheFile.getParentFile().mkdirs();
            List<CacheEntry> list = new ArrayList<>(entries.values());
            try (Writer writer = new FileWriter(cacheFile)) {
                gson.toJson(list, writer);
            }
        } catch (IOException e) {
            System.err.println(FrameworkConstants.LOG_WARNING +
                    " Failed to write LLM cache: " + e.getMessage());
        }
    }

    // ── Key Hashing ──────────────────────────────────────────────────────

    /**
     * Create a deterministic SHA-256 hash of the prompt + model parameters.
     */
    static String hashKey(String systemPrompt, String userPrompt,
                          String model, double temperature, int maxTokens) {
        String raw = systemPrompt + "|||" + userPrompt + "|||"
                + model + "|||" + temperature + "|||" + maxTokens;
        return sha256(raw);
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed available — fallback to identity hash
            return Integer.toHexString(input.hashCode());
        }
    }
}
