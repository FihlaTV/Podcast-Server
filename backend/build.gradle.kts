import com.rohanprabhu.gradle.plugins.kdjooq.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import kotlin.collections.*
import java.time.*
import java.time.format.*
import org.gradle.internal.deprecation.*

plugins {
	id("org.springframework.boot") version "2.3.1.RELEASE"
	id("io.spring.dependency-management") version "1.0.9.RELEASE"
	id("com.gorylenko.gradle-git-properties") version "2.2.2"
	id("org.flywaydb.flyway") version "6.4.4"
	id("com.rohanprabhu.kotlin-dsl-jooq") version "0.4.6"
	id("com.google.cloud.tools.jib") version "2.4.0"

	kotlin("jvm") version "1.3.72"
	kotlin("plugin.spring") version "1.3.72"
	jacoco
}

group = "com.github.davinkevin.podcastserver"
version = "2019.1.0"
java.sourceCompatibility = JavaVersion.VERSION_11

apply(from = "gradle/profile-default.gradle.kts")
apply(from = "gradle/profile-ci.gradle.kts")
apply(from = "gradle/profile-skaffold.gradle.kts")

val db = databaseParameters()

repositories {
	mavenCentral()
	maven { url = uri("https://jitpack.io") }
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.springframework.boot:spring-boot-starter-jooq")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-validation")

	implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions:1.0.2.RELEASE")
	implementation("javax.annotation:javax.annotation-api:1.3.2")
	implementation("org.postgresql:postgresql:42.2.12")
	jooqGeneratorRuntime("org.postgresql:postgresql:42.2.12")

	implementation("org.jdom:jdom2:2.0.6")
	implementation("org.apache.tika:tika-core:1.24.1")
	implementation("org.jsoup:jsoup:1.13.1")
	implementation("com.github.pedroviniv:youtubedl-java:ef7110605d23eaaae4796312163bcf84c7099311")
	implementation("com.jayway.jsonpath:json-path:2.4.0")
	implementation("commons-io:commons-io:2.4")
	implementation("net.bramp.ffmpeg:ffmpeg:0.6.1")

	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
	}
	testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.1.0")
	testImplementation("net.javacrumbs.json-unit:json-unit-assertj:2.10.0")
	testImplementation("com.github.tomakehurst:wiremock:2.25.1")
	testImplementation("com.ninja-squad:DbSetup:1.6.0")
	testImplementation("org.awaitility:awaitility:3.1.6")

}

gitProperties {
	dotGitDirectory = "${project.rootDir}/../.git"
}

flyway {
	url = db.url
	user = db.user
	password = db.password
	locations = db.sqlFiles.toTypedArray()
}

jooqGenerator {

	jooqVersion = "3.13.2"

	configuration("primary", project.sourceSets.getByName("main")) {

		configuration = jooqCodegenConfiguration {
			jdbc {
				url = db.url
				username = db.user
				password = db.password
				driver = "org.postgresql.Driver"
			}

			generator {
				database {
					name = "org.jooq.meta.postgres.PostgresDatabase"
					inputSchema = "public"
					excludes = "Databasechangelog|Databasechangeloglock|FlywaySchemaHistoryRecord"

					forcedTypes {
						forcedType {
							userType = "com.github.davinkevin.podcastserver.entity.Status"
							converter = "com.github.davinkevin.podcastserver.database.StatusConverter"
							includeExpression = "ITEM\\.STATUS"
						}
					}

					isIncludeTables = true
					isIncludeRoutines = true
					isIncludePackages = false
					isIncludeUDTs = true
					isIncludeSequences = true
					isIncludePrimaryKeys = true
					isIncludeUniqueKeys = true
					isIncludeForeignKeys = true
				}

				target {
					packageName = "com.github.davinkevin.podcastserver.database"
					directory = "${project.buildDir}/generated/jooq/primary"
				}
			}
		}
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
	systemProperty("user.timezone", "UTC")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf(
				"-Xjsr305=strict",
				"-Xallow-result-return-type"
		)
		jvmTarget = "11"
	}
}

@Suppress("PropertyName")
val `jooq-codegen-primary` by project.tasks
`jooq-codegen-primary`.dependsOn("flywayMigrate")

jib {
	val imageTags: Set<String>? by project.extra
	val providedTag = project.findProperty("tag")?.toString()
	val baseImageTag =  providedTag ?: "master"
	val jibTags = when {
		imageTags != null -> imageTags
		providedTag != null -> setOf(providedTag)
		else -> setOf(OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmssSSS"))!!)
	}

	from {
		image = "podcastserver/backend-base-image:${baseImageTag}"
	}
	to {
		image = "podcastserver/backend"
		tags = jibTags
		auth {
			username = System.getenv("DOCKER_IO_USER")
			password = System.getenv("DOCKER_IO_PASSWORD")
		}
	}
}

data class DatabaseParameters(
		val url: String,
		val user: String,
		val password: String,
		val sqlFiles: List<String> = listOf("filesystem:../database/migrations")
)
fun databaseParameters(): DatabaseParameters {
	val databaseUrl: String by project.extra
	val databaseUser: String by project.extra
	val databasePassword: String by project.extra
	return DatabaseParameters(databaseUrl, databaseUser, databasePassword)
}

tasks.register("downloadDependencies") {
	fun Configuration.isDeprecated(): Boolean = when (this) {
		is DeprecatableConfiguration -> resolutionAlternatives != null
		else -> false
	}
	doLast {
		val buildDeps = buildscript
				.configurations
				.map { it.resolve().size }
				.sum()

		val allDeps = configurations
				.filter { it.isCanBeResolved && !it.isDeprecated() }
				.map { it.resolve().size }
				.sum()

		println("Downloaded all dependencies: ${allDeps + buildDeps}")
	}
}
