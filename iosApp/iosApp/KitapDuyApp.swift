import SwiftUI
import UIKit
import ComposeApp
import OneSignalFramework

@MainActor
class AppDelegate: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        // iOS 26.x Liquid Glass: CUIDesignLibraryCatalog'u ana thread'de ön-yükle.
        // Bu olmadan SwiftUI'ın Color.resolve çağrısı SIGABRT ile çöküyor.
        // Crash analizi: iki ayrı log'da aynı stack: stat() → CUIDesignLibraryCatalog → Color.resolve.
        let traits = UITraitCollection.current
        _ = UIColor.systemBackground.resolvedColor(with: traits)
        _ = UIColor.label.resolvedColor(with: traits)
        PushService.shared.configure(launchOptions: launchOptions)
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
                        PushService.shared.refreshPermission()
                        Task { await StoreService.shared.refreshEntitlements() }
                    }
                }
        }
    }
}


@MainActor
final class PushService: NSObject, OSNotificationPermissionObserver, OSPushSubscriptionObserver {
    static let shared = PushService()
    private var configured = false

    func configure(launchOptions: [UIApplication.LaunchOptionsKey: Any]?) {
        guard !configured,
              let appId = Bundle.main.object(forInfoDictionaryKey: "ONESIGNAL_APP_ID") as? String,
              UUID(uuidString: appId) != nil else { return }
        configured = true
        OneSignal.initialize(appId, withLaunchOptions: launchOptions)
        OneSignal.Notifications.addPermissionObserver(self)
        OneSignal.User.pushSubscription.addObserver(self)
        PushNotificationBridge.shared.configureIdentity { userId in
            // FIFO main-queue delivery preserves rapid login/logout/account-switch order.
            DispatchQueue.main.async {
                if let userId = userId, !userId.isEmpty {
                    OneSignal.login(userId)
                } else {
                    OneSignal.logout()
                }
            }
        }
        PushNotificationBridge.shared.enableNotifications = {
            DispatchQueue.main.async { PushService.shared.enable() }
        }
        PushNotificationBridge.shared.disableNotifications = {
            DispatchQueue.main.async {
                OneSignal.User.pushSubscription.optOut()
                PushService.shared.refreshPermission()
            }
        }
        PushNotificationBridge.shared.refreshPermission = {
            DispatchQueue.main.async { PushService.shared.refreshPermission() }
        }
        refreshPermission()
    }

    private func enable() {
        OneSignal.Notifications.requestPermission({ accepted in
            DispatchQueue.main.async {
                if accepted { OneSignal.User.pushSubscription.optIn() }
                PushService.shared.refreshPermission()
            }
        }, fallbackToSettings: true)
    }

    func refreshPermission() {
        guard configured else { return }
        PushNotificationBridge.shared.updatePermission(
            enabled: OneSignal.Notifications.permission && OneSignal.User.pushSubscription.optedIn)
    }

    nonisolated func onNotificationPermissionDidChange(_ granted: Bool) {
        DispatchQueue.main.async { PushService.shared.refreshPermission() }
    }

    nonisolated func onPushSubscriptionDidChange(state: OSPushSubscriptionChangedState) {
        DispatchQueue.main.async { PushService.shared.refreshPermission() }
    }
}
