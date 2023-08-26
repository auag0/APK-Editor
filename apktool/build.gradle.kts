plugins {
    id("com.android.library")
}

android {
    namespace = "brut.apktool"
    compileSdk = 33
    
    defaultConfig {
        minSdk = 26
    }
    
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation("commons-cli:commons-cli:1.5.0")
    implementation("org.yaml:snakeyaml:2.1")
    implementation("org.apache.commons:commons-text:1.10.0")
    implementation("com.android.tools.smali:smali-dexlib2:3.0.3")
    implementation("org.antlr:antlr-runtime:3.5.3")
    implementation("com.android.tools.smali:smali:3.0.3")
    implementation("com.google.guava:guava:32.1.2-jre")
    implementation("com.android.tools.smali:smali-baksmali:3.0.3")
    implementation("commons-io:commons-io:2.13.0")
    implementation(files("libs\\jce.jar"))
    implementation(files("libs\\rt.jar"))
    implementation(files("libs\\xpp3-1.1.4c.jar"))
}