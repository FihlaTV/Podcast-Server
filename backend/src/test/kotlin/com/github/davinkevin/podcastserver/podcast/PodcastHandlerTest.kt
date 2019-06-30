package com.github.davinkevin.podcastserver.podcast

import com.github.davinkevin.podcastserver.IOUtils
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.extension.json.assertThatJson
import com.github.davinkevin.podcastserver.item.*
import com.github.davinkevin.podcastserver.service.FileService
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.github.davinkevin.podcastserver.tag.Tag
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import org.apache.commons.io.FilenameUtils
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import reactor.core.publisher.toMono
import java.net.URI
import java.nio.file.Path
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

/**
 * Created by kevin on 2019-02-16
 */
@WebFluxTest(controllers = [PodcastHandler::class])
@Import(PodcastRoutingConfig::class)
@ImportAutoConfiguration(ErrorWebFluxAutoConfiguration::class)
class PodcastHandlerTest {

    @Autowired lateinit var rest: WebTestClient
    @MockBean lateinit var podcastService: PodcastService
    @MockBean lateinit var itemService: ItemService
    @MockBean lateinit var p: PodcastServerParameters
    @MockBean lateinit var fileService: FileService

    val podcast = Podcast(
            id = UUID.fromString("dd16b2eb-657e-4064-b470-5b99397ce729"),
            title = "Podcast title",
            description = "desc",
            url = "https://foo.bar.com/app/file.rss",
            hasToBeDeleted = true,
            lastUpdate = OffsetDateTime.of(2019, 3, 31, 11, 21, 32, 45, ZoneOffset.ofHours(1)),
            type = "RSS",
            tags = setOf(Tag(UUID.fromString("f9d92927-1c4c-47a5-965d-efbb2d422f0c"), "Cinéma")),

            cover = CoverForPodcast(
                    id = UUID.fromString("1e275238-4cbe-4abb-bbca-95a0e4ebbeea"),
                    url = URI("https://external.domain.tld/cover.png"),
                    height = 200, width = 200
            )
    )

    @Test
    fun `should find by id`() {
        /* Given */
        whenever(podcastService.findById(podcast.id)).thenReturn(podcast.toMono())
        /* When */
        rest
                .get()
                .uri("https://localhost:8080/api/v1/podcasts/${podcast.id}")
                .exchange()
                /* Then */
                .expectStatus().isOk
                .expectBody()
                .assertThatJson {
                    isEqualTo("""{
                       "cover":{
                          "height":200,
                          "id":"1e275238-4cbe-4abb-bbca-95a0e4ebbeea",
                          "url":"/api/v1/podcasts/dd16b2eb-657e-4064-b470-5b99397ce729/cover.png",
                          "width":200
                       },
                       "hasToBeDeleted":true,
                       "id":"dd16b2eb-657e-4064-b470-5b99397ce729",
                       "lastUpdate":"2019-03-31T11:21:32.000000045+01:00",
                       "tags":[
                          {
                             "id":"f9d92927-1c4c-47a5-965d-efbb2d422f0c",
                             "name":"Cinéma"
                          }
                       ],
                       "title":"Podcast title",
                       "type":"RSS",
                       "url":"https://foo.bar.com/app/file.rss"
                    }""")
                }
    }

