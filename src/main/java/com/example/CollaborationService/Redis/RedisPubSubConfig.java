///        package com.example.CollaborationService.Redis;
///
///
///        import org.springframework.context.annotation.Bean;
///        import org.springframework.context.annotation.Configuration;
///        import org.springframework.data.redis.connection.RedisConnectionFactory;
///        import org.springframework.data.redis.listener.PatternTopic;
///        import org.springframework.data.redis.listener.RedisMessageListenerContainer;
///
///        @Configuration
///        public class RedisPubSubConfig {
///
///            @Bean
///            public RedisMessageListenerContainer redisContainer(RedisConnectionFactory redisConnectionFactory) {
///                RedisMessageListenerContainer container = new RedisMessageListenerContainer();
///                container.setConnectionFactory(redisConnectionFactory);
///                container.setMessageListeners(redisUpdateSubcriber, new PatternTopic("doc:update:*"));
///                return container;
///            }
///        }
///