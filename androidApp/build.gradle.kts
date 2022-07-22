import java.io.ByteArrayOutputStream

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
}

fun Project.execWithOutput(cmd: String, ignore: Boolean = false): String {
    var result: String = ByteArrayOutputStream().use { outputStream ->
        project.exec {
            commandLine = cmd.split(" ")
            standardOutput = outputStream
            isIgnoreExitValue = ignore ?: false
        }
        outputStream.toString()
    }
    return result
}

fun gitVersion(): String {
    var process = ""
    val maybeTagOfCurrentCommit = execWithOutput("git -C ../ describe --contains HEAD", true)
    process = if (maybeTagOfCurrentCommit.isEmpty()) {
        println("No tag on current commit. Will take the latest one.")
        execWithOutput("git -C ../ for-each-ref refs/tags --sort=-authordate --format='%(refname:short)' --count=1")
    } else {
        println("Tag found on current commit")
        execWithOutput("git -C ../ describe --contains HEAD")
    }
    return process.replace("'", "").substring(1).replace("\\.", "").trim()
}

fun versionCodeFromGit(): Int {
    println("version code " + gitVersion())
    return gitVersion().toInt()
}

fun versionNameFromGit(): String {
    println("version name " + gitVersion())
    return gitVersion()
}

android {
    compileOptions {
        // Flag to enable support for the new language APIs
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    compileSdk = 31
    buildToolsVersion = "31.0.0"
    buildFeatures {
        viewBinding = true
    }
    defaultConfig {
        applicationId = "bou.amine.apps.readerforselfossv2.android"
        minSdk = 21
        targetSdk = 31
        versionCode = versionCodeFromGit()
        versionName = versionNameFromGit()

        multiDexEnabled = true
        lint {
            abortOnError = true
        }
        vectorDrawables.useSupportLibrary = true

        // tests
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf("room.schemaLocation" to "$projectDir/schemas")
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
        getByName("debug") {
            buildConfigField("String", "LOGIN_URL", properties["appLoginUrl"] as String)
            buildConfigField("String", "LOGIN_PASSWORD", properties["appLoginPassword"] as String)
            buildConfigField("String", "LOGIN_USERNAME", properties["appLoginUsername"] as String)
        }
    }
    flavorDimensions.add("build")
    productFlavors {
        create("github") {
            versionNameSuffix = "-github"
            dimension = "build"
        }
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

}

dependencies {
    implementation(project(":shared"))
    implementation("com.google.android.material:material:1.5.0")
    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.0")

    implementation("androidx.preference:preference-ktx:1.1.1")

    // Testing
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0-alpha02")
    androidTestImplementation("androidx.test:runner:1.3.1-alpha02")
    // Espresso-contrib for DatePicker, RecyclerView, Drawer actions, Accessibility checks, CountingIdlingResource
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.4.0-alpha02")
    // Espresso-intents for validation and stubbing of Intents
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.4.0-alpha02")
    implementation(fileTree(mapOf("include" to listOf("*.jar"), "dir" to "libs")))

    // Android Support
    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("com.google.android.material:material:1.5.0")
    implementation("androidx.recyclerview:recyclerview:1.3.0-alpha01")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.vectordrawable:vectordrawable:1.2.0-alpha02")
    implementation("androidx.browser:browser:1.4.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.annotation:annotation:1.3.0")
    implementation("androidx.work:work-runtime-ktx:2.7.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.3")
    implementation("org.jsoup:jsoup:1.14.3")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.5")

    //multidex
    implementation("androidx.multidex:multidex:2.0.1")

    // About
    implementation("com.mikepenz:aboutlibraries-core:8.9.4")
    implementation("com.mikepenz:aboutlibraries:8.9.4")
    implementation("com.mikepenz:aboutlibraries-definitions:8.9.4")

    // Async
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.0")

    // Retrofit + http logging + okhttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.3")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.burgstaller:okhttp-digest:2.5")

    // Material-ish things
    implementation("com.ashokvarma.android:bottom-navigation-bar:2.2.0")
    implementation("com.amulyakhare:com.amulyakhare.textdrawable:1.0.1")

    // glide
    kapt("com.github.bumptech.glide:compiler:4.11.0")
    implementation("com.github.bumptech.glide:okhttp3-integration:4.1.1")

    // Drawer
    implementation("com.mikepenz:materialdrawer:8.4.5")

    // Themes
    implementation("com.52inc:scoops:1.0.0")
    implementation("com.jaredrummler:colorpicker:1.1.0")
    implementation("com.github.rubensousa:floatingtoolbar:1.5.1")

    // Pager
    implementation("me.relex:circleindicator:2.1.6")
    implementation("androidx.viewpager2:viewpager2:1.1.0-beta01")

    //Dependency Injection
    implementation("org.kodein.di:kodein-di:7.12.0")
    implementation("org.kodein.di:kodein-di-framework-android-x:7.12.0")

    //Settings
    implementation("com.russhwolf:multiplatform-settings-no-arg:0.9")

    //PhotoView
    implementation("com.github.chrisbanes:PhotoView:2.3.0")

    implementation("androidx.core:core-ktx:1.7.0")

    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.4.0")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.4.0")

    implementation("androidx.room:room-ktx:2.4.0-beta01")
    kapt("androidx.room:room-compiler:2.4.0-beta01")

    implementation("android.arch.work:work-runtime-ktx:1.0.1")
}