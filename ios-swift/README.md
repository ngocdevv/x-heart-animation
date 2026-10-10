# X Heart Animation — Swift / iOS

Ứng dụng Swift native độc lập, dùng SwiftUI cho màn hình nền đen và UIKit/Core Graphics để vẽ animation. Không cần React Native, Expo, CocoaPods, Swift Package hay XcodeGen. Yêu cầu iOS 16 trở lên và Xcode có iOS SDK.

## Mở và chạy

1. Mở `XHeartAnimation.xcodeproj` trong thư mục này.
2. Chọn scheme `XHeartAnimation`, rồi chọn một iPhone Simulator.
3. Nhấn Run trong Xcode. Chạm trái tim ở giữa màn hình để thích hoặc bỏ thích.

Simulator không cần cấu hình Signing Team. Nếu chạy trên iPhone thật, chọn Team của anh trong Signing & Capabilities.

## Dùng lại component

Sao chép `XHeartAnimation.swift` và `HeartAnimationModel.swift` vào ứng dụng SwiftUI, rồi dùng:

```swift
XHeartAnimation(
    initialLiked: false,
    onToggle: { liked in
        print("Liked:", liked)
    },
    debugMode: false
)
.frame(width: 58, height: 58)
```

`initialLiked` khởi tạo trạng thái một lần, tương đương `useState(initialLiked)` ở bản gốc. `onToggle` được gọi ngay khi người dùng đổi trạng thái. `debugMode: true` giữ hình nhưng vô hiệu hóa thao tác, giống bản React Native. UIKit cũng có thể dùng trực tiếp `XHeartAnimationControl(initialLiked:)`.

## Thông số giữ theo source gốc

- Canvas 58 × 58 point, SVG trái tim gốc 24 × 24 được scale 26/24 quanh tâm (29, 29). Stroke rộng 2 trước khi áp dụng các scale.
- Thích: 1.000 ms với cubic Bézier (0.21, 0.61, 0.35, 1). Bỏ thích: 200 ms với cubic Bézier (0.42, 0, 1, 1), đúng `Easing.ease` của Reanimated 4.1.1.
- Vòng tròn có bán kính ngoài 16, lỗ trong tăng theo các mốc progress gốc; dùng even-odd fill và đúng màu/opacity.
- 7 nhóm × 2 hạt, bán kính phân bố 20, độ lệch hạt 2, góc lệch 40°. Hạt thứ hai trễ 0.08 trên progress đã easing. Giữ toàn bộ màu và các mốc kích thước, vị trí, opacity.
- Tim viền xám khi progress < 0.03; sau đó chuyển fill đỏ. Khi bỏ thích, ring và hạt ẩn ngay. Chạm liên tiếp hủy đích cũ và tiếp tục từ progress hiện tại.
- Haptic mức medium chỉ khi thích; có trạng thái và thao tác VoiceOver.

Bản React Native truyền 4 mốc input nhưng 5 output cho scale. Reanimated 4.1.1 bỏ qua output cuối dư, nên tim kết thúc ở scale 1.15. Bản Swift giữ đúng hành vi này. Tương tự, hạt thứ hai vẫn còn rất nhỏ và mờ ở progress 1 vì stagger; không tự ẩn hoặc thay công thức sau khi kết thúc.

Video tham chiếu: https://x.com/ngocdevv/status/1982572513445204344. Các công thức được đối chiếu với source React Native; hình ảnh và tương tác trên máy ảo đã được kiểm tra trong [báo cáo QA](../docs/qa/README.md).

## Kiểm tra build không chạy máy ảo

```sh
xcodebuild -project XHeartAnimation.xcodeproj \
  -scheme XHeartAnimation -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath /tmp/x-heart-animation-swift-build \
  CODE_SIGNING_ALLOWED=NO build
```

Lệnh trên chỉ biên dịch ứng dụng; không mở, cài ứng dụng hoặc chạy Simulator.

Đã build thành công với Xcode 27.1 và iOS Simulator SDK 27.1 cho cả arm64 và x86_64. Sau yêu cầu kiểm tra bổ sung, đã chạy trên iPhone 18 Pro / iOS 27.2 và kiểm tra thích, bỏ thích, chạm liên tiếp.
