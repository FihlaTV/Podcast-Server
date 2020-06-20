if (System.getenv("CI") != null) {
    val databaseUrl by extra("jdbc:postgresql://postgres:5432/${System.getenv("POSTGRES_DB")}")
    val databaseUser by extra(System.getenv("POSTGRES_USER"))
    val databasePassword by extra(System.getenv("POSTGRES_PASSWORD"))
    val imageTag by extra(generateTagsList())
}

@Suppress("LocalVariableName")
fun generateTagsList(): Set<String> {
    val CI_COMMIT_TAG = System.getenv("CI_COMMIT_TAG")
    val CI_COMMIT_REF_SLUG = System.getenv("CI_COMMIT_REF_SLUG")

    if (CI_COMMIT_TAG != null) {
        return setOf(CI_COMMIT_TAG)
    }

    if(CI_COMMIT_TAG != "master") {
        return setOf(CI_COMMIT_REF_SLUG)
    }
    return setOf(CI_COMMIT_REF_SLUG, "master")
}
