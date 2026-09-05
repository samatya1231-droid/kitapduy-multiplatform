import SwiftUI
import ComposeApp

/**
 * iOS ContentView - KMP Compose UI'ı bir UIViewControllerRepresentable ile sararak SwiftUI'ya entegre eder
 */
struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.all)
            // iOS 26.x Liquid Glass renk çözümleme crash'ini önler
            .background(Color(UIColor.systemBackground))
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // Kotlin KMP tarafındaki MainViewController fonksiyonunu çağırır
        let vc = MainViewControllerKt.MainViewController()
        // iOS 26.x'te UIKitPlatformViewHost Color.resolve crash'ini önlemek için
        // viewController'ın view'una explicit background set ediyoruz
        vc.view.backgroundColor = UIColor.systemBackground
        return vc
    }
    
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
