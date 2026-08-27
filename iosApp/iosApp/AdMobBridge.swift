import Foundation
import UIKit
import GoogleMobileAds
import ComposeApp

class AdMobBridge: NSObject, GADFullScreenContentDelegate {
    static let shared = AdMobBridge()
    private var interstitial: GADInterstitialAd?

    static func setup() {
        // Kotlin tarafındaki Banner factory'sini ata
        BannerAdFactory.shared.createBanner = {
            let bannerView = GADBannerView(adSize: GADAdSizeBanner)
            // KMP AdManager.ios.kt'deki ID'yi alıyoruz
            bannerView.adUnitID = AdManager().bannerAdUnitId

            // Root View Controller bulma
            if let rootVC = UIApplication.shared.connectedScenes
                .filter({ $0.activationState == .foregroundActive })
                .compactMap({ $0 as? UIWindowScene })
                .first?.windows
                .filter({ $0.isKeyWindow }).first?.rootViewController {
                bannerView.rootViewController = rootVC
            }

            bannerView.load(GADRequest())
            return bannerView
        }

        // Interstitial bridge'leri ata
        AdMobSwiftBridge.shared.loadInterstitialAd = {
            shared.loadInterstitial()
        }

        AdMobSwiftBridge.shared.showInterstitialAdIfReady = {
            shared.showInterstitial()
        }
    }

    func loadInterstitial() {
        let request = GADRequest()
        GADInterstitialAd.load(withAdUnitID: AdManager().interstitialAdUnitId,
                                request: request,
                                completionHandler: { [weak self] ad, error in
            if let error = error {
                print("Failed to load interstitial ad with error: \(error.localizedDescription)")
                return
            }
            self?.interstitial = ad
            self?.interstitial?.fullScreenContentDelegate = self
        })
    }

    func showInterstitial() {
        if let ad = interstitial,
           let rootVC = UIApplication.shared.connectedScenes
            .filter({ $0.activationState == .foregroundActive })
            .compactMap({ $0 as? UIWindowScene })
            .first?.windows
            .filter({ $0.isKeyWindow }).first?.rootViewController {
            ad.present(fromRootViewController: rootVC)
        } else {
            print("Ad wasn't ready")
            loadInterstitial()
        }
    }

    // GADFullScreenContentDelegate methods
    func adDidDismissFullScreenContent(_ ad: GADFullScreenPresentingAd) {
        interstitial = nil
        loadInterstitial() // Bir sonrakini yükle
    }
}
