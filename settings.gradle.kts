pluginManagement {
    repositories {
        maven { url = uri("https://nexus20.tclking.com/repository/proxy-nexus-maven-pub/") }
        maven {
            url = uri("http://10.92.35.98:8081/nexus/repository/maven-public/")
            isAllowInsecureProtocol = true
        }
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.tct.sign-plugin") {
                useModule("com.tct.sign.plugin:autosign:${requested.version}")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://nexus20.tclking.com/repository/proxy-nexus-maven-pub/") }
        maven {
            url = uri("http://10.92.35.98:8081/nexus/repository/maven-public/")
            isAllowInsecureProtocol = true
        }
    }
}

rootProject.name = "ActivityTest"
include(":app")
