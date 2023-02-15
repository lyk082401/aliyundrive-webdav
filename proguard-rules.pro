# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-applymapping ../app/build/outputs/mapping/release/mapping.txt
-keepclassmembers class com.github.zxbu.webdavteambition.**,
    net.sf.webdav.WebdavServlet,
    com.github.zxbu.webdavteambition.store.AliYunDriverFileSystemStore,
    com.github.zxbu.webdavteambition.filter.ErrorFilter {
   public *;
}
-keep class net.sf.webdav.WebdavServlet
-keep class com.github.zxbu.webdavteambition.store.AliYunDriverFileSystemStore
-keep class com.github.zxbu.webdavteambition.filter.ErrorFilter
-keep class com.github.zxbu.webdavteambition.store.StartupService
-dontobfuscate
-dontoptimize
-dontpreverify
-dontshrink
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers
-dontusemixedcaseclassnames