import SwiftUI
import ComposeApp

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        // OneSignal Initialization will go here
        // OneSignal.initialize("4bc4bc20-ea45-4c6f-9b22-e2dfca6beac5", withLaunchOptions: launchOptions)
        return true
    }
    
    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        // APNs token received
    }
    
    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        // APNs token error
    }
}

@main
struct KitapDuyApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    
    init() {
        // Kotlin/KMP tarafına config değerlerini gönder
        // Bu değerler Codemagic'te environment variables olarak set edilir
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
        }
    }
}
