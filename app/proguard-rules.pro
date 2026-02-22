# Apache MINA SSHD
-keep class org.apache.sshd.** { *; }
-dontwarn org.apache.sshd.**
-keep class org.apache.mina.** { *; }
-dontwarn org.apache.mina.**

# Bouncy Castle
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.olorin.claudette.models.**$$serializer { *; }
-keepclassmembers class com.olorin.claudette.models.** {
    *** Companion;
}
-keepclasseswithmembers class com.olorin.claudette.models.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Hilt
-keep class dagger.hilt.** { *; }

# SLF4J (used by MINA)
-dontwarn org.slf4j.**
-keep class org.slf4j.** { *; }
