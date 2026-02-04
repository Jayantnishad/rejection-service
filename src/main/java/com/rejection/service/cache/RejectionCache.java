package com.rejection.service.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.security.SecureRandom;

/**
 * In-memory cache for rejection reasons
 * Thread-safe implementation for high concurrency scenarios
 * 
 * Business Logic: Stores 100 pre-defined rejection reasons for random selection
 * Performance: Uses pre-allocated array for O(1) access time
 * Security: No sensitive data stored, only rejection messages
 */
@Component
public class RejectionCache {
    
    private static final Logger logger = LoggerFactory.getLogger(RejectionCache.class);
    
    // Pre-allocated array for memory efficiency and performance
    private final String[] rejectionReasons = new String[100];
    private volatile boolean initialized = false;
    private final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * Initialize rejection reasons cache on application startup
     * 
     * Logging: Comprehensive initialization tracking
     * Performance: One-time initialization with pre-allocated array
     * Thread Safety: Volatile flag ensures visibility across threads
     */
    @PostConstruct
    public void initializeReasons() {
        logger.info("Starting rejection reasons cache initialization...");
        
        try {
            loadRejectionReasons();
            initialized = true;
            logger.info("Successfully initialized {} rejection reasons in cache", rejectionReasons.length);
            logger.debug("Cache initialization completed with {} bytes estimated memory usage", 
                        estimateMemoryUsage());
            
        } catch (Exception e) {
            logger.error("Failed to initialize rejection reasons cache: {}", e.getMessage(), e);
            throw new IllegalStateException("Cache initialization failed", e);
        }
    }
    
    /**
     * Load all rejection reasons into the cache array
     * Separated for better maintainability and readability
     */
    private void loadRejectionReasons() {
        String[] reasons = {
            "My pet parrot doesn't approve of you",
            "I promised my Netflix account I'd stay loyal",
            "I'm secretly training to be a ninja",
            "Aliens told me you're not the chosen one",
            "I can't date anyone taller than my Wi-Fi router",
            "I'm married to my job… literally, we had a ceremony",
            "I only date people who can juggle flaming swords",
            "My horoscope said I should avoid you",
            "I'm saving myself for pizza",
            "I'm allergic to people born in your month",
            "I'm in a committed relationship with my bed",
            "I can't risk you finding out I'm Batman",
            "My therapist said I should only date imaginary friends",
            "I'm waiting for Hogwarts to send me a letter",
            "I swore an oath to never date until I beat Dark Souls",
            "I only date people who can moonwalk",
            "I'm too busy teaching my goldfish to swim",
            "My emotions are on vacation, so I'm unavailable",
            "I'm planning to travel back in time, relationships complicate that",
            "I might accidentally turn into a werewolf",
            "I'm still not over the ending of Game of Thrones",
            "We're too different — you like tea, I like coffee",
            "I'm focusing on my dream to become a professional napper",
            "I don't see this going anywhere… except maybe the circus",
            "My cat thinks it's a dog and needs therapy",
            "I'm already married to my PlayStation",
            "I only date people who can beat me at Mario Kart",
            "I'm allergic to commitment and peanuts",
            "I'm too busy binge-watching cooking shows I'll never try",
            "My imaginary friend gets jealous easily",
            "I'm saving myself for the next Marvel movie",
            "I'm secretly a vampire, and you're too sunny",
            "I only date people who can recite the alphabet backwards",
            "My dog said you're not cool enough",
            "I'm too busy trying to break a world record in napping",
            "I'm emotionally invested in my houseplants",
            "I'm waiting for Elon Musk to take me to Mars",
            "I'm allergic to people who don't like pineapple on pizza",
            "I'm too busy writing fanfiction about myself",
            "I'm in a complicated relationship with Wi-Fi",
            "I'm saving myself for tacos",
            "I'm too busy trying to teach my cat algebra",
            "I'm emotionally unavailable because my emotions are stuck in traffic",
            "I'm still recovering from losing in Uno",
            "I'm too busy practicing my evil laugh",
            "I'm waiting for my Hogwarts owl, can't commit until then",
            "I'm allergic to people who don't laugh at dad jokes",
            "I'm too busy building a pillow fort empire",
            "I'm emotionally drained from watching sad dog movies",
            "I'm saving myself for dessert",
            "I'm too busy trying to invent a new color",
            "I'm emotionally unavailable because my heart is on airplane mode",
            "I'm still recovering from losing my favorite pen",
            "I'm too busy training for the Olympics in procrastination",
            "I'm waiting for my spirit animal to approve",
            "I'm allergic to people who don't like memes",
            "I'm too busy trying to teach my fish to dance",
            "I'm emotionally unavailable because my feelings are on strike",
            "I'm still recovering from losing at Monopoly",
            "I'm too busy practicing my karaoke skills",
            "I'm saving myself for sushi",
            "I'm too busy trying to invent teleportation",
            "I'm emotionally unavailable because my heart is buffering",
            "I'm still recovering from losing my favorite sock",
            "I'm too busy training my hamster for a marathon",
            "I'm waiting for my horoscope to say yes",
            "I'm allergic to people who don't like chocolate",
            "I'm too busy trying to teach my dog to code",
            "I'm emotionally unavailable because my heart is on vacation",
            "I'm still recovering from losing at Scrabble",
            "I'm too busy practicing my moonwalk",
            "I'm saving myself for burgers",
            "I'm too busy trying to invent a new dance move",
            "I'm emotionally unavailable because my heart is in airplane mode",
            "I'm still recovering from losing my favorite hoodie",
            "I'm too busy training my turtle for a race",
            "I'm waiting for my fortune cookie to approve",
            "I'm allergic to people who don't like pizza",
            "I'm too busy trying to teach my parrot Shakespeare",
            "I'm emotionally unavailable because my heart is rebooting",
            "I'm still recovering from losing at chess",
            "I'm too busy practicing my juggling skills",
            "I'm saving myself for donuts",
            "I'm too busy trying to invent a new holiday",
            "I'm emotionally unavailable because my heart is in safe mode",
            "I'm still recovering from losing my favorite hat",
            "I'm too busy training my guinea pig for a talent show",
            "I'm waiting for my lucky number to appear",
            "I'm allergic to people who don't like ice cream",
            "I'm too busy trying to teach my cat yoga",
            "I'm emotionally unavailable because my heart is updating",
            "I'm still recovering from losing at poker",
            "I'm too busy practicing my breakdance",
            "I'm saving myself for pancakes",
            "I'm too busy trying to invent a new emoji",
            "I'm emotionally unavailable because my heart is charging",
            "I'm still recovering from losing my favorite book",
            "I'm too busy training my rabbit for a magic trick",
            "I'm waiting for my lucky star to shine",
            "I'm allergic to people who don't like fries"
        };
        
        System.arraycopy(reasons, 0, rejectionReasons, 0, reasons.length);
    }
    
