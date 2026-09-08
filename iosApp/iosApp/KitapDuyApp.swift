import SwiftUI
import UIKit
import ComposeApp

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        // iOS 26.x Liquid Glass: CUIDesignLibraryCatalog'u ana thread'de ön-yükle.
        // Bu olmadan SwiftUI'ın Color.resolve çağrısı SIGABRT ile çöküyor.
        // Crash analizi: iki ayrı log'da aynı stack: stat() → CUIDesignLibraryCatalog → Color.resolve.
        let traits = UITraitCollection.current
        _ = UIColor.systemBackground.resolvedColor(with: traits)
        _ = UIColor.label.resolvedColor(with: traits)
        return true
    }
    
    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {}
    
    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {}
}

@main
@MainActor
struct KitapDuyApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    @Environment(\.scenePhase) private var scenePhase
    
    init() {
        MainViewControllerKt.initializeIOSConfig(
            supabaseUrl: Bundle.main.infoDictionary?["SUPABASE_URL"] as? String ?? "",
            supabaseKey: Bundle.main.infoDictionary?["SUPABASE_KEY"] as? String ?? "",
            cfAccountId: Bundle.main.infoDictionary?["CF_ACCOUNT_ID"] as? String ?? "",
            cfPublicUrl: Bundle.main.infoDictionary?["CF_PUBLIC_URL"] as? String ?? ""
        )
        AuthenticationService.shared.configure()
        StoreService.shared.configure()
        AdMobBridge.shared.configure()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea(.keyboard)
                .onAppear { AdMobBridge.shared.requestConsent() }
                // kitapduy://login-callback OAuth don us URL isleyici
                .onOpenURL { url in
                    MainViewControllerKt.handleOAuthDeepLink(url: url.absoluteString)
                }
                .onChange(of: scenePhase) { phase in
                    if phase == .active {
                        AdMobBridge.shared.requestConsent()
                        Task { await StoreService.shared.refreshEntitlements() }
                    }
                }
        }
    }
}
