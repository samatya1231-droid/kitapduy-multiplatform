import AuthenticationServices
import CryptoKit
import UIKit
import ComposeApp

@MainActor
final class AuthenticationService: NSObject,
    ASWebAuthenticationPresentationContextProviding,
    ASAuthorizationControllerDelegate,
    ASAuthorizationControllerPresentationContextProviding {

    static let shared = AuthenticationService()

    // MARK: - Web OAuth (Google) session
    private var session: ASWebAuthenticationSession?
    private var window: UIWindow?

    // MARK: - Native Apple Sign In state
    private var currentNonce: String?
    private var appleAuthController: ASAuthorizationController?

    private func resolvePresentationWindow() -> UIWindow? {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        let activeScene = scenes.first(where: { $0.activationState == .foregroundActive }) ?? scenes.first
        return activeScene?.windows.first(where: { $0.isKeyWindow })
            ?? activeScene?.windows.first
            ?? scenes.flatMap({ $0.windows }).first(where: { $0.isKeyWindow })
            ?? scenes.flatMap({ $0.windows }).first
    }

    // MARK: - Setup
    func configure() {
        // Google / OAuth URL flow köprüsü
        IosAuthenticationBridge.shared.open = { url in
            Task { @MainActor in AuthenticationService.shared.signInWithUrl(url: url) }
        }

        // Native Apple Sign In köprüsü
        IosAppleSignInBridge.shared.startSignIn = {
            Task { @MainActor in AuthenticationService.shared.startNativeAppleSignIn() }
        }
    }

    // MARK: - Google / OAuth URL (ASWebAuthenticationSession)
    private func signInWithUrl(url: String) {
        guard session == nil else { return }
        guard let authURL = URL(string: url), authURL.scheme == "https",
              let window = resolvePresentationWindow() else {
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
        self.session = session
        if !session.start() {
            self.session = nil
            self.window = nil
            IosAuthenticationBridge.shared.fail(message: "Giriş penceresi başlatılamadı.")
        }
    }

    func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
        window ?? resolvePresentationWindow() ?? ASPresentationAnchor()
    }

    // MARK: - Native Apple Sign In (ASAuthorizationController)
    private func startNativeAppleSignIn() {
        let nonce = randomNonceString()
        currentNonce = nonce

        let appleIDProvider = ASAuthorizationAppleIDProvider()
        let request = appleIDProvider.createRequest()
        request.requestedScopes = [.fullName, .email]
        request.nonce = sha256(nonce)

        let authorizationController = ASAuthorizationController(authorizationRequests: [request])
        authorizationController.delegate = self
        authorizationController.presentationContextProvider = self
        self.appleAuthController = authorizationController
        authorizationController.performRequests()
    }

    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        resolvePresentationWindow() ?? window ?? UIWindow()
    }

    // MARK: - ASAuthorizationControllerDelegate
    func authorizationController(controller: ASAuthorizationController,
                                 didCompleteWithAuthorization authorization: ASAuthorization) {
        appleAuthController = nil
        guard let appleIDCredential = authorization.credential as? ASAuthorizationAppleIDCredential,
              let identityTokenData = appleIDCredential.identityToken,
              let identityToken = String(data: identityTokenData, encoding: .utf8),
              let rawNonce = currentNonce else {
            currentNonce = nil
            IosAppleSignInBridge.shared.fail(message: "Apple kimlik bilgileri alınamadı. Lütfen tekrar deneyin.")
            return
        }
        currentNonce = nil
        // Kotlin tarafına ilet — Supabase IDToken akışı burada başlar
        IosAppleSignInBridge.shared.handleAppleSignIn(identityToken: identityToken, rawNonce: rawNonce)
    }

    func authorizationController(controller: ASAuthorizationController,
                                 didCompleteWithError error: Error) {
        appleAuthController = nil
        currentNonce = nil
        if (error as? ASAuthorizationError)?.code == .canceled { return }
        IosAppleSignInBridge.shared.fail(message: "Apple ile giriş tamamlanamadı. Lütfen tekrar deneyin.")
    }

    // MARK: - Nonce Helpers
    private func randomNonceString(length: Int = 32) -> String {
        precondition(length > 0)
        var randomBytes = [UInt8](repeating: 0, count: length)
        let errorCode = SecRandomCopyBytes(kSecRandomDefault, randomBytes.count, &randomBytes)
        if errorCode != errSecSuccess {
            fatalError("Unable to generate nonce. SecRandomCopyBytes failed with OSStatus \(errorCode)")
        }
        let charset: [Character] = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._")
        let nonce = randomBytes.map { byte in
            charset[Int(byte) % charset.count]
        }
        return String(nonce)
    }

    private func sha256(_ input: String) -> String {
        let inputData = Data(input.utf8)
        let hashedData = SHA256.hash(data: inputData)
        return hashedData.compactMap { String(format: "%02x", $0) }.joined()
    }
}
