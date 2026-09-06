import SwiftUI
import ComposeApp

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        return true
    }
    
    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {}
    
    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {}
}

@main
struct KitapDuyApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    
    init() {
        MainViewControllerKt.initializeIOSConfig(
            supabaseUrl: Bundle.main.infoDictionary?["SUPABASE_URL"] as? String ?? "",
            supabaseKey: Bundle.main.infoDictionary?["SUPABASE_KEY"] as? String ?? "",
            cfAccountId: Bundle.main.infoDictionary?["CF_ACCOUNT_ID"] as? String ?? "",
            cfPublicUrl: Bundle.main.infoDictionary?["CF_PUBLIC_URL"] as? String ?? ""
        )
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea(.keyboard)
                // kitapduy://login-callback OAuth don us URL isleyici
                .onOpenURL { url in
                    MainViewControllerKt.handleOAuthDeepLink(url: url.absoluteString)
                }
        }
    }
}
