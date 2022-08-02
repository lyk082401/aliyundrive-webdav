plugins {
    id("java")
}

group = "net.xdow"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    implementation ("com.squareup.okhttp3:okhttp:3.12.13") //Api19
    implementation ("com.google.guava:guava:30.1.1-android")
    implementation ("org.apache.commons:commons-lang3:3.8")  //java7
    implementation ("commons-io:commons-io:2.6") //java7
    implementation ("com.fasterxml.jackson.core:jackson-core:2.11.3")
    implementation ("com.fasterxml.jackson.core:jackson-databind:2.11.3")
    implementation ("org.slf4j:slf4j-api:1.7.32")
    implementation ("org.slf4j:slf4j-android:1.7.32")
    compileOnly ("org.apache.tomcat.embed:tomcat-embed-core:10.0.21")
//    compileOnly ("org.eclipse.jetty:jetty-webapp:8.0.0.v20110901")
//    compileOnly ("org.mortbay.jetty:servlet-api:3.0.20100224")
//    compileOnly project(":library")
}