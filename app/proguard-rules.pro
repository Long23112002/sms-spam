# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# R8 optimization settings (simplified for R8 compatibility)
-dontusemixedcaseclassnames
-verbose
-allowaccessmodification
-repackageclasses ''

# Đổi tên các class, method, field thành tên khó hiểu (R8 compatible)
# Note: R8 uses different obfuscation mechanisms
# Custom dictionaries may not work the same way in R8

# Giữ lại các annotation cần thiết
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Giữ lại các class chính của ứng dụng
-keep public class com.example.sms_app.presentation.activity.MainActivity
-keep public class com.example.sms_app.presentation.SmsApplication
-keep public class com.example.sms_app.service.SmsService
-keep public class com.example.sms_app.service.HiddenSmsReceiver

# Anti-tamper: Hide crucial classes
-keep,allowobfuscation class com.example.sms_app.** { 
    <fields>; 
    <methods>; 
}

# Block reverse engineering tools
-assumevalues class android.os.Build$VERSION {
    int SDK_INT return 10..100;
}

# Anti-debug
-keepclasseswithmembers class * {
    native <methods>;
}

# Encrypt strings
-keepclassmembers class * {
    static final java.lang.String *;
    static java.lang.String *;
    java.lang.String *;
}

# Giữ lại các class liên quan đến Compose UI
-keep class androidx.compose.** { *; }
-keep class androidx.activity.compose.** { *; }

# Giữ lại các class liên quan đến SMS API
-keep class android.telephony.SmsManager { *; }

# Giữ lại các class liên quan đến Gson
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Giữ lại các class liên quan đến JExcelAPI (jxl) - QUAN TRỌNG cho Excel import
-keep class jxl.** { *; }
-keep interface jxl.** { *; }
-keep enum jxl.** { *; }
-keepclassmembers class jxl.** {
    <fields>;
    <methods>;
    <init>(...);
}

# Giữ lại các class liên quan đến Apache POI (QUAN TRỌNG cho Excel import)
-keep class org.apache.poi.** { *; }
-keep interface org.apache.poi.** { *; }
-keep enum org.apache.poi.** { *; }
-keepclassmembers class org.apache.poi.** {
    <fields>;
    <methods>;
    <init>(...);
}

# Giữ lại các class XML và dependencies của POI
-keep class org.openxmlformats.** { *; }
-keep class org.xml.** { *; }
-keep class org.w3c.** { *; }
-keep class org.apache.commons.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class schemaorg_apache_xmlbeans.** { *; }
-keep class com.zaxxer.sparsebits.** { *; }

# Giữ lại các class logging của Apache POI
-keep class org.apache.logging.log4j.** { *; }
-keep class org.apache.log4j.** { *; }

# Giữ lại các class compression
-keep class org.apache.commons.compress.** { *; }

# Giữ lại các class math và collections
-keep class org.apache.commons.math3.** { *; }
-keep class org.apache.commons.collections4.** { *; }
-keep class org.apache.commons.codec.** { *; }
-keep class org.apache.commons.io.** { *; }

# Giữ lại các class XMLBeans cụ thể
-keep class com.microsoft.schemas.** { *; }
-keep class org.etsi.uri.** { *; }
-keep class org.w3.** { *; }

# Giữ lại tất cả các resource cho Apache POI (R8 compatible)
# Note: R8 doesn't support -keepresourcexmlelements and -keepresourcefiles
# These resources will be handled by the packaging configuration in build.gradle

# Tránh các cảnh báo không cần thiết từ JExcelAPI
-dontwarn jxl.**

# Tránh các cảnh báo không cần thiết từ Apache POI
-dontwarn org.apache.poi.**
-dontwarn org.openxmlformats.**
-dontwarn org.xml.**
-dontwarn org.w3c.**
-dontwarn org.apache.commons.**
-dontwarn org.apache.xmlbeans.**
-dontwarn schemaorg_apache_xmlbeans.**
-dontwarn com.zaxxer.sparsebits.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.apache.log4j.**
-dontwarn com.microsoft.schemas.**
-dontwarn org.etsi.uri.**
-dontwarn org.w3.**

# Ignore missing Java Beans classes
-dontwarn java.beans.**
-dontwarn javax.beans.**

# Ignore missing JMX classes
-dontwarn com.sun.jdmk.comm.**
-dontwarn javax.management.**
-dontwarn org.apache.log4j.jmx.**
-dontwarn org.apache.log4j.config.**

# Giữ lại các class liên quan đến JNI
-keepclasseswithmembernames class * {
    native <methods>;
}

# Giữ lại các class liên quan đến Android components
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.Service

# Giữ lại các class liên quan đến Android UI
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# Giữ lại các class liên quan đến R
-keep class **.R
-keep class **.R$* {
    <fields>;
}

# Giữ lại các class liên quan đến BuildConfig
-keep class **.BuildConfig { *; }

# Xóa các log và debug info
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
}

# Giữ lại các class liên quan đến reflection
-keepattributes InnerClasses
-keep class **.R$* { *; }

# Giữ lại các class liên quan đến DexClassLoader
-keep class dalvik.system.DexClassLoader { *; }
-keep class dalvik.system.PathClassLoader { *; }
-keep class dalvik.system.InMemoryDexClassLoader { *; }

# Giữ lại các class liên quan đến SafetyNet
-keep class com.google.android.gms.safetynet.** { *; }

# Giữ lại các class liên quan đến WorkManager
-keep class androidx.work.** { *; }
-keep class androidx.work.impl.** { *; }

# Giữ lại các class liên quan đến Coroutines
-keep class kotlinx.coroutines.** { *; }

# Giữ lại các class liên quan đến Lifecycle
-keep class androidx.lifecycle.** { *; }

# Xử lý các class bị thiếu trong R8
-dontwarn org.ietf.jgss.**
-dontwarn org.osgi.framework.**
-dontwarn javax.naming.**
-dontwarn java.lang.invoke.MethodHandle
-dontwarn java.lang.invoke.MethodHandles$Lookup
-dontwarn java.lang.invoke.MethodType
-dontwarn javax.**
-dontwarn java.awt.**

# Keep Google Play Services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Keep all classes in our app
-keep class com.example.sms_app.** { *; }

# Bảo vệ đặc biệt cho ExcelImporter và các class liên quan
-keep class com.example.sms_app.utils.ExcelImporter { *; }
-keep class com.example.sms_app.data.Customer { *; }
-keepclassmembers class com.example.sms_app.data.Customer {
    <fields>;
    <methods>;
    <init>(...);
}

# Bảo vệ reflection cho các class có thể được gọi động
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Bảo vệ các class Java I/O cần thiết cho Excel import
-keep class java.io.** { *; }
-keep class java.nio.** { *; }
-keep class java.util.** { *; }
-dontwarn java.io.**
-dontwarn java.nio.**

# Bảo vệ các exception class để tránh lỗi runtime
-keep class java.lang.Exception { *; }
-keep class java.lang.RuntimeException { *; }
-keep class java.io.IOException { *; }

# Bảo vệ ContentResolver và Uri cho việc đọc file
-keep class android.content.ContentResolver { *; }
-keep class android.net.Uri { *; }
-keep class android.database.Cursor { *; }

# Bật optimization và shrinking cho release build
# -dontoptimize
# -dontshrink