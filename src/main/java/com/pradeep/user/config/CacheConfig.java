package com.pradeep.user.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pradeep.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis Cache Configuration
 * 
 * Configures distributed caching using Redis for user lookup operations.
 * 
 * Cache Behavior:
 * - TTL (Time To Live): Configurable via cache.redis.time-to-live (default: 1 hour)
 * - No Maximum Entry Count: Unlike in-memory caches, Redis doesn't enforce a maximum
 *   entry count at the application level. Memory limits must be configured at the Redis
 *   server level using maxmemory and maxmemory-policy settings.
 * 
 * Redis Server Configuration Required:
 * - Set maxmemory (e.g., 256mb) to limit Redis memory usage
 * - Set maxmemory-policy (e.g., allkeys-lru) to define eviction behavior
 * - See README.md for detailed Redis server configuration instructions
 * 
 * Cache Keys:
 * - Email lookups: "email:" + email
 * - Google ID lookups: "googleId:" + googleId
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Cache TTL in milliseconds (default: 1 hour)
     * Configurable via cache.redis.time-to-live property
     */
    @Value("${cache.redis.time-to-live:3600000}")
    private long cacheTtl;

    /**
     * Whether to cache null values
     * Configurable via cache.redis.cache-null-values property
     */
    @Value("${cache.redis.cache-null-values:false}")
    private boolean cacheNullValues;

    /**
     * Configures Redis Cache Manager
     * 
     * Note: This configuration does not set a maximum entry count. Redis memory limits
     * and eviction policies must be configured at the Redis server level.
     * 
     * @param connectionFactory Redis connection factory
     * @return Configured CacheManager instance
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // This cache stores User records only. A typed serializer avoids accepting
        // attacker-controlled polymorphic type metadata from Redis values.
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        Jackson2JsonRedisSerializer<User> jsonSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, User.class);

        // Configure String serializer for keys
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // Redis cache configuration
        // TTL-based expiration: entries expire after cacheTtl milliseconds
        // Memory-based eviction: handled by Redis server maxmemory-policy
        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMillis(cacheTtl))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

        if (!cacheNullValues) {
            cacheConfig = cacheConfig.disableCachingNullValues();
        }

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(cacheConfig)
            .transactionAware()
            .build();
    }
}
