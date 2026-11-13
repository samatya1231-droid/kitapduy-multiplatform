import CoreGraphics
import Foundation
import XCTest

@MainActor
final class ReaderJourneyTests: XCTestCase {
    private let app = XCUIApplication(bundleIdentifier: "com.semtinazizi.kitapduy")

    func test01GuestAndBookPlayback() throws {
        try launchAndSignIn()
        try step("03-search-empty-result") {
            try search("zxq-no-catalog-result")
            try visible(text("Kitap veya podcast bulunamadı."))
        }
        try step("04-book-search") {
            try search("Deneme Kitabı", clearFirst: true)
            try visible(element("book-result-local_ci_book"))
        }
        try step("05-book-detail") {
            try tap(element("book-result-local_ci_book"))
            try visible(text("KitapDuy Deneme Kitabı"))
            audit("05-book-detail")
        }
        try step("06-book-playing") {
            try tap(element("book-play-pause"), scroll: true)
            try waitFor("Sesli kitap konumu ilerlemeli") { self.elapsed("book-elapsed") >= 2 }
        }
        try step("07-book-pause-and-skip") {
            try tap(element("book-play-pause"))
            // Waiting for a stable playback timestamp catches a pause button that does nothing.
            let paused = elapsed("book-elapsed")
            RunLoop.current.run(until: Date(timeIntervalSinceNow: 2.5))
            guard paused >= 0, abs(elapsed("book-elapsed") - paused) <= 1 else {
                throw JourneyError("Duraklatıldıktan sonra oynatma konumu ilerledi")
            }
            let beforeSkip = elapsed("book-elapsed")
            try tap(element("book-skip-forward"))
            try waitFor("15 saniye ileri atlama") { abs(self.elapsed("book-elapsed") - beforeSkip - 15) <= 1 }
            try tap(element("book-skip-back"))
            try waitFor("15 saniye geri atlama") { abs(self.elapsed("book-elapsed") - beforeSkip) <= 1 }
        }
        try step("08-book-back-to-search") {
            try tap(text("Geri"))
            try visible(element("discover-query"))
        }
    }

    func test02PodcastAndLibrary() throws {
        try launchAndSignIn()
        try step("09-podcast-episodes") {
            try search("Deneme Podcasti", clearFirst: element("discover-clear").exists)
            try tap(element("podcast-result-ci_podcast"))
            try visible(element("podcast-episode-0"))
        }
        try step("10-podcast-playing") {
            try tap(element("podcast-episode-0"))
            try visible(element("podcast-play-pause"), scroll: true)
            try waitFor("Podcast konumu ilerlemeli") { self.elapsed("podcast-elapsed") >= 2 }
            try tap(element("podcast-play-pause"))
            try tap(text("Geri"))
            try visible(element("podcast-episode-0"))
            try tap(text("Geri"))
        }
        try step("11-library-downloads-empty") {
            try tap(element("tab-library"))
            try visible(text("Kitaplığım"))
            try tap(element("library-tab-1"))
            try visible(text("İndirilenler listeniz şu an boş."))
        }
        try step("12-library-shelves") {
            try tap(element("library-shelves"))
            try visible(text("Bitirdiklerim"))
            try tap(text("Bitirdiklerim"))
            try visible(text("Bu raf henüz boş."))
        }
    }

    func test03FeedAndProfile() throws {
        try launchAndSignIn()
        try step("13-feed") {
            try tap(element("tab-feed"))
            try visible(text("Edebiyat Duvarı"))
            audit("13-feed")
        }
        try step("14-feed-compose") {
            try tap(element("feed-compose"))
            try tap(element("feed-compose-0"))
            try visible(element("feed-post-text"))
        }
        try step("15-feed-published") {
            try type("Simulator deneme paylasimi", into: element("feed-post-text"))
            try tap(element("feed-post-submit"))
            try visible(text("Simulator deneme paylasimi", contains: true))
            try waitFor("Yazı penceresi kapanmalı") { !self.element("feed-post-text").exists }
        }
        try step("16-profile-edit") {
            try tap(element("tab-profile"))
            try visible(text("Okur köşem"))
            try tap(element("profile-edit"), scroll: true)
            try visible(element("profile-name"))
        }
        try step("17-profile-saved") {
            let field = element("profile-name")
            let previous = try XCTUnwrap(field.value as? String)
            try tap(field)
            field.coordinate(withNormalizedOffset: CGVector(dx: 0.95, dy: 0.5)).tap()
            field.typeText(" QA")
            try tap(element("profile-save"), scroll: true)
            try visible(text(previous + " QA"), scroll: true)
        }
    }

    func test04ThemeAndPremium() throws {
        try launchAndSignIn()
        try step("18-settings-light") {
            try tap(element("tab-profile"))
            try tap(element("profile-settings"))
            try visible(element("settings-dark-mode"))
            audit("18-settings-light")
        }
        try step("19-settings-dark") {
            let toggle = element("settings-dark-mode")
            let before = String(describing: toggle.value)
            try tap(toggle)
            try waitFor("Koyu tema anahtarı durum değiştirmeli") {
                toggle.value != nil && String(describing: toggle.value) != before
            }
            audit("19-settings-dark")
        }
        try step("20-premium") {
            try tap(element("settings-premium"), scroll: true)
            try visible(text("KitapDuy · PREMIUM"))
            audit("20-premium")
        }
        try step("21-premium-legal") {
            try visible(text("Satın Alımları Geri Yükle"), scroll: true)
            try tap(text("Kullanım Koşulları (EULA)"), scroll: true)
            try visible(text("KitapDuy — Kullanım Koşulları", contains: true))
            try tap(text("Geri"))
            try visible(text("KitapDuy · PREMIUM"))
            try tap(text("Gizlilik Politikası"), scroll: true)
            try visible(text("KitapDuy — iOS Gizlilik Politikası", contains: true))
        }
    }