    @Nested
    @DisplayName("should find all")
    inner class ShouldFindAll {

        val podcast1 = Podcast(
                id = UUID.fromString("ad16b2eb-657e-4064-b470-5b99397ce729"),
                title = "Podcast first",
                description = "desc",
                url = "https://foo.bar.com/app/1.rss",
                hasToBeDeleted = true,
                lastUpdate = OffsetDateTime.of(2019, 3, 31, 11, 21, 32, 45, ZoneOffset.ofHours(1)),
                type = "RSS",
                tags = setOf(Tag(UUID.fromString("f9d92927-1c4c-47a5-965d-efbb2d422f0c"), "Cinéma")),

                cover = CoverForPodcast(
                        id = UUID.fromString("1e275238-4cbe-4abb-bbca-95a0e4ebbeea"),
                        url = URI("https://external.domain.tld/1.png"),
                        height = 200, width = 200
                )
        )
        val podcast2 = Podcast(
                id = UUID.fromString("bd16b2eb-657e-4064-b470-5b99397ce729"),
                title = "Podcast second",
                description = "desc",
                url = "https://foo.bar.com/app/2.rss",
                hasToBeDeleted = true,
                lastUpdate = OffsetDateTime.of(2019, 3, 31, 11, 21, 32, 45, ZoneOffset.ofHours(1)),
                type = "RSS",
                tags = setOf(Tag(UUID.fromString("f9d92927-1c4c-47a5-965d-efbb2d422f0c"), "Cinéma")),

                cover = CoverForPodcast(
                        id = UUID.fromString("1e275238-4cbe-4abb-bbca-95a0e4ebbeea"),
                        url = URI("https://external.domain.tld/2.png"),
                        height = 200, width = 200
                )
        )
        val podcast3 = Podcast(
                id = UUID.fromString("cd16b2eb-657e-4064-b470-5b99397ce729"),
                title = "Podcast third",
                description = "desc",
                url = "https://foo.bar.com/app/3.rss",
                hasToBeDeleted = true,
                lastUpdate = OffsetDateTime.of(2019, 3, 31, 11, 21, 32, 45, ZoneOffset.ofHours(1)),
                type = "RSS",
                tags = setOf(Tag(UUID.fromString("f9d92927-1c4c-47a5-965d-efbb2d422f0c"), "Cinéma")),

                cover = CoverForPodcast(
                        id = UUID.fromString("1e275238-4cbe-4abb-bbca-95a0e4ebbeea"),
                        url = URI("https://external.domain.tld/3.png"),
                        height = 200, width = 200
                )
        )

        @Test
        fun `with 3 podcasts`() {
            /* Given */
            val podcasts = listOf(podcast1, podcast2, podcast3)
            whenever(podcastService.findAll()).thenReturn(podcasts.toFlux())
            /* When */
            rest
                    .get()
                    .uri("https://localhost:8080/api/v1/podcasts")
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .assertThatJson {
                        isEqualTo("""{
                              "content": [
                                {
                                  "cover": { "height": 200, "id": "1e275238-4cbe-4abb-bbca-95a0e4ebbeea", "url": "/api/v1/podcasts/ad16b2eb-657e-4064-b470-5b99397ce729/cover.png", "width": 200 },
                                  "hasToBeDeleted": true,
                                  "id": "ad16b2eb-657e-4064-b470-5b99397ce729",
                                  "lastUpdate": "2019-03-31T11:21:32.000000045+01:00",
                                  "tags": [ { "id": "f9d92927-1c4c-47a5-965d-efbb2d422f0c", "name": "Cinéma" } ],
                                  "title": "Podcast first",
                                  "type": "RSS",
                                  "url": "https://foo.bar.com/app/1.rss"
                                },
                                {
                                  "cover": { "height": 200, "id": "1e275238-4cbe-4abb-bbca-95a0e4ebbeea", "url": "/api/v1/podcasts/bd16b2eb-657e-4064-b470-5b99397ce729/cover.png", "width": 200 },
                                  "hasToBeDeleted": true,
                                  "id": "bd16b2eb-657e-4064-b470-5b99397ce729",
                                  "lastUpdate": "2019-03-31T11:21:32.000000045+01:00",
                                  "tags": [ { "id": "f9d92927-1c4c-47a5-965d-efbb2d422f0c", "name": "Cinéma" } ],
                                  "title": "Podcast second",
                                  "type": "RSS",
                                  "url": "https://foo.bar.com/app/2.rss"
                                },
                                {
                                  "cover": { "height": 200, "id": "1e275238-4cbe-4abb-bbca-95a0e4ebbeea", "url": "/api/v1/podcasts/cd16b2eb-657e-4064-b470-5b99397ce729/cover.png", "width": 200 },
                                  "hasToBeDeleted": true,
                                  "id": "cd16b2eb-657e-4064-b470-5b99397ce729",
                                  "lastUpdate": "2019-03-31T11:21:32.000000045+01:00",
                                  "tags": [ { "id": "f9d92927-1c4c-47a5-965d-efbb2d422f0c", "name": "Cinéma" } ],
                                  "title": "Podcast third",
                                  "type": "RSS",
                                  "url": "https://foo.bar.com/app/3.rss"
                                }
                              ]
                            }
                            """)
                    }
        }

        @Test
        fun `with no podcast`() {
            /* Given */
            whenever(podcastService.findAll()).thenReturn(Flux.empty())
            /* When */
            rest
                    .get()
                    .uri("https://localhost:8080/api/v1/podcasts")
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .assertThatJson { isEqualTo("""{ "content": [] } """) }
        }


    }

