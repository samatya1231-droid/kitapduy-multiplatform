import SwiftUI
import ComposeApp

@main
struct KitapDuyApp: App {
    
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
