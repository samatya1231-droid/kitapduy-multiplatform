import SwiftUI
import UIKit
import ComposeApp

/**
 * iOS ContentView - KMP Compose UI'ı bir UIViewControllerRepresentable ile sararak SwiftUI'ya entegre eder
 *
 * iOS 26.x (Liquid Glass) Color.resolve SIGABRT crash root cause:
 * Apple'ın crash log analizi (Build 13, iki farklı log):
 *   Thread 0 (main): Color.resolve(in:)
 *     → UIKitPlatformColorDefinition.resolvedHDRColor
 *     → UIDynamicColor.resolvedColorWithTraitCollection
 *     → CUIDesignLibrary.colorWithTraits
 *     → CUIDesignLibraryCatalog.catalogForDesignSystem  ← crash burada: stat() hata
 *     → UIKitPlatformViewHost.init  ← Compose'un UIKit entegrasyon noktası
 *     → PlatformViewChild.updateValue()
 *
 * Teşhis: iOS 26'da CUIDesignLibraryCatalog lazy-init ediliyor.
 * SwiftUI, UIViewControllerRepresentable içinde Color.resolve çağrısı yaparken
 * catalog henüz dosyadan yüklenmemiş oluyor → stat() kilitlenmesi → SIGABRT.
 *
 * Çözüm: UIWindow üzerinden traitCollection'ı ve resolvedColor'ı
 * ilk render öncesinde ana thread'de ısıtıyoruz.
 */
struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.all)
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // iOS 26.x crash fix: CUIDesignLibraryCatalog'u ön-yüklüyoruz.
        // UITraitCollection.current üzerinden system rengi resolve edilince
        // catalog dosyaya erişim (stat) main thread'de tamamlanır.
        // Bu sayede SwiftUI'ın Color.resolve çağrısı çökmüyor.
        warmUpDesignLibrary()
        
        let vc = MainViewControllerKt.MainViewController()
        return vc
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}

    /// iOS 26+ Liquid Glass design catalog ön-yükleme.
    /// Bu metod yalnızca bir kez etkili olur (sonraki çağrılar no-op).
    private func warmUpDesignLibrary() {
        let traits = UITraitCollection.current
        // UIColor.systemBackground üzerinden catalog yüklenir.
        // Hata olursa zaten crash yapmaz, sessizce devam eder.
        _ = UIColor.systemBackground.resolvedColor(with: traits)
        _ = UIColor.label.resolvedColor(with: traits)
    }
}