    @Nested
    @DisplayName("should create")
    inner class ShouldCreate {

        val tags = listOf(
                Tag(UUID.fromString("47402ee0-0b7a-4ded-981a-79dce25b2b42"), "first_tag"),
                Tag(UUID.fromString("c2bb2e6a-32d3-47cd-995d-67e6a32ff87e"), "second_tag"),
                Tag(UUID.fromString("a0eb24c3-9b46-4ab6-9f2b-6474d8e2456c"), "third_tag")
        )

        val p = Podcast(
                id = UUID.fromString("dbb18cac-58bb-4d89-b9ec-afc9da00afc5"),
                title = "foo",
                description = "desc",
                url = "http://foo.bar.com/val.rss",
                hasToBeDeleted = true,
                lastUpdate = OffsetDateTime.of(2019, 4, 9, 11, 12, 13, 0, ZoneOffset.ofHours(2)),
                type = "RSS",
                tags = tags,
                cover = CoverForPodcast(UUID.fromString("d6d4033a-d499-4c09-8d3e-d74595ae0993"), URI("http://foo.bar.com/cover.png"), 1200, 600)
        )

        @Test
        fun `with standard information`() {
            /* Given */
            whenever(podcastService.save(any())).thenReturn(p.toMono())
            /* When */
            rest
                    .post()
                    .uri("/api/v1/podcasts")
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    .syncBody(""" {
                        "title": "foo",
                        "url": "http://foo.bar.com/val.rss",
                        "type": "RSS",
                        "tags": [
                            { "id": "47402ee0-0b7a-4ded-981a-79dce25b2b42", "name": "first_tag" },
                            { "id": "c2bb2e6a-32d3-47cd-995d-67e6a32ff87e", "name": "second_tag" },
                            { "id": null, "name": "unknown_tag" }
                        ],
                        "cover": {
                            "width": 1400, "height": 1200, "url": "http://foo.bar.com/cover.png"
                        }
                    }""")
                    /* Then */
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .assertThatJson {
                        isEqualTo(""" {
                              "cover": {
                                "height": 1200,
                                "id": "d6d4033a-d499-4c09-8d3e-d74595ae0993",
                                "url": "/api/v1/podcasts/dbb18cac-58bb-4d89-b9ec-afc9da00afc5/cover.png",
                                "width": 600
                              },
                              "hasToBeDeleted": true,
                              "id": "dbb18cac-58bb-4d89-b9ec-afc9da00afc5",
                              "lastUpdate": "2019-04-09T11:12:13+02:00",
                              "tags": [
                                {
                                  "id": "47402ee0-0b7a-4ded-981a-79dce25b2b42",
                                  "name": "first_tag"
                                },
                                {
                                  "id": "c2bb2e6a-32d3-47cd-995d-67e6a32ff87e",
                                  "name": "second_tag"
                                },
                                {
                                  "id": "a0eb24c3-9b46-4ab6-9f2b-6474d8e2456c",
                                  "name": "third_tag"
                                }
                              ],
                              "title": "foo",
                              "type": "RSS",
                              "url": "http://foo.bar.com/val.rss"
                            } """)
                    }
        }

    }

    @Nested
    @DisplayName("should update")
    inner class ShouldUpdate {


        val tags = listOf(
                Tag(UUID.fromString("47402ee0-0b7a-4ded-981a-79dce25b2b42"), "first_tag"),
                Tag(UUID.fromString("c2bb2e6a-32d3-47cd-995d-67e6a32ff87e"), "second_tag"),
                Tag(UUID.fromString("a0eb24c3-9b46-4ab6-9f2b-6474d8e2456c"), "third_tag")
        )

        val p = Podcast(
                id = UUID.fromString("dbb18cac-58bb-4d89-b9ec-afc9da00afc5"),
                title = "foo",
                description = "desc",
                url = "http://foo.bar.com/val.rss",
                hasToBeDeleted = true,
                lastUpdate = OffsetDateTime.of(2019, 4, 9, 11, 12, 13, 0, ZoneOffset.ofHours(2)),
                type = "RSS",
                tags = tags,
                cover = CoverForPodcast(UUID.fromString("d6d4033a-d499-4c09-8d3e-d74595ae0993"), URI("http://foo.bar.com/cover.png"), 1200, 600)
        )


        @Test
        fun `a podcast`() {
            /* Given */
            whenever(podcastService.update(any())).thenReturn(p.toMono())
            /* When */
            rest
                    .put()
                    .uri("/api/v1/podcasts/${p.id}")
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    .syncBody(""" {
                        "id": "dbb18cac-58bb-4d89-b9ec-afc9da00afc5",
                        "title": "foo",
                        "url": "http://foo.bar.com/val.rss",
                        "hasToBeDeleted": true,
                        "tags": [
                            { "id": "47402ee0-0b7a-4ded-981a-79dce25b2b42", "name": "first_tag" },
                            { "id": "c2bb2e6a-32d3-47cd-995d-67e6a32ff87e", "name": "second_tag" },
                            { "id": null, "name": "unknown_tag" }
                        ],
                        "cover": {
                            "width": 1400, "height": 1200, "url": "http://foo.bar.com/cover.png"
                        }
                    }""")
                    /* Then */
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .assertThatJson {
                        isEqualTo(""" {
                              "cover": {
                                "height": 1200,
                                "id": "d6d4033a-d499-4c09-8d3e-d74595ae0993",
                                "url": "/api/v1/podcasts/dbb18cac-58bb-4d89-b9ec-afc9da00afc5/cover.png",
                                "width": 600
                              },
                              "hasToBeDeleted": true,
                              "id": "dbb18cac-58bb-4d89-b9ec-afc9da00afc5",
                              "lastUpdate": "2019-04-09T11:12:13+02:00",
                              "tags": [
                                {
                                  "id": "47402ee0-0b7a-4ded-981a-79dce25b2b42",
                                  "name": "first_tag"
                                },
                                {
                                  "id": "c2bb2e6a-32d3-47cd-995d-67e6a32ff87e",
                                  "name": "second_tag"
                                },
                                {
                                  "id": "a0eb24c3-9b46-4ab6-9f2b-6474d8e2456c",
                                  "name": "third_tag"
                                }
                              ],
                              "title": "foo",
                              "type": "RSS",
                              "url": "http://foo.bar.com/val.rss"
                            } """)
                    }

        }

    }

