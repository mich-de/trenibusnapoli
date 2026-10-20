-keep class org.jsoup.** { *; }
-keep class org.json.** { *; }
-keep class com.eavorari.data.model.** { *; }
-dontwarn org.jspecify.annotations.**

-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**

-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
