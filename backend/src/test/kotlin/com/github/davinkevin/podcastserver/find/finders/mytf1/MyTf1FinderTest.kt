package com.github.davinkevin.podcastserver.find.finders.mytf1

import com.github.davinkevin.podcastserver.MockServer
import com.github.davinkevin.podcastserver.config.WebClientConfig
import com.github.davinkevin.podcastserver.fileAsString
import com.github.davinkevin.podcastserver.find.FindCoverInformation
import com.github.davinkevin.podcastserver.find.FindPodcastInformation
import com.github.davinkevin.podcastserver.remapToMockServer
import com.github.davinkevin.podcastserver.service.image.CoverInformation
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.client.WebClient
import reactor.kotlin.core.publisher.toMono
import reactor.test.StepVerifier
import java.net.URI
import com.github.davinkevin.podcastserver.service.image.ImageServiceV2 as ImageService

/**
 * Created by kevin on 12/01/2020
 */
@ExtendWith(SpringExtension::class)
class MyTf1FinderTest(
        @Autowired private val finder: MyTf1Finder
) {

    @MockBean lateinit var image: ImageService

    @TestConfiguration
    @Import(MyTf1FinderConfig::class, WebClientAutoConfiguration::class, JacksonAutoConfiguration::class, WebClientConfig::class)
    class LocalTestConfiguration {
        @Bean fun webClientCustomization() = WebClientCustomizer { it.filter(remapToMockServer("www.tf1.fr")) }
    }

    @Nested
    @ExtendWith(MockServer::class)
    @DisplayName("should find")
    inner class ShouldFind {

        @Test
        fun `information about tf1 podcast with its url`(backend: WireMockServer) {
            /* Given */
            val url = "https://www.tf1.fr/tmc/quotidien-avec-yann-barthes"
            val coverUrl = URI("https://photos.tf1.fr/1200/0/vignette-portrait-quotidien-2-aa530a-0@1x.png")

            whenever(image.fetchCoverInformation(coverUrl)).thenReturn(CoverInformation(
                    url = coverUrl,
                    height = 123,
                    width = 456
            ).toMono())

            backend.stubFor(get("/tmc/quotidien-avec-yann-barthes").willReturn(
                    ok(fileAsString("/remote/podcast/tf1replay/quotidien.root.html"))
            ))

            /* When */
            StepVerifier.create(finder.findInformation(url))
                    /* Then */
                    .expectSubscription()
                    .expectNext(FindPodcastInformation(
                            title = "Quotidien avec Yann Barthès - TMC | MYTF1",
                            description = "Yann Barthès est désormais sur TMC et TF1",
                            url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes"),
                            cover = FindCoverInformation(
                                    height = 123,
                                    width = 456,
                                    url = URI("https://photos.tf1.fr/1200/0/vignette-portrait-quotidien-2-aa530a-0@1x.png")
                            ),
                            type= "TF1Replay"
                    ))
                    .verifyComplete()
        }

        @Nested
        @DisplayName("information with")
        inner class InformationWithCover {

            @Test
            fun `no cover url`(backend: WireMockServer) {
                /* Given */
                val url = "https://www.tf1.fr/tmc/quotidien-avec-yann-barthes"

                backend.stubFor(get("/tmc/quotidien-avec-yann-barthes").willReturn(
                        ok(fileAsString("/remote/podcast/tf1replay/quotidien.root-without-picture.html"))
                ))

                /* When */
                StepVerifier.create(finder.findInformation(url))
                        /* Then */
                        .expectSubscription()
                        .expectNext(FindPodcastInformation(
                                title = "Quotidien avec Yann Barthès - TMC | MYTF1",
                                description = "Yann Barthès est désormais sur TMC et TF1",
                                url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes"),
                                cover = null,
                                type= "TF1Replay"
                        ))
                        .verifyComplete()
            }

            @Test
            fun `empty url for cover`(backend: WireMockServer) {
                /* Given */
                val url = "https://www.tf1.fr/tmc/quotidien-avec-yann-barthes"

                backend.stubFor(get("/tmc/quotidien-avec-yann-barthes").willReturn(
                        ok(fileAsString("/remote/podcast/tf1replay/quotidien.root-without-picture-url.html"))
                ))

                /* When */
                StepVerifier.create(finder.findInformation(url))
                        /* Then */
                        .expectSubscription()
                        .expectNext(FindPodcastInformation(
                                title = "Quotidien avec Yann Barthès - TMC | MYTF1",
                                description = "Yann Barthès est désormais sur TMC et TF1",
                                url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes"),
                                cover = null,
                                type= "TF1Replay"
                        ))
                        .verifyComplete()
            }

            @Test
            fun `relative url for cover`(backend: WireMockServer) {
                /* Given */
                val url = "https://www.tf1.fr/tmc/quotidien-avec-yann-barthes"
                val coverUrl = URI("https://photos.tf1.fr/1200/0/vignette-portrait-quotidien-2-aa530a-0@1x.png")

                whenever(image.fetchCoverInformation(coverUrl)).thenReturn(CoverInformation(
                        url = coverUrl,
                        height = 123,
                        width = 456
                ).toMono())

                backend.stubFor(get("/tmc/quotidien-avec-yann-barthes").willReturn(
                        ok(fileAsString("/remote/podcast/tf1replay/quotidien.root-with-picture-url-relative.html"))
                ))

                /* When */
                StepVerifier.create(finder.findInformation(url))
                        /* Then */
                        .expectSubscription()
                        .expectNext(FindPodcastInformation(
                                title = "Quotidien avec Yann Barthès - TMC | MYTF1",
                                description = "Yann Barthès est désormais sur TMC et TF1",
                                url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes"),
                                cover = FindCoverInformation(
                                        height = 123,
                                        width = 456,
                                        url = URI("https://photos.tf1.fr/1200/0/vignette-portrait-quotidien-2-aa530a-0@1x.png")
                                ),
                                type= "TF1Replay"
                        ))
                        .verifyComplete()
            }

        }




    }

    @Nested
    @DisplayName("on compatibility")
    inner class OnCompatibility {

        @ParameterizedTest(name = "with {0}")
        @ValueSource(strings = [
            "https://www.tf1.fr/tmc/quotidien-avec-yann-barthes",
            "https://www.tf1.fr/tf1/tout-est-permis-avec-arthur",
            "https://www.tf1.fr/tf1/les-douze-coups-de-midi",
            "https://www.tf1.fr/tf1/greys-anatomy"
        ])
        fun `should be compatible `(/* Given */ url: String) {
            /* When */
            val compatibility = finder.compatibility(url)
            /* Then */
            assertThat(compatibility).isEqualTo(1)
        }

        @ParameterizedTest(name = "with {0}")
        @ValueSource(strings = [
            "https://www.france2.tv/france-2/vu/",
            "https://www.foo.com/france-2/vu/",
            "https://www.mycanal.fr/france-2/vu/",
            "https://www.gulli.fr/france-2/vu/"
        ])
        fun `should not be compatible`(/* Given */ url: String) {
            /* When */
            val compatibility = finder.compatibility(url)
            /* Then */
            assertThat(compatibility).isEqualTo(Int.MAX_VALUE)
        }

        @Test
        fun `should not be compatible with null value`() {
            /* Given */
            /* When */
            val compatibility = finder.compatibility(null)
            /* Then */
            assertThat(compatibility).isEqualTo(Int.MAX_VALUE)
        }
    }

}
