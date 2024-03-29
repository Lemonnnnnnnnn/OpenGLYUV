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
-keep class com.quectel.openglyuv.display.encoder.MediaRecorderThread {
   public *;
}
-keep class com.quectel.openglyuv.display.opengl.CameraSurfaceRender {
   public *;
}
-keep class com.quectel.openglyuv.display.utils.CameraUtil {
   public *;
}
-keep class com.quectel.openglyuv.display.utils.Camera2Helper {
   *;
}
-keep class com.quectel.openglyuv.display.opengl.YUVProgram {
   *;
}
-keep class com.quectel.openglyuv.display.opengl.ShaderHelper {
    *;
 }
-keep class com.quectel.openglyuv.display.opengl.ShaderProgram {
    *;
 }

-keep public interface com.quectel.openglyuv.display.utils.Camera2Helper$onPreviewFrame{ *;}
-keep public interface com.quectel.openglyuv.display.utils.Camera2Helper$onCameraError{ *;}
-keep public class com.quectel.openglyuv.display.utils.Camera2Helper$ImageSaver{ *;}
-keep class com.quectel.openglyuv.display.utils.Camera1Helper {
   public *;
}
-keep class com.quectel.openglyuv.display.utils.SystemPropertiesProxy {
   public *;
}
-keep class com.quectel.openglyuv.display.utils.SDcardUtil {
   public *;
}