plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.docscanner"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.docscanner"
        minSdk = 26
        targetSdk = 35
        versionCode = 8
        versionName = "1.5.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val keystorePath = System.getenv("KEYSTORE_PATH")
    val hasSigningConfig = !keystorePath.isNullOrEmpty()

    if (hasSigningConfig) {
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(keystorePath!!)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = true
            if (hasSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = false
            all { test ->
                test.jvmArgs(
                    "--add-opens", "java.base/java.lang=ALL-UNNAMED",
                    "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED"
                )
            }
        }
    }
}

ksp {
    // Export Room schema JSON to app/schemas/ — commit these files so migration history is tracked in git
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // ML Kit Document Scanner
    implementation(libs.gms.mlkit.document.scanner)

    // Coil for image loading
    implementation(libs.coil.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Drag-to-reorder
    implementation(libs.reorderable)

    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// ── JaCoCo custom coverage report with exclusions ────────────────────────────
// AGP's createDebugUnitTestCoverageReport uses an internal JacocoReportTask that
// doesn't support classDirectories filtering. We create a standard JacocoReport
// task that reads the same .exec file and applies exclusions, so the percentage
// reflects only the code that can meaningfully be tested in JVM unit tests.
//
// Excluded: UI (Compose screens/VMs/theme), Room DAOs + generated impls,
//           filesystem/PDF data layer, DI wiring, Android entry points, entities.
// Included: domain/usecase, domain/model, common (pure logic).
apply(plugin = "jacoco")

val jacocoExclusions = listOf(
    "**/ui/**",
    "**/data/local/db/**",
    "**/*_Impl*",
    "**/data/local/filesystem/**",
    "**/data/pdf/**",
    "**/data/repository/DocumentRepositoryImpl*",
    "**/di/**",
    "**/MainActivity*",
    "**/MyApplication*",
    "**/data/local/entity/**",
)

tasks.register<JacocoReport>("coverageReport") {
    group = "verification"
    description = "Unit-test coverage — excludes UI, Room, DI, and Android entry points."
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(false)
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/coverage/filtered"))
    }

    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include("outputs/unit_test_code_coverage/debugUnitTest/*.exec")
        }
    )

    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))

    classDirectories.setFrom(
        fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
            exclude(jacocoExclusions)
        }
    )
}
