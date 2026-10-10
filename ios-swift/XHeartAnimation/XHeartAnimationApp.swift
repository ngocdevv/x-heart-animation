import SwiftUI

@main
struct XHeartAnimationApp: App {
    var body: some Scene {
        WindowGroup {
            ZStack {
                Color.black
                XHeartAnimation(initialLiked: false) { liked in
                    print("Liked: \(liked)")
                }
                .frame(width: 58, height: 58)
            }
            .ignoresSafeArea()
            .preferredColorScheme(.dark)
        }
    }
}
