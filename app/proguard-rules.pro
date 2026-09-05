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

# 릴리즈 크래시에서 줄 번호를 복원하려면 필요하다. 이것이 없으면 스택트레이스가 파일·줄 없이
# 남아 난독화 매핑을 갖고도 어디서 터졌는지 알 수 없다.
-keepattributes SourceFile,LineNumberTable

# 원본 파일명은 감춘다. 위에서 줄 번호를 남기더라도 소스 파일 이름까지 노출할 이유는 없다.
-renamesourcefileattribute SourceFile