    private func launchAndSignIn() throws {
        continueAfterFailure = false
        let fixture = try XCTUnwrap(ProcessInfo.processInfo.environment["KITAPDUY_CI_FIXTURE_URL"])
        let url = try XCTUnwrap(URLComponents(string: fixture))
        guard url.scheme == "http", url.host == "localhost", url.port != nil,
              url.user == nil, url.password == nil, url.path.isEmpty,
              url.query == nil, url.fragment == nil else {
            throw JourneyError("Simülatör için yerel test sunucusu gerekli")
        }
        app.launchEnvironment["KITAPDUY_CI_SMOKE_TEST"] = "1"
        app.launchEnvironment["KITAPDUY_CI_FIXTURE_URL"] = fixture
        app.launchEnvironment["KITAPDUY_CI_SKIP_HTTPS"] = "1"
        app.launch()
        try waitFor("Giriş ekranı veya Ana Sayfa") { self.element("guest-open").exists || self.element("tab-home").exists }
        if element("guest-open").exists {
            try step("01-guest-entry") {
                try tap(element("guest-open"))
                try visible(element("guest-name"))
            }
            try step("02-guest-home") {
                try tap(element("guest-start"))
                try visible(element("tab-home"))
                try visible(text("Yeni hikâyeler keşfet"))
            }
        } else {
            try tap(element("tab-home"))
        }
    }

    private func search(_ query: String, clearFirst: Bool = false) throws {
        if !element("discover-query").exists { try tap(element("tab-discover")) }
        if clearFirst, element("discover-clear").exists { try tap(element("discover-clear")) }
        try type(query, into: element("discover-query"))
    }

    private func elapsed(_ id: String) -> Int {
        let parts = element(id).label.split(separator: ":").compactMap { Int($0) }
        return parts.count == 2 ? parts[0] * 60 + parts[1] : -1
    }

    private func element(_ id: String) -> XCUIElement {
        app.descendants(matching: .any).matching(NSPredicate(format: "identifier == %@", id)).firstMatch
    }

    private func text(_ label: String, contains: Bool = false) -> XCUIElement {
        let predicate = contains ? NSPredicate(format: "label CONTAINS %@", label) : NSPredicate(format: "label == %@", label)
        return app.descendants(matching: .any).matching(predicate).firstMatch
    }

    private func visible(_ target: XCUIElement, scroll: Bool = false) throws {
        if scroll {
            for _ in 0..<8 {
                if target.exists && target.isHittable { return }
                app.swipeUp(velocity: .slow)
            }
        }
        do {
            try waitFor("Erişilebilir öğe görünmeli") { target.exists && target.isHittable }
        } catch {
            throw JourneyError("Erişilebilir öğe görünmedi: \(target.debugDescription)")
        }
    }

    private func tap(_ target: XCUIElement, scroll: Bool = false) throws {
        try visible(target, scroll: scroll)
        try waitFor("Düğme etkin olmalı") { target.isEnabled }
        target.tap()
    }

    private func type(_ input: String, into target: XCUIElement) throws {
        try tap(target)
        target.typeText(input)
    }

    private func waitFor(_ message: String, condition: @escaping () -> Bool) throws {
        let ready = XCTNSPredicateExpectation(predicate: NSPredicate { _, _ in condition() }, object: nil)
        guard XCTWaiter.wait(for: [ready], timeout: 20) == .completed else { throw JourneyError(message) }
    }

    private func step(_ name: String, action: () throws -> Void) throws {
        try XCTContext.runActivity(named: name) { activity in
            do {
                try action()
                attachScreenshot(name, to: activity)
            } catch {
                attachScreenshot("failure-\(name)", to: activity)
                throw JourneyError("\(name): \(error.localizedDescription)")
            }
        }
    }

    private func audit(_ screen: String) {
        guard #available(iOS 17.0, *) else { return }
        XCTContext.runActivity(named: "Accessibility · \(screen)") { activity in
            var findings: [String] = []
            do {
                try app.performAccessibilityAudit(for: [.contrast, .textClipped, .hitRegion, .sufficientElementDescription]) { issue in
                    let label = issue.element?.label ?? ""
                    findings.append("\(issue.compactDescription) [\(label)]\n\(issue.detailedDescription)")
                    return true // Report existing issues without interrupting the journey.
                }
                if findings.isEmpty { findings.append("Apple erişilebilirlik denetimi uyarı üretmedi.") }
            } catch {
                findings.append("Denetim tamamlanamadı: \(error.localizedDescription)")
            }
            let attachment = XCTAttachment(string: findings.joined(separator: "\n\n"))
            attachment.name = "audit-\(screen)"
            attachment.lifetime = .keepAlways
            activity.add(attachment)
        }
    }

    private func attachScreenshot(_ name: String, to activity: XCTActivity) {
        let attachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        activity.add(attachment)
    }
}

private struct JourneyError: LocalizedError {
    let errorDescription: String?
    init(_ message: String) { errorDescription = message }
}
