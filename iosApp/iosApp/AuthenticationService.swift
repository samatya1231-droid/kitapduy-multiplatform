import AuthenticationServices
import UIKit
import ComposeApp

@MainActor
final class AuthenticationService: NSObject, ASWebAuthenticationPresentationContextProviding {
    static let shared = AuthenticationService()
    private var session: ASWebAuthenticationSession?
    private var window: UIWindow?

    func configure() {
        IosAuthenticationBridge.shared.open = { url in
            Task { @MainActor in AuthenticationService.shared.signIn(url: url) }
        }
    }

    private func signIn(url: String) {
        guard session == nil else { return }
        guard let authURL = URL(string: url), authURL.scheme == "https",
              let window = UIApplication.shared.connectedScenes.compactMap({ $0 as? UIWindowScene })
                .flatMap({ $0.windows }).first(where: { $0.isKeyWindow }) else {
            IosAuthenticationBridge.shared.fail(message: "Giriş penceresi açılamadı. Lütfen tekrar deneyin.")
            return
        }
        self.window = window
        let session = ASWebAuthenticationSession(url: authURL, callbackURLScheme: "kitapduy") { [weak self] callback, error in
            Task { @MainActor in
                self?.session = nil
                self?.window = nil
                if let callback {
                    IosAuthenticationBridge.shared.handleCallback(url: callback.absoluteString)
                } else if (error as? ASWebAuthenticationSessionError)?.code != .canceledLogin {
                    IosAuthenticationBridge.shared.fail(message: "Giriş tamamlanamadı. Lütfen tekrar deneyin.")
                }
            }
        }
        session.presentationContextProvider = self
        self.session = session // ASWebAuthenticationSession must be retained until completion.
        if !session.start() {
            self.session = nil
            self.window = nil
            IosAuthenticationBridge.shared.fail(message: "Giriş penceresi başlatılamadı.")
        }
    }

    func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
        window ?? ASPresentationAnchor()
    }
}
