# Socket.IO
-keep class io.socket.** { *; }
-dontwarn io.socket.**

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.chatroom.app.data.model.**$$serializer { *; }
-keepclassmembers class com.chatroom.app.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.chatroom.app.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep launcher icon resources (referenced from manifest, not code)
-keep class androidx.compose.material.icons.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
