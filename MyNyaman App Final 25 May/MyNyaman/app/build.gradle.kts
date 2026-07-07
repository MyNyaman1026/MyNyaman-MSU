import java.util.Properties
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use { load(it) }
    }
}

fun escapeForBuildConfig(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")

android {
    namespace = "com.mynyaman.mynyamanapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mynyaman.mynyamanapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "FORGE_API_URL", "\"${escapeForBuildConfig(localProperties.getProperty("BUILT_IN_FORGE_API_URL", "https://forge.manus.im"))}\"")
        buildConfigField("String", "FORGE_API_KEY", "\"${escapeForBuildConfig(localProperties.getProperty("BUILT_IN_FORGE_API_KEY", ""))}\"")
        buildConfigField("String", "DEEPSEEK_API_URL", "\"${escapeForBuildConfig(localProperties.getProperty("DEEPSEEK_API_URL", "https://api.deepseek.com"))}\"")
        buildConfigField("String", "DEEPSEEK_API_KEY", "\"${escapeForBuildConfig(localProperties.getProperty("DEEPSEEK_API_KEY", ""))}\"")
        buildConfigField("String", "GROQ_API_KEY", "\"${escapeForBuildConfig(localProperties.getProperty("GROQ_API_KEY", ""))}\"")
        buildConfigField("String", "GROQ_MODEL", "\"${escapeForBuildConfig(localProperties.getProperty("GROQ_MODEL", "llama-3.1-8b-instant"))}\"")
        buildConfigField("String", "GROQ_BASE_URL", "\"${escapeForBuildConfig(localProperties.getProperty("GROQ_BASE_URL", "https://api.groq.com/openai/v1/chat/completions"))}\"")
        buildConfigField("String", "GROQ_CONSULTANT_MODEL", "\"${escapeForBuildConfig(localProperties.getProperty("GROQ_CONSULTANT_MODEL", "llama-3.3-70b-versatile"))}\"")
        buildConfigField("String", "WEBSITE_BASE_URL", "\"${escapeForBuildConfig(localProperties.getProperty("MYNYAMAN_WEB_BASE_URL", ""))}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/INDEX.LIST",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt"
            )
        }
    }
}
tasks.withType<JavaCompile>().configureEach {
    javaCompiler.set(javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    })
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-analytics")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}

configurations.all {
    resolutionStrategy {
        force("io.grpc:grpc-core:1.70.0")
        force("io.grpc:grpc-api:1.70.0")
        force("io.grpc:grpc-context:1.70.0")
        force("io.grpc:grpc-android:1.70.0")
        force("io.grpc:grpc-okhttp:1.70.0")
        force("io.grpc:grpc-protobuf-lite:1.70.0")
        force("io.grpc:grpc-stub:1.70.0")
        force("io.grpc:grpc-util:1.70.0")
    }
}

