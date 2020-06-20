
val skaffold: String? by project

if (System.getenv("SKAFFOLD") != null || (skaffold?.toBoolean() == true)) {
    val databaseUrl by extra("jdbc:postgresql://postgres:${System.getenv("DATABASE_PORT")}/${System.getenv("DATABASE_NAME")}")
    val databaseUser by extra(System.getenv("DATABASE_USERNAME"))
    val databasePassword by extra(System.getenv("DATABASE_PASSWORD"))
}
