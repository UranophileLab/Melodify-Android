# ─── Debugging / Attributes ───────────────────────────────────────────────────
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod

# ─── Kotlin ───────────────────────────────────────────────────────────────────
-keep class kotlin.Metadata { *; }

# ─── Gson / JSON Models ───────────────────────────────────────────────────────
# Prevent R8 from stripping fields used by Gson for JSON deserialization
-dontwarn sun.misc.**

-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }

# Keep all data/model/record classes that Gson deserializes into
-keep class dev.melodify.uranophilelab.model.** { *; }
-keep class dev.melodify.uranophilelab.records.** { *; }
-keep class dev.melodify.uranophilelab.activities.ListActivity$ArtistData { *; }
-keep class dev.melodify.uranophilelab.utils.UpdateManager$GitHubRelease { *; }
-keep class dev.melodify.uranophilelab.utils.UpdateManager$GitHubRelease$Asset { *; }
-keep class dev.melodify.uranophilelab.utils.TrackDownloader$DownloadedTrack { *; }
-keep class dev.melodify.uranophilelab.utils.SharedPreferenceManager$** { *; }

# ─── OkHttp ───────────────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ─── Glide ────────────────────────────────────────────────────────────────────
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.AppGlideModule { *; }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

# ─── Room ─────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class **_Impl { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}
-keepclassmembers class * {
    @androidx.room.Dao *;
    @androidx.room.Database *;
    @androidx.room.Entity *;
}
-dontwarn androidx.room.paging.**
# Specific Room implementations for SharedPreferenceManager
-keep class dev.melodify.uranophilelab.utils.CacheDatabase { *; }
-keep class dev.melodify.uranophilelab.utils.CacheDatabase_Impl { *; }
-keep class dev.melodify.uranophilelab.utils.KeyValue { *; }
-keep class dev.melodify.uranophilelab.utils.KeyValueDao { *; }
-dontwarn androidx.room.util.TableInfo$Column
-dontwarn androidx.room.util.TableInfo$ForeignKey
-dontwarn androidx.room.util.TableInfo$Index

# ─── jAudioTagger & ImageIO ───────────────────────────────────────────────────
-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**
-dontwarn java.awt.**
-dontwarn javax.imageio.**
# jAudioTagger uses SLF4J; StaticLoggerBinder is an optional binding removed in SLF4J 2.x
-dontwarn org.slf4j.**
-dontwarn org.slf4j.impl.**

# ─── ExoPlayer / Media3 ───────────────────────────────────────────────────────
-dontwarn androidx.media3.**
-keep class androidx.media3.** { *; }

# ─── AndroidX / Support ───────────────────────────────────────────────────────
-keep class androidx.core.app.CoreComponentFactory { *; }

# ─── lrclib / kotlinx.serialization ──────────────────────────────────────────
-keep class com.samyak.lrclib.** { *; }
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class * {
    *** Companion;
    *** $serializer;
}

# ─── Ktor ─────────────────────────────────────────────────────────────────────
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# ─── UI Libraries ─────────────────────────────────────────────────────────────
-keep class com.yarolegovich.slidingrootnav.** { *; }
-keep interface com.yarolegovich.slidingrootnav.** { *; }
-keep class com.markomilos.paginate.** { *; }
-keep interface com.markomilos.paginate.** { *; }
-keep class com.facebook.shimmer.** { *; }
-keep class me.everything.android.ui.overscroll.** { *; }

# ─── Enums ────────────────────────────────────────────────────────────────────
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ─── HiddenApiBypass ──────────────────────────────────────────────────────────
-keep class org.lsposed.hiddenapibypass.** { *; }
