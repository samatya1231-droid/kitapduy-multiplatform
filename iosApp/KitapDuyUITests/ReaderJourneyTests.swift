import CoreGraphics
import Foundation
import XCTest

@MainActor
final class ReaderJourneyTests: XCTestCase {
    private let app = XCUIApplication(bundleIdentifier: "com.semtinazizi.kitapduy")
    private let guestName = "Misafir"

    func test01HomeDiscoverAndBookDetail() throws {
        try launchAndSignIn()
        try step("22-home-catalog") {
            try visible(text("Yeni hikâyeler keşfet"))
            audit("22-home-catalog")
        }
        try step("24-discover") {
            try tap(element("tab-discover"))
            try visible(element("discover-query"))
            audit("24-discover")
        }
        try step("24b-discover-categories") {
            try visible(element("discover-category-Polisiye"), scroll: true)
        }
        for _ in 0..<6 {
            if element("discover-query").exists && element("discover-query").isHittable { break }
            app.swipeDown(velocity: .slow)
        }
        try step("03-search-empty-result") {
            try search("zxq-no-catalog-result")
            try visible(text("Kitap veya podcast bulunamadı."))
        }
        try step("04-book-search") {
            try search("Kızıl Dosya", clearFirst: true)
            try visible(element("book-result-0mVxipNVFmBxtJNs9O0J"))
        }
        try step("05-book-detail") {
            try openBookResult()
            audit("05-book-detail")
        }
        try step("08-book-back-to-search") {
            try tap(text("Geri"))
            try visible(element("discover-query"))
        }
    }

    func test07HomePodcasts() throws {
        try launchAndSignIn()
        try step("23-home-podcasts") {
            try visible(text("Podcast dünyası"), scroll: true)
        }
    }

