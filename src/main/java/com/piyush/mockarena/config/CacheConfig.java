package com.piyush.mockarena.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // Configure default cache
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(60, TimeUnit.MINUTES)
                .recordStats());

        // Configure specific caches - use Arrays.asList() for Collection
        cacheManager.setCacheNames(Arrays.asList(
                "userProfile",
                "userStats",
                "contests",
                "leaderboard",
                "problems",
                "problemStats",
                "languages",
                "tags"
        ));

        return cacheManager;
    }
}
