# X Heart Animation — Kotlin / Android

Ứng dụng Android native độc lập, chuyển từ `src/components/x-heart-animation` và `App.tsx`. Màn hình đen, nút tim nằm giữa toàn bộ viewport, canvas 58dp và tim 26dp. Giữ hướng dọc như `app.json`; Android 16 có thể bỏ qua khóa hướng trên màn hình lớn. Không cần Expo, React Native hay Compose.

## Mở và chạy sau

1. Trong Android Studio, chọn **Open** rồi mở thư mục `android-kotlin`.
2. Chờ Gradle Sync. Cài Android SDK Platform 36 nếu IDE yêu cầu; dùng Gradle JDK 17 hoặc 21.
3. Chọn máy ảo hoặc thiết bị Android 6.0 / API 23 trở lên, rồi chạy module `app`.

Gradle Wrapper đã có đủ `gradlew`, `gradlew.bat`, JAR và SHA-256 của bản phân phối chính thức. Toolchain được ghim: AGP 8.11.1, Kotlin 2.2.21, Gradle 8.14.3, compile/target SDK 36, bytecode JVM 17. Các phiên bản nằm trong bảng tương thích của [Android](https://developer.android.com/build/releases/agp-8-11-0-release-notes) và [Kotlin](https://kotlinlang.org/docs/gradle-configure-project.html).

Build và kiểm tra bằng terminal, không cần mở máy ảo:

```sh
cd android-kotlin
./gradlew assembleDebug testDebugUnitTest lint
```

Nếu Gradle không tìm được SDK, để Android Studio tự tạo `local.properties`, hoặc cấu hình `ANDROID_HOME` theo đường dẫn SDK của máy. APK debug được tạo ở `app/build/outputs/apk/debug/app-debug.apk`; các thư mục build và cấu hình máy cục bộ đã được bỏ qua bởi Git.

## Component dùng lại

```kotlin
val heart = XHeartAnimationView(
    context = context,
    initialLiked = false,
    onToggle = { liked -> /* cập nhật trạng thái của ứng dụng */ },
    debugMode = false,
)

heart.setLiked(true, animate = true) // cập nhật từ ngoài, mặc định không gọi callback
heart.setLiked(false, animate = false) // đặt ngay trạng thái cuối
heart.onToggle = { liked -> /* ... */ }
heart.debugMode = true // tắt tương tác giống prop debugMode của bản gốc
```

`XHeartAnimationView.kt` vẽ bằng `Canvas` / `Path` và điều khiển bằng `ValueAnimator`. `HeartAnimationMath.kt` chứa tham số chuyển động, màu và vị trí 14 hạt. `MainActivity.kt` là màn hình demo; nút có mô tả và trạng thái checkable cho TalkBack, hỗ trợ click từ bàn phím. Trạng thái like được giữ khi Activity tạo lại.

## Đối chiếu hành vi

- Dùng nguyên các lệnh cubic của SVG tim gốc, cùng thứ tự vẽ: vòng tròn → hạt → tim. Stroke 2 được biến đổi cùng ma trận SVG 26/24.
- Like 1000ms với cubic Bézier `(0.21, 0.61, 0.35, 1)`. Unlike 200ms với `Easing.ease` của Reanimated 4.1.1, tức Bézier `(0.42, 0, 1, 1)`.
- Giữ 7 cặp hạt, lệch góc 40°, bán kính nhóm 20 và khoảng cách hạt 2. Độ trễ cặp thứ hai là `0.08` trừ trực tiếp trên progress đã easing. Màu nội suy RGB không gamma, có clamp giống Skia.
- Vòng tròn dùng fill even-odd, bán kính ngoài 16; vòng và hạt ẩn ngay khi unlike. Bấm liên tiếp sẽ hủy animator cũ và tiếp tục từ progress đang hiển thị.
- Chỉ khi like mới rung: waveform Android của `expo-haptics` Medium là 43ms, amplitude 50; Android dưới API 26 dùng pattern 43ms. Manifest có quyền `VIBRATE`.
- Giữ hai đặc điểm thực tế của source: scale tim cuối là **1.15** vì output scale cuối thừa không được dùng; 7 hạt thuộc cặp thứ hai còn rất nhỏ sau khi like kết thúc (`r ≈ 0.427dp`, opacity `0.2`). Không tự sửa các đặc điểm này.

`HeartAnimationParityTest.kt` so các mốc progress, easing, màu RGB và 14 tọa độ hạt với snapshot lấy từ phép tính của source React Native. Đã cài và chạy demo trên máy ảo Android API 36; kết quả tương tác và video nằm trong [báo cáo QA](../docs/qa/README.md).