    func test02BookPlayback() throws {
        try launchAndSignIn()
        try search("Kızıl Dosya", clearFirst: true)
        try openBookResult()
        try step("25-book-player") {
            try visible(element("book-play-pause"), scroll: true)
        }
        try step("06-book-playing") {
            try tap(element("book-play-pause"), scroll: true)
            try visible(element("book-elapsed"), scroll: true)
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
    }

    func test03PodcastPlayback() throws {
        try launchAndSignIn()
        try step("09-podcast-episodes") {
            try search("Barış Özcan", clearFirst: element("discover-clear").exists)
            try tap(element("podcast-result-podcast_111hz"))
            try visible(element("podcast-episode-0"))
        }
        try step("26-podcast-player") {
            try tap(element("podcast-episode-0"))
            try visible(element("podcast-play-pause"), scroll: true)
            try visible(element("podcast-elapsed"), scroll: true)
        }
        try step("10-podcast-playing") {
            try waitFor("Podcast konumu ilerlemeli") { self.elapsed("podcast-elapsed") >= 2 }
        }
        try step("31-podcast-back-to-search") {
            try tap(element("podcast-play-pause"))
            try tap(text("Geri"))
            try visible(element("podcast-episode-0"))
            try tap(text("Geri"))
            try visible(element("discover-query"))
        }
    }

    func test04Library() throws {
        try launchAndSignIn()
        try step("27-library-favorites") {
            try tap(element("tab-library"))
            try visible(text("Kitaplığım"))
            try tap(element("library-tab-0"))
            try visible(text("Favoriler listeniz şu an boş."))
            audit("27-library-favorites")
        }
        try step("11-library-downloads-empty") {
            try tap(element("library-tab-1"))
            try visible(text("İndirilenler listeniz şu an boş."))
        }
        try step("28-library-history") {
            try tap(element("library-tab-2"))
            try visible(text("Geçmiş"))
        }
        try step("12-library-shelves") {
            try tap(element("library-shelves"))
            try visible(text("Bitirdiklerim"))
            try tap(text("Bitirdiklerim"))
            try visible(text("Bu raf henüz boş."))
        }
    }

    func test05FeedAndProfile() throws {
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
        try step("29-profile") {
            try tap(element("tab-profile"))
            try visible(text("Okur köşem"))
            audit("29-profile")
        }
        try step("16-profile-edit") {
            try tap(element("profile-edit"), scroll: true)
            try visible(element("profile-name"))
        }
        try step("17-profile-saved") {
            let field = element("profile-name")
            let updatedName = guestName + " Profil"
            try tap(field)
            field.coordinate(withNormalizedOffset: CGVector(dx: 0.95, dy: 0.5)).tap()
            field.typeText(" Profil")
            field.typeText(XCUIKeyboardKey.return.rawValue)
            try waitFor("Profil klavyesi kapanmalı") { !self.app.keyboards.firstMatch.exists }
            try tap(app.buttons["profile-save"], scroll: true)
            try waitFor("Profil düzenleme ekranı kapanmalı") { !field.exists }
            let name = text(updatedName)
            for _ in 0..<4 {
                if name.exists && name.isHittable { break }
                app.swipeDown(velocity: .slow)
            }
            try visible(name)
        }
    }

    func test06ThemeAndPremium() throws {
        try launchAndSignIn()
        try step("18-settings-light") {
            try tap(element("tab-profile"))
            try tap(element("profile-settings"))
            try visible(element("settings-dark-mode-off"))
            audit("18-settings-light")
        }
        try step("19-settings-dark") {
            try tap(element("settings-dark-mode-off"))
            try visible(element("settings-dark-mode-on"))
            audit("19-settings-dark")
        }
        try step("20-premium") {
            try tap(element("settings-premium"), scroll: true)
            try visible(text("KitapDuy · PREMIUM"))
            audit("20-premium")
        }
        try step("30-premium-terms") {
            try visible(text("Satın Alımları Geri Yükle"), scroll: true)
            try tap(text("Kullanım Koşulları (EULA)"), scroll: true)
            try visible(text("KitapDuy — Kullanım Koşulları", contains: true))
        }
        try step("21-premium-legal") {
            try tap(text("Geri"))
            try visible(text("KitapDuy · PREMIUM"))
            try tap(text("Gizlilik Politikası"), scroll: true)
            try visible(text("KitapDuy — iOS Gizlilik Politikası", contains: true))
        }
    }

    func test08AdvisorArenaAndAskida() throws {
        try launchAndSignIn()
        try step("32-home-advisor") {
            try tap(element("home-advisor-fab"))
            try visible(element("advisor-intent-analysis"))
        }
        try tap(element("advisor-back"))
        try step("33-discover-advisor") {
            try tap(element("tab-discover"))
            try tap(element("discover-advisor-button"))
            try visible(element("advisor-mood-gripping"), scroll: true)
            try tap(element("advisor-mood-gripping"))
            try visible(text("Bu Kitabı Dinle", contains: true), scroll: true)
        }
        try tap(element("advisor-back"))
        try step("34-arena-menu") {
            try tap(element("discover-arena-card"), scroll: true)
            try visible(element("quiz-start-classic"))
        }
        try step("35-arena-playing") {
            try tap(element("quiz-start-classic"))
            try visible(element("quiz-question-text"))
            try visible(element("quiz-option-0"))
        }
        try tap(element("quiz-option-0"))
        try tap(text("Geri"))
        if element("quiz-start-classic").exists {
            try tap(text("Geri"))
        }
        try step("36-askida-list") {
            if !element("discover-askida-card").exists {
                try tap(element("tab-discover"))
            }
            for _ in 0..<4 {
                if element("discover-askida-card").exists && element("discover-askida-card").isHittable { break }
                app.swipeDown(velocity: .slow)
            }
            try tap(element("discover-askida-card"))
            try visible(element("askida-tab-0"))
            try visible(text("Kürk Mantolu Madonna", contains: true))
        }
        try step("37-askida-donate") {
            try tap(element("askida-tab-1"))
            try visible(element("askida-donate-title"))
        }
    }

    private func openBookResult() throws {
        try tap(element("book-result-0mVxipNVFmBxtJNs9O0J"))
        try visible(text("Sherlock Holmes - Kızıl Dosya"))
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
        let timer = element(id)
        for text in [timer.label, timer.value as? String ?? ""] {
            let parts = text.split(separator: ":", omittingEmptySubsequences: false)
            let numbers = parts.compactMap { Int($0) }
            if numbers.count == parts.count && (numbers.count == 2 || numbers.count == 3) {
                return numbers.reduce(0) { $0 * 60 + $1 }
            }
        }
        return -1
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
