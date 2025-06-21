plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val googleServicesJson = file("google-services.json")
if (googleServicesJson.exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.shane.ota"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.shane.ota"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "DEBUG"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild {
            cmake {
                arguments(
                    "-DANDROID_STL=c++_shared",
                    "-DANDROID_PLATFORM=android-24",
                    "-DANDROID_ARM_NEON=TRUE"
                )
                cppFlags("-std=c++17", "-fexceptions", "-frtti")
            }
        }

        ndk {
            // Specify target ABI
            abiFilters.add("arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
        debug {
            isJniDebuggable = true
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
            keepDebugSymbols.add("**/*.so")
        }

        // Exclude conflicting native libraries
        resources {
            excludes += "/lib/arm64-v8a/libstdc++.so"
            excludes += "/lib/arm64-v8a/libc++_shared.so"
        }
    }

    // NDK version specification
    ndkVersion = "25.1.8937393" // Use your specific NDK version

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Android Request Inspector WebView library
    implementation("com.github.acsbendi:Android-Request-Inspector-WebView:1.0.3")

    // RxJava dependencies needed for the Request Inspector
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    implementation("io.reactivex.rxjava3:rxjava:3.1.5")
    implementation("io.reactivex.rxjava3:rxandroid:3.0.0")
    implementation("com.github.akarnokd:rxjava3-bridge:3.0.0")

    // Gson for JSON handling
    implementation("com.google.code.gson:gson:2.10.1")

    // WebKit for WebView features
    implementation("androidx.webkit:webkit:1.6.1")
    implementation(libs.firebase.messaging.ktx)

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation(platform("com.google.firebase:firebase-bom:33.12.0"))
    implementation(platform("com.google.firebase:firebase-messaging"))

    implementation("androidx.biometric:biometric:1.1.0")
    
    // AndroidX Security for encrypted storage
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}
