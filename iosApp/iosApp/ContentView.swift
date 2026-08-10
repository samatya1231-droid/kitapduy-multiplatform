import SwiftUI
import ComposeApp

/**
 * iOS ContentView - KMP Compose UI'ı bir UIViewControllerRepresentable ile sararak SwiftUI'ya entegre eder
 */
struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.all)
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // Kotlin KMP tarafındaki MainViewController fonksiyonunu çağırır
        MainViewControllerKt.MainViewController()
    }
    
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
