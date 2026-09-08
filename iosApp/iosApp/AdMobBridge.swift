import Foundation
import UIKit
import GoogleMobileAds
import UserMessagingPlatform
import ComposeApp

@MainActor
final class AdMobBridge: NSObject, FullScreenContentDelegate {
    static let shared = AdMobBridge()
    static let adsChanged = Notification.Name("KitapDuyAdsConsentChanged")
    private var requestedConsent = false
    private var startingSDK = false
    private var sdkReady = false
    private var loadingInterstitial = false
    private var consentGeneration = 0
    private var interstitial: InterstitialAd?
    var canRequestAds: Bool { sdkReady && ConsentInformation.shared.canRequestAds }

    func configure() {
        BannerAdFactory.shared.createBanner = { ConsentBannerView() }
        AdMobSwiftBridge.shared.loadInterstitialAd = {
            Task { @MainActor in AdMobBridge.shared.loadInterstitial() }
        }
        AdMobSwiftBridge.shared.showInterstitialAdIfReady = {
            Task { @MainActor in AdMobBridge.shared.showInterstitial() }
        }
        AdsPrivacyBridge.shared.showOptions = {
            Task { @MainActor in await AdMobBridge.shared.showPrivacyOptions() }
        }
    }

    func requestConsent() {
        guard !requestedConsent, UIApplication.shared.applicationState == .active,
              Self.presenter != nil else { return }
        requestedConsent = true
        ConsentInformation.shared.requestConsentInfoUpdate(with: RequestParameters()) { error in
            Task { @MainActor in
                if error == nil {
                    do { try await ConsentForm.loadAndPresentIfRequired(from: Self.presenter) }
                    catch { print("KitapDuy: consent form unavailable; respecting current consent state") }
                }
                self.updateConsentState()
            }
        }
    }

    private func showPrivacyOptions() async {
        guard ConsentInformation.shared.privacyOptionsRequirementStatus == .required else { return }
        do { try await ConsentForm.presentPrivacyOptionsForm(from: Self.presenter) }
        catch { print("KitapDuy: privacy options could not be presented") }
        updateConsentState()
    }

    private func updateConsentState() {
        AdsPrivacyBridge.shared.updateRequired(
            value: ConsentInformation.shared.privacyOptionsRequirementStatus == .required)
        consentGeneration += 1
        loadingInterstitial = false
        interstitial = nil
        NotificationCenter.default.post(name: Self.adsChanged, object: nil)
        guard ConsentInformation.shared.canRequestAds else { return }
        if sdkReady {
            loadInterstitial()
        } else if !startingSDK {
            startingSDK = true
            // No IDFA/ATT request. Disable publisher first-party ID; request non-personalized ads.
            MobileAds.shared.requestConfiguration.setPublisherFirstPartyIDEnabled(false)
            MobileAds.shared.start { _ in
                Task { @MainActor in
                    self.sdkReady = true
                    NotificationCenter.default.post(name: Self.adsChanged, object: nil)
                    self.loadInterstitial()
                }
            }
        }
    }

    static func adRequest() -> GoogleMobileAds.Request {
        let request = GoogleMobileAds.Request()
        let extras = Extras()
        extras.additionalParameters = ["npa": "1"]
        request.register(extras)
        return request
    }

    private func loadInterstitial() {
        guard canRequestAds, !loadingInterstitial, interstitial == nil else { return }
        loadingInterstitial = true
        let generation = consentGeneration
        InterstitialAd.load(with: ComposeApp.AdManager().interstitialAdUnitId,
                            request: Self.adRequest()) { [weak self] ad, _ in
            Task { @MainActor in
                guard let self, generation == self.consentGeneration else { return }
                self.loadingInterstitial = false
                guard self.canRequestAds else { return }
                self.interstitial = ad
                self.interstitial?.fullScreenContentDelegate = self
            }
        }
    }

    private func showInterstitial() {
        guard canRequestAds, let presenter = Self.presenter else { return }
        guard let ad = interstitial else { loadInterstitial(); return }
        interstitial = nil
        ad.present(from: presenter)
    }

    func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) { loadInterstitial() }
    func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        interstitial = nil
    }

    static var presenter: UIViewController? {
        var controller = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
            .filter { $0.activationState == .foregroundActive }
            .flatMap { $0.windows }.first { $0.isKeyWindow }?.rootViewController
        while let presented = controller?.presentedViewController { controller = presented }
        return controller
    }
}

@MainActor
private final class ConsentBannerView: UIView {
    private var banner: GoogleMobileAds.BannerView?

    init() {
        super.init(frame: .zero)
        NotificationCenter.default.addObserver(self, selector: #selector(reload),
                                               name: AdMobBridge.adsChanged, object: nil)
    }
    required init?(coder: NSCoder) { fatalError("init(coder:) is not supported") }
    deinit { NotificationCenter.default.removeObserver(self) }

    override func didMoveToWindow() {
        super.didMoveToWindow()
        if window != nil { reload() }
        else { banner?.removeFromSuperview(); banner = nil }
    }

    @objc private func reload() {
        banner?.removeFromSuperview()
        banner = nil
        guard window != nil, AdMobBridge.shared.canRequestAds,
              let presenter = AdMobBridge.presenter else { return }
        let view = GoogleMobileAds.BannerView(adSize: AdSizeBanner)
        view.adUnitID = ComposeApp.AdManager().bannerAdUnitId
        view.rootViewController = presenter
        view.translatesAutoresizingMaskIntoConstraints = false
        addSubview(view)
        NSLayoutConstraint.activate([
            view.centerXAnchor.constraint(equalTo: centerXAnchor),
            view.centerYAnchor.constraint(equalTo: centerYAnchor)
        ])
        banner = view
        view.load(AdMobBridge.adRequest())
    }
}
