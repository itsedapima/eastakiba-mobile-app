# ── Retrofit + OkHttp ────────────────────────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# ── Gson ──────────────────────────────────────────────────────────────────────
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# ── Data models (preserve for JSON deserialization) ──────────────────────────
-keep class com.sacco.app.data.models.** { *; }

# ── R8 full mode safe-keeps ──────────────────────────────────────────────────
-keepclassmembers class * extends androidx.lifecycle.ViewModel { <init>(...); }
-keep class * extends androidx.fragment.app.Fragment { *; }

# ── iTextPDF ──────────────────────────────────────────────────────────────────
-dontwarn com.itextpdf.**
-keep class com.itextpdf.** { *; }

# ── Facebook Shimmer ─────────────────────────────────────────────────────────
-keep class com.facebook.shimmer.** { *; }

# ── Glide ─────────────────────────────────────────────────────────────────────
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep class com.bumptech.glide.** { *; }
-dontwarn com.bumptech.glide.**