    @Nested
    @DisplayName("should find cover")
    inner class ShouldFindCover {

        @Test
        fun `by redirecting to local file server if cover exists locally`() {
            /* Given */
            whenever(podcastService.findById(podcast.id)).thenReturn(podcast.toMono())
            whenever(fileService.coverExists(any<Podcast>())).then {
                FilenameUtils.getName(it.getArgument<Podcast>(0).cover.url.toASCIIString()).toMono()
            }
            whenever(p.coverDefaultName).thenReturn("cover")
            /* When */
            rest
                    .get()
                    .uri("https://localhost:8080/api/v1/podcasts/{id}/cover.png", podcast.id)
                    .exchange()
                    /* Then */
                    .expectStatus().isSeeOther
                    .expectHeader()
                    .valueEquals("Location", "https://localhost:8080/data/Podcast%20title/cover.png")
        }

        @Test
        fun `by redirecting to external file if cover does not exist locally`() {
            /* Given */
            whenever(podcastService.findById(podcast.id)).thenReturn(podcast.toMono())
            whenever(fileService.coverExists(any<Podcast>())).then { Mono.empty<Path>() }

            /* When */
            rest
                    .get()
                    .uri("https://localhost:8080/api/v1/podcasts/{id}/cover.png", podcast.id)
                    .exchange()
                    /* Then */
                    .expectStatus().isSeeOther
                    .expectHeader()
                    .valueEquals("Location", "https://external.domain.tld/cover.png")
        }

    }

