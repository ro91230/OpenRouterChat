pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT) // تم التعديل هنا لتخطي قفل الأمان
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "OpenRouterChat"
include(":app")
