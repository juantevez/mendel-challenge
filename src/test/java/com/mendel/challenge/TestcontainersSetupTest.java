package com.mendel.challenge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Testcontainers Setup Integration Test")
class TestcontainersSetupTest extends BaseIntegrationTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    @DisplayName("Redis container should be running and accessible")
    void redisContainerShouldBeRunningAndAccessible() {
        // Given
        String key = "test-key";
        String value = "test-value";

        // When
        redisTemplate.opsForValue().set(key, value);
        String retrieved = redisTemplate.opsForValue().get(key);

        // Then
        assertThat(retrieved).isEqualTo(value);
    }

    @Test
    @DisplayName("Spring context should load successfully")
    void springContextShouldLoadSuccessfully() {
        assertThat(redisTemplate).isNotNull();
    }
}