    @Nested
    @DisplayName("should find stats")
    inner class ShouldFindStats {

        @Nested
        @DisplayName("globally")
        inner class Globally {

            @Nested
            @DisplayName("by creation date")
            inner class ByCreationDate {

                @Test
                fun `with some data`() {
                    /* Given */
                    val youtube = StatsPodcastType("YOUTUBE", setOf(
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-01-02"), 3),
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-01-12"), 2),
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-01-28"), 6))
                    )
                    val rss = StatsPodcastType("YOUTUBE", setOf(
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-02-02"), 5),
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-02-12"), 8),
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-02-28"), 1))
                    )
                    whenever(podcastService.findStatByTypeAndCreationDate(3)).thenReturn(Flux.just(youtube, rss))
                    /* When */
                    rest.get()
                            .uri { it.path("/api/v1/podcasts/stats/byCreationDate")
                                    .queryParam("numberOfMonths", 3)
                                    .build()
                            }
                            .exchange()
                            /* Then */
                            .expectStatus().isOk
                            .expectBody()
                            .assertThatJson { isEqualTo("""{
                                       "content":[ {
                                             "type":"YOUTUBE", "values":[
                                                { "date":"2019-01-02", "numberOfItems":3 },
                                                { "date":"2019-01-12", "numberOfItems":2 },
                                                { "date":"2019-01-28", "numberOfItems":6 }
                                             ]
                                          }, {
                                             "type":"YOUTUBE", "values":[
                                                { "date":"2019-02-02", "numberOfItems":5 },
                                                { "date":"2019-02-12", "numberOfItems":8 },
                                                { "date":"2019-02-28", "numberOfItems":1 }
                                             ]
                                          } ]
                                    }""") }
                }

                @Test
                fun `with no data`() {
                    /* Given */
                    whenever(podcastService.findStatByTypeAndCreationDate(3)).thenReturn(Flux.empty())
                    /* When */
                    rest.get()
                            .uri { it.path("/api/v1/podcasts/stats/byCreationDate")
                                    .queryParam("numberOfMonths", 3)
                                    .build()
                            }
                            .exchange()
                            /* Then */
                            .expectStatus().isOk
                            .expectBody()
                            .assertThatJson { isEqualTo(""" {"content":[]} """) }
                }
            }

            @Nested
            @DisplayName("by pubDate")
            inner class ByPubDate {

                @Test
                fun `with some data`() {
                    /* Given */
                    val youtube = StatsPodcastType("YOUTUBE", setOf(
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-01-02"), 3),
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-01-12"), 2),
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-01-28"), 6))
                    )
                    val rss = StatsPodcastType("YOUTUBE", setOf(
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-02-02"), 5),
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-02-12"), 8),
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-02-28"), 1))
                    )
                    whenever(podcastService.findStatByTypeAndPubDate(3)).thenReturn(Flux.just(youtube, rss))
                    /* When */
                    rest.get()
                            .uri { it.path("/api/v1/podcasts/stats/byPubDate")
                                    .queryParam("numberOfMonths", 3)
                                    .build()
                            }
                            .exchange()
                            /* Then */
                            .expectStatus().isOk
                            .expectBody()
                            .assertThatJson { isEqualTo("""{
                                       "content":[ {
                                             "type":"YOUTUBE", "values":[
                                                { "date":"2019-01-02", "numberOfItems":3 },
                                                { "date":"2019-01-12", "numberOfItems":2 },
                                                { "date":"2019-01-28", "numberOfItems":6 }
                                             ]
                                          }, {
                                             "type":"YOUTUBE", "values":[
                                                { "date":"2019-02-02", "numberOfItems":5 },
                                                { "date":"2019-02-12", "numberOfItems":8 },
                                                { "date":"2019-02-28", "numberOfItems":1 }
                                             ]
                                          } ]
                                    }""") }
                }

                @Test
                fun `with no data`() {
                    /* Given */
                    whenever(podcastService.findStatByTypeAndPubDate(3)).thenReturn(Flux.empty())
                    /* When */
                    rest.get()
                            .uri { it.path("/api/v1/podcasts/stats/byPubDate")
                                    .queryParam("numberOfMonths", 3)
                                    .build()
                            }
                            .exchange()
                            /* Then */
                            .expectStatus().isOk
                            .expectBody()
                            .assertThatJson { isEqualTo(""" {"content":[]} """) }
                }
            }
            @Nested
            @DisplayName("by download date")
            inner class ByDownloadDate {

                @Test
                fun `with some data`() {
                    /* Given */
                    val youtube = StatsPodcastType("YOUTUBE", setOf(
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-01-02"), 3),
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-01-12"), 2),
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-01-28"), 6))
                    )
                    val rss = StatsPodcastType("YOUTUBE", setOf(
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-02-02"), 5),
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-02-12"), 8),
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-02-28"), 1))
                    )
                    whenever(podcastService.findStatByTypeAndDownloadDate(3)).thenReturn(Flux.just(youtube, rss))
                    /* When */
                    rest.get()
                            .uri { it.path("/api/v1/podcasts/stats/byDownloadDate")
                                    .queryParam("numberOfMonths", 3)
                                    .build()
                            }
                            .exchange()
                            /* Then */
                            .expectStatus().isOk
                            .expectBody()
                            .assertThatJson { isEqualTo("""{
                                       "content":[ {
                                             "type":"YOUTUBE", "values":[
                                                { "date":"2019-01-02", "numberOfItems":3 },
                                                { "date":"2019-01-12", "numberOfItems":2 },
                                                { "date":"2019-01-28", "numberOfItems":6 }
                                             ]
                                          }, {
                                             "type":"YOUTUBE", "values":[
                                                { "date":"2019-02-02", "numberOfItems":5 },
                                                { "date":"2019-02-12", "numberOfItems":8 },
                                                { "date":"2019-02-28", "numberOfItems":1 }
                                             ]
                                          } ]
                                    }""") }
                }

                @Test
                fun `with no data`() {
                    /* Given */
                    whenever(podcastService.findStatByTypeAndDownloadDate(3)).thenReturn(Flux.empty())
                    /* When */
                    rest.get()
                            .uri { it.path("/api/v1/podcasts/stats/byDownloadDate")
                                    .queryParam("numberOfMonths", 3)
                                    .build()
                            }
                            .exchange()
                            /* Then */
                            .expectStatus().isOk
                            .expectBody()
                            .assertThatJson { isEqualTo(""" {"content":[]} """) }
                }
            }


        }

        @Nested
        @DisplayName("for a given podcast")
        inner class ForAGivenPodcast {

            @Nested
            @DisplayName("by pubDate")
            inner class ByPubDate {

                @Test
                fun `with some data`() {
                    /* Given */
                    val r = listOf(
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-01-02"), 3),
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-01-12"), 2),
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-01-28"), 6)
                    )
                    whenever(podcastService.findStatByPodcastIdAndPubDate(podcast.id, 3)).thenReturn(r.toFlux())
                    /* When */
                    rest.get()
                            .uri { it.path("/api/v1/podcasts/${podcast.id}/stats/byPubDate")
                                    .queryParam("numberOfMonths", 3)
                                    .build()
                            }
                            .exchange()
                            /* Then */
                            .expectStatus().isOk
                            .expectBody()
                            .assertThatJson { isEqualTo(""" [
                               { "date":"2019-01-02", "numberOfItems":3 },
                               { "date":"2019-01-12", "numberOfItems":2 },
                               { "date":"2019-01-28", "numberOfItems":6 }
                        ] """) }
                }

                @Test
                fun `with no data`() {
                    /* Given */
                    whenever(podcastService.findStatByPodcastIdAndPubDate(podcast.id, 3)).thenReturn(Flux.empty())
                    /* When */
                    rest.get()
                            .uri { it.path("/api/v1/podcasts/${podcast.id}/stats/byPubDate")
                                    .queryParam("numberOfMonths", 3)
                                    .build()
                            }
                            .exchange()
                            /* Then */
                            .expectStatus().isOk
                            .expectBody()
                            .assertThatJson { isArray.isEmpty() }
                }
            }

            @Nested
            @DisplayName("by downloadDate")
            inner class ByDownloadDate {

                @Test
                fun `with some data`() {
                    /* Given */
                    val r = listOf(
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-01-02"), 3),
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-01-12"), 2),
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-01-28"), 6)
                    )
                    whenever(podcastService.findStatByPodcastIdAndDownloadDate(podcast.id, 3)).thenReturn(r.toFlux())
                    /* When */
                    rest.get()
                            .uri { it.path("/api/v1/podcasts/${podcast.id}/stats/byDownloadDate")
                                    .queryParam("numberOfMonths", 3)
                                    .build()
                            }
                            .exchange()
                            /* Then */
                            .expectStatus().isOk
                            .expectBody()
                            .assertThatJson { isEqualTo(""" [
                               { "date":"2019-01-02", "numberOfItems":3 },
                               { "date":"2019-01-12", "numberOfItems":2 },
                               { "date":"2019-01-28", "numberOfItems":6 }
                        ] """) }
                }

                @Test
                fun `with no data`() {
                    /* Given */
                    whenever(podcastService.findStatByPodcastIdAndDownloadDate(podcast.id, 3)).thenReturn(Flux.empty())
                    /* When */
                    rest.get()
                            .uri { it.path("/api/v1/podcasts/${podcast.id}/stats/byDownloadDate")
                                    .queryParam("numberOfMonths", 3)
                                    .build()
                            }
                            .exchange()
                            /* Then */
                            .expectStatus().isOk
                            .expectBody()
                            .assertThatJson { isArray.isEmpty() }
                }
            }

            @Nested
            @DisplayName("by creationDate")
            inner class ByCreationDate {

                @Test
                fun `with some data`() {
                    /* Given */
                    val r = listOf(
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-01-02"), 3),
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-01-12"), 2),
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-01-28"), 6)
                    )
                    whenever(podcastService.findStatByPodcastIdAndCreationDate(podcast.id, 3)).thenReturn(r.toFlux())
                    /* When */
                    rest.get()
                            .uri { it.path("/api/v1/podcasts/${podcast.id}/stats/byCreationDate")
                                    .queryParam("numberOfMonths", 3)
                                    .build()
                            }
                            .exchange()
                            /* Then */
                            .expectStatus().isOk
                            .expectBody()
                            .assertThatJson { isEqualTo(""" [
                               { "date":"2019-01-02", "numberOfItems":3 },
                               { "date":"2019-01-12", "numberOfItems":2 },
                               { "date":"2019-01-28", "numberOfItems":6 }
                        ] """) }
                }

                @Test
                fun `with no data`() {
                    /* Given */
                    whenever(podcastService.findStatByPodcastIdAndCreationDate(podcast.id, 3)).thenReturn(Flux.empty())
                    /* When */
                    rest.get()
                            .uri { it.path("/api/v1/podcasts/${podcast.id}/stats/byCreationDate")
                                    .queryParam("numberOfMonths", 3)
                                    .build()
                            }
                            .exchange()
                            /* Then */
                            .expectStatus().isOk
                            .expectBody()
                            .assertThatJson { isArray.isEmpty() }
                }
            }

        }


    }

    @Nested
    @DisplayName("should generate opml")
    inner class ShouldGenerateOPML {

        val podcast1 = Podcast(
                id = UUID.fromString("ad16b2eb-657e-4064-b470-5b99397ce729"),
                title = "Podcast first",
                description = "desc",
                url = "https://foo.bar.com/app/1.rss",
                hasToBeDeleted = true,
                lastUpdate = OffsetDateTime.of(2019, 3, 31, 11, 21, 32, 45, ZoneOffset.ofHours(1)),
                type = "RSS",
                tags = setOf(Tag(UUID.fromString("f9d92927-1c4c-47a5-965d-efbb2d422f0c"), "Cinéma")),

                cover = CoverForPodcast(
                        id = UUID.fromString("1e275238-4cbe-4abb-bbca-95a0e4ebbeea"),
                        url = URI("https://external.domain.tld/1.png"),
                        height = 200, width = 200
                )
        )
        val podcast2 = Podcast(
                id = UUID.fromString("bd16b2eb-657e-4064-b470-5b99397ce729"),
                title = "Podcast second",
                description = "desc",
                url = "https://foo.bar.com/app/2.rss",
                hasToBeDeleted = true,
                lastUpdate = OffsetDateTime.of(2019, 3, 31, 11, 21, 32, 45, ZoneOffset.ofHours(1)),
                type = "RSS",
                tags = setOf(Tag(UUID.fromString("f9d92927-1c4c-47a5-965d-efbb2d422f0c"), "Cinéma")),

                cover = CoverForPodcast(
                        id = UUID.fromString("1e275238-4cbe-4abb-bbca-95a0e4ebbeea"),
                        url = URI("https://external.domain.tld/2.png"),
                        height = 200, width = 200
                )
        )
        val podcast3 = Podcast(
                id = UUID.fromString("cd16b2eb-657e-4064-b470-5b99397ce729"),
                title = "Podcast third",
                description = "desc",
                url = "https://foo.bar.com/app/3.rss",
                hasToBeDeleted = true,
                lastUpdate = OffsetDateTime.of(2019, 3, 31, 11, 21, 32, 45, ZoneOffset.ofHours(1)),
                type = "RSS",
                tags = setOf(Tag(UUID.fromString("f9d92927-1c4c-47a5-965d-efbb2d422f0c"), "Cinéma")),

                cover = CoverForPodcast(
                        id = UUID.fromString("1e275238-4cbe-4abb-bbca-95a0e4ebbeea"),
                        url = URI("https://external.domain.tld/3.png"),
                        height = 200, width = 200
                )
        )

        @Test
        fun `with no podcast`() {
            /* Given */
            whenever(podcastService.findAll()).thenReturn(Flux.empty())

            /* When */
            rest
                    .get()
                    .uri("https://localhost:8080/api/v1/podcasts/opml")
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .xml("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <opml version="2.0">
                          <head>
                            <title>Podcast-Server</title>
                          </head>
                          <body />
                        </opml>
                    """.trimIndent())
        }

        @Test
        fun `with one podcast`() {
            /* Given */
            whenever(podcastService.findAll()).thenReturn(listOf(podcast1).toFlux())

            /* When */
            rest
                    .get()
                    .uri("https://localhost:8080/api/v1/podcasts/opml")
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .xml("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <opml version="2.0">
                          <head>
                            <title>Podcast-Server</title>
                          </head>
                          <body>
                            <outline text="Podcast first" description="desc" htmlUrl="https://localhost:8080/podcasts/ad16b2eb-657e-4064-b470-5b99397ce729" title="Podcast first" type="rss" version="RSS2" xmlUrl="https://localhost:8080/api/podcasts/ad16b2eb-657e-4064-b470-5b99397ce729/rss" />
                          </body>
                        </opml>
                    """.trimIndent())
        }

        @Test
        fun `with 3 podcasts`() {
            /* Given */
            whenever(podcastService.findAll()).thenReturn(listOf(podcast1, podcast2, podcast3).toFlux())

            /* When */
            rest
                    .get()
                    .uri("https://localhost:8080/api/v1/podcasts/opml")
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .xml("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <opml version="2.0">
                          <head>
                            <title>Podcast-Server</title>
                          </head>
                          <body>
                            <outline text="Podcast first" description="desc" htmlUrl="https://localhost:8080/podcasts/ad16b2eb-657e-4064-b470-5b99397ce729" title="Podcast first" type="rss" version="RSS2" xmlUrl="https://localhost:8080/api/podcasts/ad16b2eb-657e-4064-b470-5b99397ce729/rss" />
                            <outline text="Podcast second" description="desc" htmlUrl="https://localhost:8080/podcasts/bd16b2eb-657e-4064-b470-5b99397ce729" title="Podcast second" type="rss" version="RSS2" xmlUrl="https://localhost:8080/api/podcasts/bd16b2eb-657e-4064-b470-5b99397ce729/rss" />
                            <outline text="Podcast third" description="desc" htmlUrl="https://localhost:8080/podcasts/cd16b2eb-657e-4064-b470-5b99397ce729" title="Podcast third" type="rss" version="RSS2" xmlUrl="https://localhost:8080/api/podcasts/cd16b2eb-657e-4064-b470-5b99397ce729/rss" />
                          </body>
                        </opml>
                    """.trimIndent())
        }

    }

    @Nested
    @DisplayName("should generate rss")
    inner class ShouldGenerateRss {

        private val coverForItem = CoverForItem(
                id = UUID.fromString("f4efe8db-7abf-4998-b15c-9fa2e06096a1"),
                url = "https://external.domain.tld/foo/bar.png",
                width = 200,
                height = 200
        )

        private val podcastForItem = PodcastForItem(
                id = UUID.fromString("dd16b2eb-657e-4064-b470-5b99397ce729"),
                title = "Podcast title",
                url = "https://foo.bar.com/app/file.rss"
        )

        private val items = (200 downTo 1)
                .map { it.toString().padStart(3, '0') }
                .map { Item(
                            id = UUID.fromString("27184b1a-7642-4ffd-ac7e-14fb36f7f$it"),
                            title = "Foo $it",
                            url = "https://external.domain.tld/foo/bar.$it.mp4",

                            pubDate = OffsetDateTime.of(2019, 6, 24, 5, 28, 54, 34, ZoneOffset.ofHours(2)).minusDays(200 - it.toLong() ),
                            creationDate = OffsetDateTime.of(2019, 6, 24, 5, 29, 54, 34, ZoneOffset.ofHours(2)).minusDays(200 - it.toLong()),
                            downloadDate = OffsetDateTime.of(2019, 6, 25, 5, 30, 54, 34, ZoneOffset.ofHours(2)).minusDays(200 - it.toLong()),

                            description = "desc $it",
                            mimeType = "video/mp4",
                            length = 100,
                            fileName = null,
                            status = Status.NOT_DOWNLOADED,

                            podcast = podcastForItem,
                            cover = coverForItem
                    ) }

        @Test
        fun `for podcast with limit`() {
            /* Given */
            val podcastId = podcastForItem.id
            val size = 50
            val page = ItemPageRequest(0, size, ItemSort("DESC", "pubDate"))
            val result = PageItem.of(items.take(size), size, page)

            whenever(itemService.search(anyOrNull(), eq(listOf()), eq(listOf()), eq(page), eq(podcastId)))
                    .thenReturn(result.toMono())
            whenever(podcastService.findById(podcastId))
                    .thenReturn(podcast.toMono())

            val xml = IOUtils.fileAsString("/xml/podcast-with-50-items.xml")

            /* When */
            rest
                    .get()
                    .uri("https://localhost:8080/api/v1/podcasts/$podcastId/rss")
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .xml(xml.trimIndent())
        }

        @Test
        fun `for podcast without limit`() {
            /* Given */
            val podcastId = podcastForItem.id
            val page = ItemPageRequest(0, Int.MAX_VALUE, ItemSort("DESC", "pubDate"))
            val result = PageItem.of(items.take(200), 200, page)

            whenever(itemService.search(anyOrNull(), eq(listOf()), eq(listOf()), eq(page), eq(podcastId)))
                    .thenReturn(result.toMono())
            whenever(podcastService.findById(podcastId))
                    .thenReturn(podcast.toMono())

            val xml = IOUtils.fileAsString("/xml/podcast-with-200-items.xml")

            /* When */
            rest
                    .get()
                    .uri("https://localhost:8080/api/v1/podcasts/$podcastId/rss?limit=false")
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .xml(xml.trimIndent())
        }



    }
}