    /**
     * Get random rejection reason from cache
     * 
     * Thread Safety: Uses ThreadLocalRandom for concurrent access
     * Performance: O(1) array access time
     * Logging: Debug level logging for request tracking
     * 
     * @return Random rejection reason string
     * @throws IllegalStateException if cache not initialized
     */
    public String getRandomReason() {
        if (!initialized) {
            logger.error("Attempted to access uninitialized rejection cache");
            throw new IllegalStateException("Rejection cache not initialized");
        }
        
        // Cryptographically secure random number generation
        int randomIndex = secureRandom.nextInt(rejectionReasons.length);
        String reason = rejectionReasons[randomIndex];
        
        logger.debug("Retrieved rejection reason at index {}: '{}'", randomIndex, 
                    reason.length() > 50 ? reason.substring(0, 50) + "..." : reason);
        
        return reason;
    }
    
    /**
     * Check if cache is properly initialized
     * 
     * @return true if cache is ready for use
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Get total number of cached rejection reasons
     * 
     * @return Cache size for monitoring and metrics
     */
    public int getCacheSize() {
        return rejectionReasons.length;
    }
    
    /**
     * Estimate memory usage for monitoring
     * 
     * Performance Monitoring: Helps track memory efficiency
     * 
     * @return Estimated memory usage in bytes
     */
    private long estimateMemoryUsage() {
        long totalBytes = 0;
        for (String reason : rejectionReasons) {
            if (reason != null) {
                // Prevent arithmetic overflow by checking bounds
                int length = reason.length();
                if (length > Integer.MAX_VALUE / 2) {
                    logger.warn("String length {} too large for memory calculation, using max safe value", length);
                    length = Integer.MAX_VALUE / 2;
                }
                
                // Rough estimate: 2 bytes per character + object overhead
                // Check for multiplication overflow before calculating
                if (length > (Long.MAX_VALUE - 24L) / 2L) {
                    logger.warn("String length {} would cause overflow in memory calculation, skipping", length);
                    continue;
                }
                long stringBytes = (long) length * 2L + 24L;
                
                // Check for overflow before adding using Math.addExact
                try {
                    totalBytes = Math.addExact(totalBytes, stringBytes);
                } catch (ArithmeticException e) {
                    logger.error("Memory usage calculation overflow detected at string length {}: {}", length, e.getMessage());
                    throw new IllegalStateException("Memory calculation overflow - cache size too large", e);
                }
            }
        }
        return totalBytes;
    }
}