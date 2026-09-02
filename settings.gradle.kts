// Позволяет Gradle самому скачать JDK, указанный в gradle/gradle-daemon-jvm.properties,
// если подходящего нет в системе. Без него файл критериев требует уже установленный JDK.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "wanderingtable"
