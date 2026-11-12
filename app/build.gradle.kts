plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "pt.upt.studenthub"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "pt.upt.studenthub.a51602"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
}
