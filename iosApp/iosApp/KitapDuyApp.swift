import SwiftUI
import UIKit
import ComposeApp
import OneSignalFramework
import UserNotifications

@MainActor
class AppDelegate: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        // iOS 26.x Liquid Glass: CUIDesignLibraryCatalog'u ana thread'de ön-yükle.
        // Bu olmadan SwiftUI'ın Color.resolve çağrısı SIGABRT ile çöküyor.
        // Crash analizi: iki ayrı log'da aynı stack: stat() → CUIDesignLibraryCatalog → Color.resolve.
        let traits = UITraitCollection.current
        _ = UIColor.systemBackground.resolvedColor(with: traits)
        _ = UIColor.label.resolvedColor(with: traits)
        if Self.fixtureURL == nil { PushService.shared.configure(launchOptions: launchOptions) }
        #if targetEnvironment(simulator)
        let environment = ProcessInfo.processInfo.environment
        if environment["KITAPDUY_CI_SMOKE_TEST"] == "1", environment["KITAPDUY_CI_SKIP_HTTPS"] != "1" {
            IosNetworkSmokeCheck.shared.start(
                url: environment["KITAPDUY_CI_HTTPS_URL"] ?? "https://example.com/"
            )
        }
        #endif
        return true
    }
    
    // Available only to simulator launches using the loopback server started by the test runner.
    static var fixtureURL: String? {
        #if targetEnvironment(simulator)
        let env = ProcessInfo.processInfo.environment
        if env["KITAPDUY_CI_SMOKE_TEST"] == "1",
           let value = env["KITAPDUY_CI_FIXTURE_URL"],
           let url = URLComponents(string: value),
           url.scheme == "http", url.host == "localhost", url.port != nil,
           url.user == nil, url.password == nil, url.path.isEmpty,
           url.query == nil, url.fragment == nil {
            return value
        }
        #endif
        return nil
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {}
    
    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        NSLog("KitapDuy: APNs registration failed (code %ld)", (error as NSError).code)
    }
}

@main
@MainActor
struct KitapDuyApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    @Environment(\.scenePhase) private var scenePhase
    
    init() {
        let fixtureURL = AppDelegate.fixtureURL
        MainViewControllerKt.initializeIOSConfig(
            supabaseUrl: fixtureURL ?? (Bundle.main.infoDictionary?["SUPABASE_URL"] as? String ?? ""),
            supabaseKey: fixtureURL == nil ? (Bundle.main.infoDictionary?["SUPABASE_KEY"] as? String ?? "") : "simulator-fixture",
            cfAccountId: Bundle.main.infoDictionary?["CF_ACCOUNT_ID"] as? String ?? "",
            cfPublicUrl: fixtureURL ?? (Bundle.main.infoDictionary?["CF_PUBLIC_URL"] as? String ?? "")
        )
        AuthenticationService.shared.configure()
        StoreService.shared.configure()
        AdMobBridge.shared.configure()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea(.keyboard)
                .onAppear {
                    if AppDelegate.fixtureURL == nil { AdMobBridge.shared.requestConsent() }
                    #if targetEnvironment(simulator)
                    let environment = ProcessInfo.processInfo.environment
                    if environment["KITAPDUY_CI_SMOKE_TEST"] == "1" {
                        NSLog("KITAPDUY_CI_UI_APPEARED")
                    }
                    #endif
                }
                // kitapduy://login-callback OAuth don us URL isleyici
                .onOpenURL { url in
                    MainViewControllerKt.handleOAuthDeepLink(url: url.absoluteString)
                }
                .onChange(of: scenePhase) { phase in
                    if phase == .active {
                        if AppDelegate.fixtureURL == nil { AdMobBridge.shared.requestConsent() }
                        PushService.shared.refreshPermission()
                        Task { await StoreService.shared.refreshEntitlements() }
                    }
                }
        }
    }
}


@MainActor
final class PushService: NSObject, OSNotificationPermissionObserver, OSPushSubscriptionObserver, OSNotificationLifecycleListener {
    static let shared = PushService()
    private var configured = false
    private var checkedInitialPermission = false

    func configure(launchOptions: [UIApplication.LaunchOptionsKey: Any]?) {
        guard !configured,
              let appId = Bundle.main.object(forInfoDictionaryKey: "ONESIGNAL_APP_ID") as? String,
              UUID(uuidString: appId) != nil else { return }
        configured = true
        OneSignal.initialize(appId, withLaunchOptions: launchOptions)
        OneSignal.Notifications.addPermissionObserver(self)
        OneSignal.Notifications.addForegroundLifecycleListener(self)
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
        PushNotificationBridge.shared.requestInitialNotifications = {
            Task { @MainActor in await PushService.shared.requestInitialPermission() }
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

    private func requestInitialPermission() async {
        guard configured, !checkedInitialPermission else { return }
        checkedInitialPermission = true
        let settings = await UNUserNotificationCenter.current().notificationSettings()
        guard settings.authorizationStatus == .notDetermined else { return }
        enable(fallbackToSettings: false)
    }

    private func enable(fallbackToSettings: Bool = true) {
        OneSignal.Notifications.requestPermission({ accepted in
            DispatchQueue.main.async {
                if accepted { OneSignal.User.pushSubscription.optIn() }
                PushService.shared.refreshPermission()
            }
        }, fallbackToSettings: fallbackToSettings)
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

    nonisolated func onWillDisplay(event: OSNotificationWillDisplayEvent) {
        event.notification.display()
    }
}
