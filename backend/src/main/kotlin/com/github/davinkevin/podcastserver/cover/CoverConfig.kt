package com.github.davinkevin.podcastserver.cover

import com.github.davinkevin.podcastserver.config.ClockConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.function.server.router

@Configuration
@Import(CoverHandler::class)
class CoverRoutingConfig {

    @Bean
    fun coverRouter(cover: CoverHandler) = router {
        "/api/v1/covers".nest {
            DELETE("/", cover::deleteOldCovers)
        }
    }
}

@Configuration
@Import(CoverRepositoryV2::class, CoverService::class, ClockConfig::class, CoverRoutingConfig::class)
class CoverConfig
