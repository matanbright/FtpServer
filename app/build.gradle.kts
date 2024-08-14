plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.matanbright.ftpserver"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.matanbright.ftpserver"
        minSdk = 28
        /*
         * The storage read/write permissions had been deprecated starting in SDK version 29
         * and were eventually replaced by another one, which is unfortunately not available
         * in some of Android's variants (e.g., Android TV, Android Auto, Wear OS).
         * So, in order to use these permissions, the target SDK version must be bellow 29.
         */
        //noinspection ExpiredTargetSdkVersion
        targetSdk = 28
        versionCode = 1
        versionName = "1.0.0"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    packaging {
        resources.excludes.add("META-INF/DEPENDENCIES")
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.preference)

    implementation(libs.ftpserver.core)
    implementation(libs.ftplet.api)
    implementation(libs.mina.core)
    implementation(libs.slf4j.api)
    testImplementation(libs.slf4j.simple)
}
