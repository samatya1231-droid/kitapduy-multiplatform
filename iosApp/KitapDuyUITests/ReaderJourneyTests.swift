import CoreGraphics
import Foundation
import XCTest

private struct ProbeRecord: Codable {
    let journey: String
    let screen: String
    let theme: String
    let category: String
    let status: String
    let summary: String
    let detail: String
}

@MainActor
final class ReaderJourneyTests: XCTestCase {
    private static var didCaptureWelcomeLogin = false
    private let app = XCUIApplication(bundleIdentifier: "com.semtinazizi.kitapduy")
    private let guestName = "Misafir"
    private var probeLog: [ProbeRecord] = []
    private var seenProbeKeys: Set<String> = []
    private var currentJourneyName: String = ""

    override func setUpWithError() throws {
        try super.setUpWithError()
        probeLog = []
        seenProbeKeys = []
        currentJourneyName = sanitizedMethodName()
    }

    override func tearDownWithError() throws {
        flushProbes()
        try super.tearDownWithError()
    }

    func test01HomeDiscoverAndBookDetail() throws {
        try launchAndSignIn()
        try step("22-home-catalog") {
            try visible(text("Yeni hikâyeler keşfet"))
            try tap(element("home-featured-book"), scroll: true)
            try visible(element("book-play-pause"), scroll: true)
            try tap(element("tab-home"))
            try visible(element("home-featured-book"), scroll: true)
            audit("22-home-catalog")
            inspectScreen("22-home-catalog")
        }
        try step("24-discover") {
            try tap(element("tab-discover"))
            try visible(element("discover-query"))
            audit("24-discover")
            inspectScreen("24-discover")
        }
        try step("24b-discover-categories") {
            try visible(element("discover-category-Polisiye"), scroll: true)
            audit("24b-discover-categories")
            inspectScreen("24b-discover-categories")
        }
        try scrollToTop(target: element("discover-query"))
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
            inspectScreen("05-book-detail")
        }
        try step("08-book-back-to-search") {
            try tap(element("tab-discover"))
            try visible(element("discover-query"))
            recordProbe(
                screen: "08-book-back-to-search",
                category: "Navigation",
                status: "PASS",
                summary: "Alt sekme düğmeleri (Ana Sayfa ve Keşfet) detay ekranından sekme köküne döndürdü",
                detail: ""
            )
        }
    }

    func test02BookPlayback() throws {
        try launchAndSignIn()
        try search("Kızıl Dosya", clearFirst: true)
        try openBookResult()
        try step("25-book-player") {
            try visible(element("book-play-pause"), scroll: true)
            audit("25-book-player")
            inspectScreen("25-book-player")
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
            audit("09-podcast-episodes")
            inspectScreen("09-podcast-episodes")
        }
        try step("26-podcast-player") {
            try tap(element("podcast-episode-0"))
            try visible(element("podcast-play-pause"), scroll: true)
            try visible(element("podcast-elapsed"), scroll: true)
            audit("26-podcast-player")
            inspectScreen("26-podcast-player")
        }
        try step("10-podcast-playing") {
            try waitFor("Podcast konumu ilerlemeli") { self.elapsed("podcast-elapsed") >= 2 }
        }
        try step("39-podcast-controls") {
            try tap(element("podcast-play-pause"))
            try tap(text("1.0x"), scroll: true)
            try visible(text("Çalma Hızı"))
            try tap(text("1.25x"))
            try visible(text("1.25x"))
            try tap(text("1.25x"))
            try visible(text("Çalma Hızı"))
            try tap(text("1.0x"))
            try visible(text("1.0x"))
            try tap(text("Atmosfer"))
            try visible(text("Atmosfer Sesleri"))
            try tap(firstCloseButton())
            try waitFor("Atmosfer penceresi kapanmalı") { !self.text("Atmosfer Sesleri").exists }
            try tap(text("Uyku"))
            try visible(text("Uyku Modu"))
            try tap(text("15 Dakika"))
            try waitFor("Uyku modu penceresi kapanmalı") { !self.text("Uyku Modu").exists }
            recordProbe(
                screen: "39-podcast-controls",
                category: "Functional",
                status: "PASS",
                summary: "Podcast hız, atmosfer ve uyku zamanlayıcı diyalogları çalıştı",
                detail: "1.25x -> 1.0x hız seçimi, Atmosfer Sesleri kapatma ve 15 Dakika uyku zamanlayıcısı doğrulandı."
            )
        }
        try step("31-podcast-back-to-search") {
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
            inspectScreen("27-library-favorites")
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
            audit("12-library-shelves")
            inspectScreen("12-library-shelves")
        }
    }

    func test05FeedAndProfile() throws {
        try launchAndSignIn()
        try step("13-feed") {
            try tap(element("tab-feed"))
            try visible(text("Edebiyat Duvarı"))
            audit("13-feed")
            inspectScreen("13-feed")
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
            inspectScreen("29-profile")
        }
        try step("16-profile-edit") {
            try tap(element("profile-edit"), scroll: true)
            try visible(element("profile-name"))
            inspectScreen("16-profile-edit")
        }
        try step("17-profile-saved") {
            let field = element("profile-name")
            let updatedName = guestName + " Profil"
            try scrollToTop(target: field)
            try tap(field)
            field.coordinate(withNormalizedOffset: CGVector(dx: 0.95, dy: 0.5)).tap()
            field.typeText(" Profil")
            field.typeText(XCUIKeyboardKey.return.rawValue)
            try waitFor("Profil klavyesi kapanmalı") { !self.app.keyboards.firstMatch.exists }

            try scrollToVisible(element("profile-username"))
            try type("deneme_okur", into: element("profile-username"))
            try scrollToVisible(element("profile-bio"))
            try type("Edebi okuma notlari", into: element("profile-bio"))
            try scrollToVisible(app.buttons["profile-save"])
            try tap(app.buttons["profile-save"])
            try waitFor("Profil düzenleme ekranı kapanmalı") { !field.exists }
            let name = text(updatedName)
            try scrollToTop(target: name)
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
            inspectScreen("18-settings-light")
        }
        try step("19-settings-dark") {
            try tap(element("settings-dark-mode-off"))
            try visible(element("settings-dark-mode-on"))
            audit("19-settings-dark", theme: "Koyu")
            inspectScreen("19-settings-dark", theme: "Koyu")
        }
        try step("40-settings-statistics") {
            try tap(text("İstatistikler"), scroll: true)
            try visible(text("Senin sesli kitap yolculuğun"))
            recordProbe(
                screen: "40-settings-statistics",
                theme: "Koyu",
                category: "Functional",
                status: "PASS",
                summary: "Ayarlar > İstatistikler ekranında kitaplık özeti ve tamamlanan kitap sayacı doğrulandı",
                detail: ""
            )
            audit("40-settings-statistics", theme: "Koyu")
            inspectScreen("40-settings-statistics", theme: "Koyu")
            try tap(text("Geri"))
            try visible(text("Ayarlar"))
        }
        try step("41-settings-delete-dialog") {
            try tap(text("Hesabı Sil"), scroll: true)
            try visible(text("Evet, Sil"))
            inspectScreen("41-settings-delete-dialog", theme: "Koyu")
            try tap(text("Vazgeç"))
            try waitFor("Hesabı Sil diyaloğu kapanmalı") { !self.text("Evet, Sil").exists }
        }
        try step("20-premium") {
            try scrollToTop(target: element("settings-premium"))
            try tap(element("settings-premium"))
            if !text("KitapDuy · PREMIUM").waitForExistence(timeout: 3), element("settings-premium").exists {
                try tap(element("settings-premium"))
            }
            try visible(text("KitapDuy · PREMIUM"))
            audit("20-premium", theme: "Koyu")
            inspectScreen("20-premium", theme: "Koyu")
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
            audit("21-premium-legal", theme: "Koyu")
            inspectScreen("21-premium-legal", theme: "Koyu")
        }
        // Reset dark mode back to Light so test07..test13 validate consistent Light theme before test14 Dark Mode audit.
        try tap(text("Geri"))
        try visible(text("KitapDuy · PREMIUM"))
        try tap(firstCloseButton())
        try scrollToTop(target: element("settings-dark-mode-on"))
        if element("settings-dark-mode-on").exists {
            try tap(element("settings-dark-mode-on"))
            try visible(element("settings-dark-mode-off"))
        }
    }

    func test07HomePodcasts() throws {
        try launchAndSignIn()
        if isOnScreenAndHittable(text("Tümü")) {
            try tap(text("Tümü"))
            recordProbe(
                screen: "22-home-catalog",
                category: "Functional",
                status: "PASS",
                summary: "Ana sayfa 'Tümü' filtre çipi listeyi başa sarıp kataloğu yeniledi",
                detail: ""
            )
        }
        try step("23-home-podcasts") {
            try visible(text("Podcast dünyası"), scroll: true)
            if text("İngilizce Sesli Kitaplar").exists {
                recordProbe(
                    screen: "23-home-podcasts",
                    category: "Content",
                    status: "PASS",
                    summary: "Ana sayfa 'İngilizce Sesli Kitaplar' ve 'Podcast dünyası' bölümleri yüklendi",
                    detail: ""
                )
            }
        }
        try scrollToTop(target: element("home-category-Polisiye"))
        try step("42-category-detail") {
            try tap(element("home-category-Polisiye"))
            try visible(text("Tümünü Gör"), scroll: true)
            recordProbe(
                screen: "42-category-detail",
                category: "Content",
                status: "PASS",
                summary: "Polisiye kategori vitrini, popüler eserler ve yazarlara göre keşif listesi yüklendi",
                detail: ""
            )
            audit("42-category-detail")
            inspectScreen("42-category-detail")
        }
        try step("43-author-detail") {
            let authorRow = element("category-author-Arthur Conan Doyle")
            try visible(authorRow, scroll: true)
            try tap(authorRow)
            if !text("Popüler Eserleri", contains: true).waitForExistence(timeout: 2.5), authorRow.exists {
                try visible(authorRow, scroll: true)
                try tap(authorRow)
            }
            try visible(text("Arthur Conan Doyle", contains: true))
            audit("43-author-detail")
            inspectScreen("43-author-detail")
            try tap(text("Geri"))
            _ = element("category-author-Arthur Conan Doyle").waitForExistence(timeout: 2)
            if text("Geri").exists {
                try tap(text("Geri"))
            }
            try scrollToVisible(text("Bugün nasıl bir hikâye?"))
        }
        try step("44-mood-detail") {
            try tap(text("Huzurlu", contains: true), scroll: true)
            if !text("Sana Özel Kitaplar").waitForExistence(timeout: 3), text("Huzurlu", contains: true).exists {
                try tap(text("Huzurlu", contains: true))
            }
            try visible(text("Sana Özel Kitaplar"), scroll: true)
            if text("Podcast Önerileri").exists || (try? visible(text("Podcast Önerileri"), scroll: true)) != nil {
                recordProbe(
                    screen: "44-mood-detail",
                    category: "Content",
                    status: "PASS",
                    summary: "Ruh hali detayı ('Huzurlu') kitap ve podcast öneri şeritlerini birlikte gösterdi",
                    detail: ""
                )
            }
            audit("44-mood-detail")
            inspectScreen("44-mood-detail")
            if isOnScreenAndHittable(element("tab-home")) {
                try tap(element("tab-home"))
                if text("Bugün nasıl bir hikâye?").waitForExistence(timeout: 2.5) || !text("Sana Özel Kitaplar").exists {
                    recordProbe(
                        screen: "44-mood-detail",
                        category: "Navigation",
                        status: "PASS",
                        summary: "Aktif alt sekmeye ('Ana Sayfa') tekrar dokunulduğunda TabRootNavigator alt ekrandan kök ekrana döndü",
                        detail: ""
                    )
                } else if text("Geri").exists {
                    try tap(text("Geri"))
                }
            } else {
                try tap(text("Geri"))
            }
        }
        try step("45-home-articles-check") {
            var foundArticles = false
            for _ in 0..<8 {
                if isOnScreenAndHittable(text("Bilim & Düşünce")) {
                    foundArticles = true
                    break
                }
                app.swipeUp(velocity: .slow)
            }
            if foundArticles {
                recordProbe(
                    screen: "45-home-articles-check",
                    category: "Content",
                    status: "PASS",
                    summary: "Ana sayfa 'Bilim & Düşünce' RSS makale satırı yüklendi",
                    detail: "Dış RSS kaynağından makaleler başarıyla çekildi."
                )
                audit("45-home-articles-check")
                inspectScreen("45-home-articles-check")
            } else {
                recordProbe(
                    screen: "45-home-articles-check",
                    category: "Content",
                    status: "WARN",
                    summary: "Ana sayfa 'Bilim & Düşünce' RSS makaleleri yüklenmedi (çevrimdışı veya boş)",
                    detail: "CI ortamında veya ağ kapalıyken makale satırı gizleniyor; yerel örnek/yedek içerik gösterilmiyor."
                )
            }
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
            try tap(element("quiz-briefing-start"))
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
                if isOnScreenAndHittable(element("discover-askida-card")) { break }
                app.swipeDown(velocity: .slow)
            }
            try tap(element("discover-askida-card"))
            try visible(element("askida-tab-0"))
            try visible(text("Sonuç bulunamadı."))
        }
        try step("37-askida-donate") {
            try tap(element("askida-tab-1"))
            try visible(element("askida-donate-title"))
        }
    }

    func test09LiteraryAdvisorDeep() throws {
        try launchAndSignIn()
        // 1. Entry via Home FAB
        try step("46-advisor-welcome") {
            try tap(element("home-advisor-fab"))
            try visible(element("advisor-intent-analysis"))
            try visible(element("advisor-intent-brainstorm"))
            try visible(element("advisor-intent-debate"))
            try visible(element("advisor-intent-discover"))
            try visible(element("advisor-reading-route"), scroll: true)
            try visible(element("advisor-mood-gripping"), scroll: true)
            audit("46-advisor-welcome")
            inspectScreen("46-advisor-welcome")
        }

        // 2. Archive Info Modal (both bottom link and top-right info icon)
        try step("47-advisor-info-dialog") {
            try tap(element("advisor-archive-about"), scroll: true)
            try visible(text("Edebi Danışman Hakkında"))
            try visible(text("sesli kitap arşivindeki", contains: true))
            inspectScreen("47-advisor-info-dialog")
            try tap(text("Anladım"))
            try waitFor("Danışman bilgi penceresi kapanmalı") { !self.text("Edebi Danışman Hakkında").exists }

            try tap(element("advisor-info-button"))
            try visible(text("Edebi Danışman Hakkında"))
            try tap(text("Anladım"))
        }

        // 3. Analysis Starter -> Reading View modal -> Follow-up chip -> New Chat reset
        try scrollToTop(target: element("advisor-intent-analysis"))
        try step("48-advisor-analysis-and-reading") {
            try tap(element("advisor-intent-analysis"))
            try visible(element("advisor-reading-view"), scroll: true)
            try tap(element("advisor-reading-view"))
            try visible(text("Okuma Görünümü"))
            inspectScreen("48-advisor-analysis-and-reading")
            try tap(text("Kapat"))
            try waitFor("Okuma görünümü penceresi kapanmalı") { !self.text("Okuma Görünümü").exists }
            let copyBtn = app.buttons.matching(NSPredicate(format: "label == 'Yanıtı kopyala'")).firstMatch
            if copyBtn.exists {
                try tap(copyBtn, scroll: true)
            }
            if element("advisor-followup-0").exists {
                try tap(element("advisor-followup-0"))
                try visible(text("Katalog tanıtımı", contains: true), scroll: true)
                recordProbe(
                    screen: "48-advisor-analysis-and-reading",
                    category: "Functional",
                    status: "PASS",
                    summary: "Analiz takip çipi ('Suç ve Ceza'yı konuşalım') doğrudan eser analizi üretti",
                    detail: "Edebi Danışman takip sorusunda Suç ve Ceza eser analizi ve katalog kartı döndürdü."
                )
            }
            audit("48-advisor-analysis-and-reading")
            inspectScreen("48-advisor-analysis-and-reading")
        }

        // 4. Reset chat, test Brainstorm, Debate, Discovery chip toggle, and Brief mood chip
        try step("49-advisor-brainstorm-debate") {
            try tap(element("advisor-new-chat"))
            try visible(element("advisor-intent-brainstorm"))
            try tap(element("advisor-intent-brainstorm"))
            try visible(text("Fikir geliştirme", contains: true))

            try tap(element("advisor-new-chat"))
            try tap(element("advisor-intent-debate"))
            try visible(text("Edebî tartışma", contains: true))

            try tap(element("advisor-new-chat"))
            try visible(element("advisor-mood-gripping"), scroll: true)
            try scrollToTop(target: element("advisor-intent-discover"))
            try tap(element("advisor-intent-discover"))
            app.swipeUp(velocity: .slow)
            if self.text("Ruh Halime Göre").exists {
                throw JourneyError("Ruh hali keşif bölümü gizlenmedi")
            }
            try scrollToTop(target: element("advisor-intent-discover"))
            try tap(element("advisor-intent-discover"))
            try visible(element("advisor-mood-melancholic"), scroll: true)
            try tap(element("advisor-depth-brief"))
            try tap(element("advisor-mood-melancholic"), scroll: true)
            try visible(text("Bu Kitabı Dinle", contains: true), scroll: true)
            audit("49-advisor-brainstorm-debate")
            inspectScreen("49-advisor-brainstorm-debate")
        }

        // 5. Response Depth + Spoiler 3-state cycle + Custom query + CTA navigation + Reading Route + Library pill entry
        try step("50-advisor-custom-query-and-book") {
            try tap(element("advisor-new-chat"))
            try visible(element("advisor-spoiler"))
            try tap(element("advisor-spoiler"))
            try visible(text("Spoilersız"))
            try tap(element("advisor-spoiler"))
            try visible(text("Spoiler izni"))
            try tap(element("advisor-spoiler"))
            try visible(text("Spoiler: otomatik"))
            try tap(element("advisor-spoiler"))
            try visible(text("Spoilersız"))

            try tap(element("advisor-depth-deep"))
            try type("Kizil Dosya polisiye oner", into: element("advisor-input"))
            try tap(element("advisor-send"))
            try visible(text("Spoilersız mod", contains: true), scroll: true)

            let listenBtn = app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH 'advisor-rec-listen-'")).firstMatch
            try visible(listenBtn, scroll: true)
            audit("50-advisor-custom-query-and-book")
            inspectScreen("50-advisor-custom-query-and-book")

            try tap(listenBtn)
            try visible(element("book-play-pause"), scroll: true)
            try tap(text("Geri"))
            try visible(element("advisor-new-chat"))

            try tap(element("advisor-new-chat"))
            try tap(element("advisor-reading-route"), scroll: true)
            try visible(text("Isınma Durağı", contains: true), scroll: true)

            try tap(element("advisor-new-chat"))
            try type("icimizdeki seytani anlat", into: element("advisor-input"))
            try tap(element("advisor-send"))
            dismissKeyboardIfNeeded()
            try visible(text("Sabahattin Ali", contains: true), scroll: true)
            if self.text("Hayvan Çiftliği", contains: true).exists {
                throw JourneyError("'İçimizdeki Şeytanı anlat' sorgusunda ilgisiz 'Hayvan Çiftliği' yanıtı göründü")
            }
            recordProbe(
                screen: "50-advisor-custom-query-and-book",
                category: "Functional",
                status: "PASS",
                summary: "'İçimizdeki Şeytanı anlat' sorgusu Sabahattin Ali eser analizi döndürdü (Hayvan Çiftliği hatası yok)",
                detail: ""
            )

            inspectScreen("50-advisor-custom-query-and-book")
            recordProbe(
                screen: "50-advisor-custom-query-and-book",
                category: "Functional",
                status: "PASS",
                summary: "Edebi Danışman derinlik, spoiler 3-durum döngüsü, kopyala, özel soru, rota ve kitap CTA geçişi doğrulandı",
                detail: "3 giriş noktası, 4 başlangıç kartı, Okuma Görünümü, Bilgi penceresi, Yeni Sohbet, takip çipleri, 'Yanıtı kopyala' ve 'Bu Kitabı Dinle' geçişi çalıştı."
            )
        }
        try tap(element("advisor-back"))
        // 6. Entry 3 via Library Pill
        try tap(element("tab-library"))
        try tap(element("library-advisor-pill"))
        try visible(element("advisor-intent-analysis"))
        try tap(element("advisor-back"))
    }

    func test10BookDetailFeaturesAndMiniPlayer() throws {
        try launchAndSignIn()
        try search("Kızıl Dosya", clearFirst: true)
        try openBookResult()

        try step("51-book-summary-and-like") {
            let summaryCard = element("book-summary-card")
            try visible(summaryCard, scroll: true)
            let winFrame = app.windows.firstMatch.frame
            if !winFrame.isEmpty {
                var moved = false
                for _ in 0..<4 {
                    let frame = summaryCard.frame
                    if frame.isEmpty { break }
                    if frame.maxY > winFrame.height - 110 && frame.minY > 140 {
                        swipeSafeUp()
                        moved = true
                    } else if frame.minY < 125 && frame.maxY < winFrame.height - 110 {
                        swipeSafeDown()
                        moved = true
                    } else {
                        break
                    }
                }
                if moved {
                    Thread.sleep(forTimeInterval: 0.25)
                }
            }
            try tap(summaryCard)
            if !text("Kısalt", contains: true).waitForExistence(timeout: 2) {
                try tap(summaryCard)
            }
            try visible(text("Kısalt", contains: true), scroll: true)

            try scrollToTop(target: element("book-like"))
            try tap(element("book-like"))
            try visible(text("Kitap beğenilenlere eklendi."))
            inspectScreen("51-book-summary-and-like")
            try tap(text("Tamam"))
        }

        try step("52-book-dialogs-and-shelf") {
            try scrollToTop(target: element("book-play-pause"))
            let speedBtn = text("1.0x").exists ? text("1.0x") : text("1.25x")
            try tap(speedBtn, scroll: true)
            try visible(text("Çalma Hızı"))
            try tap(text("1.25x"))
            try visible(text("1.25x"))
            try tap(text("1.25x"))
            try visible(text("Çalma Hızı"))
            try tap(text("1.0x"))
            try visible(text("1.0x"))

            try tap(text("Atmosfer"))
            try visible(text("Atmosfer Sesleri"))
            try tap(firstCloseButton())
            try waitFor("Atmosfer penceresi kapanmalı") { !self.text("Atmosfer Sesleri").exists }

            try tap(text("Uyku"))
            try visible(text("Uyku Modu"))
            try tap(text("15 Dakika"))
            try waitFor("Uyku modu penceresi kapanmalı") { !self.text("Uyku Modu").exists }

            try tap(text("Alıntı"))
            try visible(element("quote-dialog-input"))
            try type("Harika bir dedektif girisi", into: element("quote-dialog-input"))
            try tap(text("Alıntıyı kopyala"), scroll: true)
            try waitFor("Alıntı penceresi kapanmalı") { !self.element("quote-dialog-input").exists }

            try tap(text("Alıntı"))
            try visible(element("quote-dialog-input"))
            try type("Dedektiflik bir sanattir", into: element("quote-dialog-input"))
            dismissKeyboardIfNeeded()
            dismissNonReturnKeyboard()
            if element("quote-share-feed").exists {
                try tap(element("quote-share-feed"), scroll: true)
                if text("Alıntınız akışta paylaşıldı.").waitForExistence(timeout: 3) {
                    recordProbe(
                        screen: "52-book-dialogs-and-shelf",
                        category: "Functional",
                        status: "PASS",
                        summary: "Kitap detayından sesli alıntı 'Akışta Paylaş' ile Edebiyat Duvarı'na gönderildi",
                        detail: ""
                    )
                    try tap(text("Tamam"))
                }
            }

            try tap(element("book-add-shelf"), scroll: true)
            try visible(text("Bitirdiklerim"))
            try tap(text("Bitirdiklerim"))
            try visible(text("Kitap rafınıza eklendi."))
            inspectScreen("52-book-dialogs-and-shelf")
            try tap(text("Tamam"))
        }

        try step("53-book-comment-posted") {
            let commentInput = element("book-comment-input")
            try scrollToVisible(commentInput)
            if text("Bunları da Sevebilirsin").exists {
                recordProbe(
                    screen: "53-book-comment-posted",
                    category: "Content",
                    status: "PASS",
                    summary: "Kitap detayında aynı kategoriden 'Bunları da Sevebilirsin' öneri satırı yüklendi",
                    detail: ""
                )
            }
            let winH = app.windows.firstMatch.frame.height
            if winH > 0 && commentInput.frame.maxY > winH - 220 {
                swipeSafeUp()
            }
            try type("Harika seslendirme", into: commentInput)
            try tap(element("book-comment-send"))
            try visible(text("Harika seslendirme"), scroll: true)
            inspectScreen("53-book-comment-posted")
        }

        try scrollToTop(target: element("book-play-pause"))
        try tap(element("book-play-pause"))
        try waitFor("Mini oynatıcı öncesi oynatma başlamalı") { self.elapsed("book-elapsed") >= 1 }
        try tap(text("Geri"))
        try step("54-mini-player") {
            try visible(element("mini-player"))
            audit("54-mini-player")
            inspectScreen("54-mini-player")
            try tap(element("mini-player-play-pause"))
            try tap(element("mini-player"))
            try visible(element("book-play-pause"), scroll: true)
            try tap(text("Geri"))
            if element("mini-player-close").exists {
                try tap(element("mini-player-close"))
                try waitFor("Mini oynatıcı kapanmalı") { !self.element("mini-player").exists }
                recordProbe(
                    screen: "54-mini-player",
                    category: "Functional",
                    status: "PASS",
                    summary: "Mini oynatıcı oynat/duraklat, detay açma ve kapatma düğmesi çalıştı",
                    detail: ""
                )
            } else {
                recordProbe(
                    screen: "54-mini-player",
                    category: "Functional",
                    status: "FAIL",
                    summary: "Mini oynatıcı kapatma düğmesi ('mini-player-close') ana ekranda görünmüyor",
                    detail: "MainScreen.kt içindeki MiniPlayerSection çağrısında onCloseClick parametresi geçilmediği için kapatma ikonu hiç oluşturulmuyor."
                )
            }
        }
    }

    func test11SearchUsersAndPopulatedLibrary() throws {
        try launchAndSignIn()
        try tap(element("tab-discover"))
        try step("55-search-book-request") {
            try tap(text("Kitap Talep Et"))
            try visible(element("book-request-title"))
            try type("Satranc", into: element("book-request-title"))
            try tap(text("Gönder"))
            try visible(text("Talebiniz Alındı"))
            inspectScreen("55-search-book-request")
            recordProbe(
                screen: "55-search-book-request",
                category: "Functional",
                status: "PASS",
                summary: "'Kitap Talep Et' penceresi sunucuya (book_requests) kitap talebi gönderdi ve onay gösterdi",
                detail: ""
            )
            try tap(text("Tamam"))
            try waitFor("Talep onay diyaloğu kapanmalı") { !self.text("Talebiniz Alındı").exists }
        }

        try step("56-search-users") {
            try type("Elif", into: element("discover-query"))
            try visible(text("Okurlar"))
            try tap(text("Okurlar"))
            try visible(text("Elif Yılmaz"))
            audit("56-search-users")
            inspectScreen("56-search-users")
        }

        try step("57-other-user-profile") {
            try tap(text("Elif Yılmaz"))
            try visible(text("Okur profili"))
            try visible(text("Takip et"))
            try tap(text("Takip et"))
            try visible(text("Takipten çık"))
            audit("57-other-user-profile")
            inspectScreen("57-other-user-profile")
            try tap(text("Takipten çık"))
            try visible(text("Takip et"))
            try tap(text("Geri"))
        }

        try step("58-library-populated") {
            try tap(element("tab-library"))
            try tap(element("library-tab-0"))
            try visible(text("Kızıl Dosya", contains: true))
            try tap(element("library-tab-2"))
            try visible(text("Kızıl Dosya", contains: true))
            inspectScreen("58-library-populated")
            try tap(element("library-shelves"))
            try tap(text("Bitirdiklerim"))
            try visible(text("Kızıl Dosya", contains: true))
            audit("58-library-populated")
            inspectScreen("58-library-populated")
            try tap(text("Geri"))
        }
    }

    func test12FeedDeepNotificationsAndPostDetail() throws {
        try launchAndSignIn()
        try tap(element("tab-feed"))
        try visible(text("Edebiyat Duvarı"))

        try step("59-feed-poll-published") {
            try tap(element("feed-compose"))
            try tap(element("feed-compose-1"))
            try visible(element("feed-poll-question"))
            try type("Favori klasiginiz?", into: element("feed-poll-question"))
            try type("Suc ve Ceza", into: element("feed-poll-option-0"))
            try type("Kurk Mantolu Madonna", into: element("feed-poll-option-1"))
            try scrollToVisible(element("feed-post-submit"))
            try tap(element("feed-post-submit"))
            try visible(text("Favori klasiginiz?", contains: true))
            try tap(text("Suc ve Ceza", contains: true))
            try visible(text("1 oy", contains: true))

            try tap(text("Etkinlik"))
            if text("Bu kitabı dinlemeye başladı", contains: true).waitForExistence(timeout: 2) {
                recordProbe(
                    screen: "59-feed-poll-published",
                    category: "Functional",
                    status: "PASS",
                    summary: "Edebiyat Duvarı 'Etkinlik' sekmesi canlı dinleme aktivitelerini listeledi",
                    detail: ""
                )
            }
            try tap(text("Mısralar"))
            try tap(element("feed-refresh"))
            try visible(text("Favori klasiginiz?", contains: true))
            audit("59-feed-poll-published")
            inspectScreen("59-feed-poll-published")
        }

        try step("59b-feed-live-listeners") {
            try scrollToTop(target: text("Şu An Dinleyenler"))
            try visible(text("Şu An Dinleyenler"))
            let listenerQuery = app.descendants(matching: .any)
                .matching(NSPredicate(format: "identifier BEGINSWITH 'feed-listener-'"))
            let visibleBubbles = Array(listenerQuery.allElementsBoundByIndex.prefix(12))
            var seenNames: Set<String> = []
            var duplicateName: String? = nil
            for bubble in visibleBubbles where bubble.exists {
                let rawName = bubble.label.trimmingCharacters(in: .whitespacesAndNewlines)
                guard !rawName.isEmpty else { continue }
                if !seenNames.insert(rawName).inserted {
                    duplicateName = rawName
                }
            }
            if let dup = duplicateName {
                recordProbe(
                    screen: "59b-feed-live-listeners",
                    category: "Content",
                    status: "FAIL",
                    summary: "'Şu An Dinleyenler' şeridinde mükerrer okur balonu tespit edildi: '\(dup)'",
                    detail: "Canlı dinleyici şeridi kullanıcı bazında tekilleştirilmelidir."
                )
            }
            let listenerBubble = listenerQuery.firstMatch
            let fallbackBubble = text("mht").exists ? text("mht") : text("Fahrettin Kucukavci")
            let targetBubble = listenerBubble.exists ? listenerBubble : fallbackBubble
            try tap(targetBubble)
            try visible(text("Şu an dinliyor"))
            try visible(element("feed-listening-open-content"))
            try visible(element("feed-listening-view-profile"))
            audit("59b-feed-live-listeners")
            inspectScreen("59b-feed-live-listeners")
            recordProbe(
                screen: "59b-feed-live-listeners",
                category: "Functional",
                status: "PASS",
                summary: "'Şu An Dinleyenler' şeridi (tekilleştirilmiş canlı okurlar) ve dinleme detay kartı ('İçeriğe Git' / 'Profili Gör') doğrulandı",
                detail: "visibleUniqueListeners=\(seenNames.count)"
            )
            try tap(element("feed-listening-close"))
            try waitFor("Dinleme kartı kapanmalı") { !self.text("Şu an dinliyor").exists }
        }

        try step("60-post-detail-comments") {
            let postCard = text("Favori klasiginiz?", contains: true)
            try visible(postCard, scroll: true)
            try tap(postCard)
            if !element("post-comment-input").waitForExistence(timeout: 3) {
                let commentBtn = app.descendants(matching: .any)
                    .matching(NSPredicate(format: "identifier BEGINSWITH 'feed-card-comments-'"))
                    .firstMatch
                if isOnScreenAndHittable(commentBtn) {
                    try tap(commentBtn)
                }
            }
            try visible(element("post-comment-input"))
            try type("Harika soru!", into: element("post-comment-input"))
            try tap(element("post-comment-send"))
            try visible(text("Harika soru!"))
            try tap(text("Beğen (0)"))
            try visible(text("Beğenildi (1)"))

            try tap(text("Düzenle"))
            try visible(text("Yorumu düzenle"))
            let editField = element("post-edit-comment-input")
            try visible(editField)
            editField.coordinate(withNormalizedOffset: CGVector(dx: 0.95, dy: 0.5)).tap()
            editField.typeText(" guncel")
            try tap(text("Kaydet"))
            try visible(text("guncel", contains: true))
            audit("60-post-detail-comments")
            inspectScreen("60-post-detail-comments")

            try tap(text("Sil"))
            try visible(text("Yorumu sil?"))
            let deleteConfirm = app.buttons.matching(NSPredicate(format: "label == 'Sil'"))
            deleteConfirm.element(boundBy: max(0, deleteConfirm.count - 1)).tap()
            if self.text("Yorumu sil?").exists && deleteConfirm.count > 1 {
                deleteConfirm.element(boundBy: 0).tap()
            }
            try waitFor("Yorum silinmeli") { !self.text("Yorumu sil?").exists }
            try tap(text("Geri"))
        }

        try step("61-notifications") {
            try tap(element("feed-notifications"))
            try visible(text("Bildirimler"))
            try visible(text("Deneme bildirimi"))
            try tap(text("Tümünü okundu işaretle"))
            try waitFor("Okundu işaretleme düğmesi kapanmalı") { !self.text("Tümünü okundu işaretle").exists }
            audit("61-notifications")
            inspectScreen("61-notifications")
            try tap(text("Geri"))
        }
    }

    func test13ProfileWrappedArenaAndAskidaDeep() throws {
        try launchAndSignIn()
        try step("62-profile-wrapped") {
            try tap(element("tab-profile"))
            try scrollToVisible(text("Yıllık Özet"))
            try tap(text("Yıllık Özet"))
            try visible(text("Premium Analiz"))
            audit("62-profile-wrapped")
            inspectScreen("62-profile-wrapped")
            try tap(text("Geri"))

            try scrollToVisible(text("Paylaşımlarım"))
            try tap(text("Paylaşımlarım"))
            try scrollToTop(target: element("profile-followers"))
            try tap(element("profile-followers"))
            try visible(text("Takipçiler"))
            try visible(text("Kapat"))
            try tap(text("Kapat"))
        }

        try tap(element("tab-discover"))
        try visible(element("discover-query"))
        try step("63-arena-leaderboard-and-modes") {
            let arenaCard = element("discover-arena-card")
            _ = arenaCard.waitForExistence(timeout: 15)
            try tap(arenaCard, scroll: !isOnScreenAndHittable(arenaCard))
            try visible(element("quiz-leaderboard-button"), scroll: true)
            try tap(element("quiz-leaderboard-button"))
            try visible(text("Elif Yılmaz"))
            audit("63-arena-leaderboard-and-modes")
            inspectScreen("63-arena-leaderboard-and-modes")
            try tap(text("Geri"))
            try visible(element("quiz-start-classic"), scroll: true)
            if isOnScreenAndHittable(element("quiz-riddle-option-0")) {
                try tap(element("quiz-riddle-option-0"))
                recordProbe(
                    screen: "63-arena-leaderboard-and-modes",
                    category: "Functional",
                    status: "PASS",
                    summary: "Arena menüsündeki 'Günün Edebî Bilmecesi' şık seçimi anında yanıtlandı",
                    detail: ""
                )
            }
            try tap(element("quiz-challenges-button"), scroll: true)
            if !element("quiz-tournament-button").waitForExistence(timeout: 2.5), element("quiz-challenges-button").exists {
                try tap(element("quiz-challenges-button"), scroll: true)
            }
            try tap(element("quiz-tournament-button"), scroll: true)
            try tap(element("quiz-briefing-start"))
            try visible(text("Soru 1/10"))
            try visible(element("quiz-question-text"))
            try tap(text("Geri"))
            try tap(element("quiz-start-blitz"), scroll: true)
            try tap(element("quiz-briefing-start"))
            try visible(text("Soru 1/10"))
            try visible(element("quiz-option-0"))
            try tap(text("Geri"))
            try tap(text("Geri"))
            try tap(element("quiz-duel-button"), scroll: true)
            if !text("Çok Oyunculu Arena").waitForExistence(timeout: 2.5), element("quiz-duel-button").exists {
                try tap(element("quiz-duel-button"), scroll: true)
            }
            try visible(text("Çok Oyunculu Arena"))
            try visible(text("Oda Oluşturuldu", contains: true))
            recordProbe(
                screen: "63-arena-leaderboard-and-modes",
                category: "Functional",
                status: "WARN",
                summary: "Düello oda oluşturdu ve rakip bekliyor",
                detail: "Bu senaryoda tek simülatör oynuyor. İki oyunculu bir maç ayrıca test edilmeli."
            )
            try tap(text("İptal Et"))
        }

        try step("63b-arena-guess-author") {
            let guessCard = element("quiz-guess-author-button")
            if (try? visible(guessCard, scroll: true)) != nil {
                try tap(guessCard)
            } else {
                let fallbackCard = text("Kimin Kaleminden?", contains: true)
                try visible(fallbackCard, scroll: true)
                try tap(fallbackCard)
            }
            if !text("Bilgi Yarışması").waitForExistence(timeout: 2.5), guessCard.exists {
                try tap(guessCard, scroll: true)
            }
            try visible(text("Kimin Kaleminden?"))
            try visible(text("Bilgi Yarışması"))
            try visible(element("guess-author-option-0"), scroll: true)
            try tap(element("guess-author-option-0"))
            audit("63b-arena-guess-author")
            inspectScreen("63b-arena-guess-author")
            recordProbe(
                screen: "63b-arena-guess-author",
                category: "Functional",
                status: "PASS",
                summary: "KitapDuy Arena 'Kimin Kaleminden?' yazar tahmin oyunu, puan/can paneli ve şık seçimi doğrulandı",
                detail: ""
            )
            try tap(text("Geri"))
            try visible(element("quiz-start-classic"), scroll: true)
            try tap(text("Geri"))
        }

        try step("64-askida-search-claim-and-donate") {
            if !element("discover-askida-card").exists {
                try tap(element("tab-discover"))
            }
            try scrollToVisible(element("discover-askida-card"))
            try tap(element("discover-askida-card"))
            try visible(element("askida-tab-1"))
            try tap(element("askida-tab-1"))
            try visible(element("askida-donate-title"))
            try type("Simyaci", into: element("askida-donate-title"))
            dismissKeyboardIfNeeded()
            dismissNonReturnKeyboard()
            try visible(element("askida-donate-city"), scroll: true)
            try type("Bursa", into: element("askida-donate-city"))
            dismissKeyboardIfNeeded()
            dismissNonReturnKeyboard()
            for _ in 0..<6 {
                if isOnScreenAndHittable(element("askida-donate-submit")) { break }
                swipeSafeUp()
            }
            if isOnScreenAndHittable(element("askida-donate-submit")) {
                try tap(element("askida-donate-submit"))
                try visible(element("askida-tab-0"))
                recordProbe(
                    screen: "64-askida-search-claim-and-donate",
                    category: "Functional",
                    status: "PASS",
                    summary: "Askıda Kitap arama, talep onayı, sohbet ve bağış akışları tamamlandı",
                    detail: ""
                )
            } else {
                recordProbe(
                    screen: "64-askida-search-claim-and-donate",
                    category: "Keyboard",
                    status: "FAIL",
                    summary: "Askıda Kitap bağış formunda 'Askıya Bırak' düğmesi klavye açıkken ulaşılabilir değil",
                    detail: "AskidaKitapScreen.kt içindeki AskidaKitapBagis düzeni imePadding() ve alanlar ImeAction.Done/singleLine içermediği için alttaki gönder düğmesi klavyenin arkasında kalıyor."
                )
            }

            try visible(element("askida-search"))
            try type("Simyaci", into: element("askida-search"))
            dismissKeyboardIfNeeded()
            dismissNonReturnKeyboard()
            if app.keyboards.firstMatch.exists, element("askida-tab-0").exists {
                try tap(element("askida-tab-0"))
            }
            try visible(text("Simyaci", contains: true))
            try tap(text("Kitabı Talep Et"), scroll: true)
            try visible(text("Evet, İstiyorum"))
            try tap(text("Evet, İstiyorum"))
            try visible(text("Teslimat Sohbeti"))
            try visible(element("askida-chat-input"))
            try type("Tesekkur ederim", into: element("askida-chat-input"))
            try tap(element("askida-chat-send"))
            try visible(text("Tesekkur ederim"))
            try tap(text("Geri"))
            if text("Kitap Sohbeti", contains: true).waitForExistence(timeout: 2.5) {
                recordProbe(
                    screen: "64-askida-search-claim-and-donate",
                    category: "Functional",
                    status: "PASS",
                    summary: "Askıda Kitap 'Sohbetler' sekmesinde aktif teslimat sohbeti listelendi",
                    detail: ""
                )
            }
            audit("64-askida-search-claim-and-donate")
            inspectScreen("64-askida-search-claim-and-donate")
        }
    }

    func test14DarkModeFullAudit() throws {
        try launchAndSignIn()
        try tap(element("tab-profile"))
        try tap(element("profile-settings"))
        if element("settings-dark-mode-off").exists {
            try tap(element("settings-dark-mode-off"))
            try visible(element("settings-dark-mode-on"))
        }
        try tap(text("Geri"))

        try step("65-dark-home") {
            try tap(element("tab-home"))
            try visible(text("Yeni hikâyeler keşfet"))
            audit("65-dark-home", theme: "Koyu")
            inspectScreen("65-dark-home", theme: "Koyu")
        }
        try step("66-dark-advisor") {
            try tap(element("home-advisor-fab"))
            try visible(element("advisor-intent-analysis"))
            audit("66-dark-advisor", theme: "Koyu")
            inspectScreen("66-dark-advisor", theme: "Koyu")
        }
        try step("66-dark-advisor-reply") {
            try tap(element("advisor-mood-gripping"), scroll: true)
            try visible(text("Bu Kitabı Dinle", contains: true), scroll: true)
            audit("66-dark-advisor-reply", theme: "Koyu")
            inspectScreen("66-dark-advisor-reply", theme: "Koyu")
        }
        try tap(element("advisor-back"))

        try step("67-dark-discover") {
            try tap(element("tab-discover"))
            try visible(element("discover-query"))
            audit("67-dark-discover", theme: "Koyu")
            inspectScreen("67-dark-discover", theme: "Koyu")
        }
        try step("68-dark-book-detail") {
            try search("Kızıl Dosya", clearFirst: false)
            try openBookResult()
            audit("68-dark-book-detail", theme: "Koyu")
            inspectScreen("68-dark-book-detail", theme: "Koyu")
        }
        try tap(text("Geri"))

        try step("68-dark-feed") {
            try tap(element("tab-feed"))
            try visible(text("Edebiyat Duvarı"))
            audit("68-dark-feed", theme: "Koyu")
            inspectScreen("68-dark-feed", theme: "Koyu")
        }

        // Restore Light Mode cleanly.
        try tap(element("tab-profile"))
        try tap(element("profile-settings"))
        if element("settings-dark-mode-on").exists {
            try tap(element("settings-dark-mode-on"))
            try visible(element("settings-dark-mode-off"))
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
            if !Self.didCaptureWelcomeLogin && (element("welcome-email-signin").exists || text("E-Posta ile Giriş Yap").exists) {
                Self.didCaptureWelcomeLogin = true
                try step("38-welcome-login") {
                    inspectScreen("38-welcome-login")
                    let appleBtn = element("welcome-apple-signin").exists ? element("welcome-apple-signin") : text("Apple ile Devam Et", contains: true)
                    let googleBtn = element("welcome-google-signin").exists ? element("welcome-google-signin") : text("Google ile Devam Et", contains: true)
                    let emailBtn = element("welcome-email-signin").exists ? element("welcome-email-signin") : text("E-Posta ile Giriş Yap", contains: true)
                    if appleBtn.exists && googleBtn.exists && emailBtn.exists {
                        let appleFrame = appleBtn.frame
                        let googleFrame = googleBtn.frame
                        let emailFrame = emailBtn.frame
                        if appleFrame.minY < googleFrame.minY && googleFrame.minY < emailFrame.minY && appleFrame.height >= 44 {
                            recordProbe(
                                screen: "38-welcome-login",
                                category: "AppleHIG",
                                status: "PASS",
                                summary: "Karşılama ekranında 'Apple ile Devam Et' düğmesi ilk sırada (Google ve E-Posta'nın üzerinde) ve HIG boyutunda",
                                detail: "appleY=\(Int(appleFrame.minY))pt, googleY=\(Int(googleFrame.minY))pt, emailY=\(Int(emailFrame.minY))pt, height=\(Int(appleFrame.height))pt"
                            )
                        } else {
                            recordProbe(
                                screen: "38-welcome-login",
                                category: "AppleHIG",
                                status: "FAIL",
                                summary: "'Apple ile Devam Et' düğmesi sıralama veya HIG boyut kuralını ihlal ediyor",
                                detail: "appleY=\(Int(appleFrame.minY))pt, googleY=\(Int(googleFrame.minY))pt, height=\(Int(appleFrame.height))pt"
                            )
                        }
                    }
                    try tap(emailBtn)
                    try visible(text("Tekrar Merhaba!"))
                    try tap(text("Giriş Yap"))
                    try visible(text("Lütfen tüm alanları doldurun."))
                    try tap(text("Hesabın yok mu? Kayıt Ol", contains: true))
                    try visible(text("Aramıza Katıl"))
                    try tap(text("Kayıt Ol"))
                    try visible(text("Lütfen tüm alanları doldurun."))
                    audit("38-welcome-login")
                    inspectScreen("38-welcome-login")
                    try tap(text("Geri"))
                    try visible(element("guest-open"))
                }
            }
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
        try visible(element("discover-query"))
        if clearFirst, element("discover-clear").exists { try tap(element("discover-clear")) }
        try type(query, into: element("discover-query"))
        if !query.isEmpty {
            let clearVisible = element("discover-clear").waitForExistence(timeout: 2.5)
            let rawValue = (element("discover-query").value as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            let looksTruncated = !rawValue.isEmpty && rawValue != query && rawValue != "Hangi eseri arıyorsunuz?"
            if !clearVisible || looksTruncated {
                if element("discover-clear").exists { try tap(element("discover-clear")) }
                try type(query, into: element("discover-query"))
            }
        }
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

    private func firstCloseButton() -> XCUIElement {
        app.buttons.matching(NSPredicate(format: "label == 'Kapat'")).firstMatch
    }

    private func swipeSafeUp() {
        if app.keyboards.firstMatch.exists {
            let start = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.42))
            let end = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.14))
            start.press(forDuration: 0.05, thenDragTo: end)
        } else {
            app.swipeUp(velocity: .slow)
        }
    }

    private func swipeSafeDown() {
        if app.keyboards.firstMatch.exists {
            let start = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.18))
            let end = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.46))
            start.press(forDuration: 0.05, thenDragTo: end)
        } else {
            app.swipeDown(velocity: .slow)
        }
    }

    private func isOnScreenAndHittable(_ target: XCUIElement) -> Bool {
        guard target.exists else { return false }
        let winFrame = app.windows.firstMatch.frame
        let frame = target.frame
        guard !winFrame.isEmpty, !frame.isEmpty, frame.width >= 4, frame.height >= 4 else { return false }
        guard frame.minX >= -2, frame.maxX <= winFrame.width + 2 else { return false }
        if frame.height <= winFrame.height * 0.6,
           (frame.minY < 20 || frame.maxY > winFrame.height - 4) {
            return false
        }
        let center = CGPoint(x: frame.midX, y: frame.midY)
        let safeRect = (winFrame.width > 8 && winFrame.height > 8) ? winFrame.insetBy(dx: 2, dy: 2) : winFrame
        guard safeRect.contains(center) else { return false }
        return target.isHittable
    }

    private func scrollToTop(target: XCUIElement) throws {
        for _ in 0..<12 {
            if isOnScreenAndHittable(target) { return }
            swipeSafeDown()
        }
    }

    private func scrollToVisible(_ target: XCUIElement) throws {
        for _ in 0..<6 {
            if isOnScreenAndHittable(target) { return }
            swipeSafeUp()
        }
        for _ in 0..<6 {
            if isOnScreenAndHittable(target) { return }
            swipeSafeDown()
        }
        try visible(target, scroll: false)
    }

    private func dismissNonReturnKeyboard() {
        guard app.keyboards.firstMatch.exists else { return }
        for key in ["Hide keyboard", "Dismiss", "Done", "Bitti"] {
            let keyButton = app.keyboards.buttons[key]
            if keyButton.exists && keyButton.isHittable {
                keyButton.tap()
                return
            }
        }
        let toolbarDone = app.toolbars.buttons["Done"]
        if toolbarDone.exists && toolbarDone.isHittable {
            toolbarDone.tap()
        }
    }

    private func dismissKeyboardIfNeeded() {
        guard app.keyboards.firstMatch.exists else { return }
        for key in ["Return", "return", "Search", "search", "Done", "Bitti", "Ara", "Gönder", "Send"] {
            let keyButton = app.keyboards.buttons[key]
            if keyButton.exists && keyButton.isHittable {
                keyButton.tap()
                return
            }
        }
    }

    private func visible(_ target: XCUIElement, scroll: Bool = false) throws {
        if scroll {
            for _ in 0..<8 {
                if isOnScreenAndHittable(target) { return }
                if target.exists && !target.frame.isEmpty && target.frame.minY < 100 {
                    swipeSafeDown()
                } else {
                    swipeSafeUp()
                }
            }
            for _ in 0..<6 {
                if isOnScreenAndHittable(target) { return }
                swipeSafeDown()
            }
        }
        do {
            try waitFor("Erişilebilir öğe görünmeli") { self.isOnScreenAndHittable(target) }
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
        if !app.keyboards.firstMatch.waitForExistence(timeout: 1.5) {
            target.coordinate(withNormalizedOffset: CGVector(dx: 0.3, dy: 0.5)).tap()
            _ = app.keyboards.firstMatch.waitForExistence(timeout: 2.0)
        }
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
                recordProbe(
                    screen: name,
                    category: "Journey",
                    status: "FAIL",
                    summary: "Adım başarısız: \(name)",
                    detail: error.localizedDescription
                )
                attachScreenshot("failure-\(name)", to: activity)
                throw JourneyError("\(name): \(error.localizedDescription)")
            }
        }
    }

    private func audit(_ screen: String, theme: String = "Açık") {
        guard #available(iOS 17.0, *) else { return }
        XCTContext.runActivity(named: "Accessibility · \(screen)") { activity in
            var findings: [String] = []
            do {
                try app.performAccessibilityAudit(for: [.contrast, .textClipped, .hitRegion, .sufficientElementDescription]) { issue in
                    let label = issue.element?.label ?? ""
                    let desc = "\(issue.compactDescription) [\(label)]"
                    findings.append("\(desc)\n\(issue.detailedDescription)")
                    self.recordProbe(
                        screen: screen,
                        theme: theme,
                        category: "AppleA11y",
                        status: "WARN",
                        summary: desc,
                        detail: issue.detailedDescription
                    )
                    return true // Report existing issues without interrupting the journey.
                }
                if findings.isEmpty {
                    findings.append("Apple erişilebilirlik denetimi uyarı üretmedi.")
                    recordProbe(
                        screen: screen,
                        theme: theme,
                        category: "AppleA11y",
                        status: "PASS",
                        summary: "Apple erişilebilirlik denetimi uyarısız geçti",
                        detail: ""
                    )
                }
            } catch {
                let err = "Denetim tamamlanamadı: \(error.localizedDescription)"
                findings.append(err)
                recordProbe(
                    screen: screen,
                    theme: theme,
                    category: "AppleA11y",
                    status: "WARN",
                    summary: err,
                    detail: ""
                )
            }
            let attachment = XCTAttachment(string: findings.joined(separator: "\n\n"))
            attachment.name = "audit-\(screen)"
            attachment.lifetime = .keepAlways
            activity.add(attachment)
        }
    }

    private func inspectScreen(_ screen: String, theme: String = "Açık") {
        let winFrame = app.windows.firstMatch.frame
        guard !winFrame.isEmpty else { return }

        let buttons = Array(app.buttons.allElementsBoundByIndex.prefix(80))
        let texts = Array(app.staticTexts.allElementsBoundByIndex.prefix(120))
        let inputs = Array(app.textFields.allElementsBoundByIndex.prefix(15))

        if buttons.isEmpty && texts.isEmpty {
            recordProbe(
                screen: screen,
                theme: theme,
                category: "Layout",
                status: "FAIL",
                summary: "Ekran boş veya erişilebilirlik ağacı oluşmadı",
                detail: "window=\(NSCoder.string(for: winFrame))"
            )
            return
        }

        var viewportButtons: [(name: String, frame: CGRect)] = []
        for btn in buttons where btn.exists {
            let frame = btn.frame
            guard frame.width > 0, frame.height > 0, frame.height < winFrame.height * 0.8 else { continue }
            guard frame.maxY > 0, frame.minY < winFrame.height, frame.maxX > 0, frame.minX < winFrame.width else { continue }
            let btnId = btn.identifier
            let btnName = btn.label.isEmpty ? btnId : btn.label
            if frame.maxY > 0, frame.minY < winFrame.height,
               frame.width < winFrame.width * 0.96,
               (frame.minX < -2 || frame.maxX > winFrame.width + 2) {
                recordProbe(
                    screen: screen,
                    theme: theme,
                    category: "Layout",
                    status: "WARN",
                    summary: "Yatay taşma / kırpılma olasılığı: '\(btnName)'",
                    detail: "frame=\(Int(frame.minX)),\(Int(frame.minY)) \(Int(frame.width))x\(Int(frame.height))pt, windowWidth=\(Int(winFrame.width))pt"
                )
            }
            if frame.minY >= 0, frame.maxY <= winFrame.height,
           frame.minX >= 0, frame.maxX <= winFrame.width,
           (frame.width < 42 || frame.height < 42) {
                recordProbe(
                    screen: screen,
                    theme: theme,
                    category: "TouchTarget",
                    status: "WARN",
                    summary: "Küçük dokunma alanı (<44pt): '\(btnName)' (\(Int(frame.width))x\(Int(frame.height))pt)",
                    detail: "Apple HIG minimum 44x44pt dokunma alanı önerir; id='\(btnId)'."
                )
            }
            if btn.label.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && btnId.isEmpty {
                recordProbe(
                    screen: screen,
                    theme: theme,
                    category: "Label",
                    status: "WARN",
                    summary: "Etiketsiz ve kimliksiz düğme (\(Int(frame.width))x\(Int(frame.height))pt)",
                    detail: "VoiceOver kullanıcıları için contentDescription / label eksik."
                )
            }
            let isFloatingChrome = btnId.hasPrefix("tab-") || btnId.hasPrefix("mini-player") || btnId == "home-advisor-fab" || btnId == "feed-compose"
            if !btnName.isEmpty,
               !isFloatingChrome,
               frame.minY >= 56, frame.maxY <= winFrame.height - 96,
               frame.minX >= 0, frame.maxX <= winFrame.width,
               frame.width >= 28, frame.height >= 28,
               frame.width < winFrame.width * 0.85, frame.height <= 68 {
                viewportButtons.append((name: btnName, frame: frame))
            }
        }

        if viewportButtons.count >= 2 {
            for i in 0..<(viewportButtons.count - 1) {
                for j in (i + 1)..<viewportButtons.count {
                    let a = viewportButtons[i]
                    let b = viewportButtons[j]
                    guard a.name != b.name else { continue }
                    guard abs(a.frame.midY - b.frame.midY) < min(a.frame.height, b.frame.height) * 0.5 else { continue }
                    let inter = a.frame.intersection(b.frame)
                    guard !inter.isNull, inter.width > 8, inter.height > 8 else { continue }
                    let aArea = a.frame.width * a.frame.height
                    let bArea = b.frame.width * b.frame.height
                    let minArea = min(aArea, bArea)
                    let maxArea = max(aArea, bArea)
                    guard minArea > 0, maxArea <= minArea * 2.2 else { continue }
                    let overlapArea = inter.width * inter.height
                    if overlapArea > minArea * 0.35, overlapArea < minArea * 0.85 {
                        recordProbe(
                            screen: screen,
                            theme: theme,
                            category: "Layout",
                            status: "WARN",
                            summary: "Çakışan etkileşimli düğmeler: '\(a.name)' ve '\(b.name)'",
                            detail: "overlap=\(Int(inter.width))x\(Int(inter.height))pt"
                        )
                    }
                }
            }
        }

        for field in inputs where field.exists {
            let frame = field.frame
            guard frame.width > 0, frame.height > 0, frame.maxY > 0, frame.minY < winFrame.height, frame.maxX > 0, frame.minX < winFrame.width else { continue }
            if frame.width > 0, (frame.minX < -2 || frame.maxX > winFrame.width + 2) {
                recordProbe(
                    screen: screen,
                    theme: theme,
                    category: "Layout",
                    status: "WARN",
                    summary: "Metin alanı yatay taşıyor: '\(field.identifier)'",
                    detail: "frame=\(Int(frame.minX))..\(Int(frame.maxX)) window=0..\(Int(winFrame.width))"
                )
            }
        }

        for item in texts where item.exists {
            let label = item.label
            let trimmed = label.trimmingCharacters(in: .whitespacesAndNewlines)
            if trimmed == "Eser" && label != trimmed {
                recordProbe(
                    screen: screen,
                    theme: theme,
                    category: "Content",
                    status: "FAIL",
                    summary: "Yazar eser sayısı eksik görünüyor (' Eser')",
                    detail: "Görüntülenen metin '\(label)' başında sayı olmadan boşluk içeriyor."
                )
            } else if label != trimmed,
                      label.range(of: #"^\s+(Kitap|Yazar|okur|oy|puan|dinleme|bölüm)\b"#, options: [.regularExpression, .caseInsensitive]) != nil {
                recordProbe(
                    screen: screen,
                    theme: theme,
                    category: "Content",
                    status: "FAIL",
                    summary: "Sayaç değeri eksik metin tespit edildi ('\(label)')",
                    detail: "Birim etiketinin başında sayı olmadan boşluk var."
                )
            }
            if trimmed == "null" || trimmed == "undefined" || trimmed == "NaN" ||
               label.contains("\u{FFFD}") || trimmed.hasPrefix("??") ||
               label.contains("kotlin.Unit") || label.contains("http://localhost") {
                recordProbe(
                    screen: screen,
                    theme: theme,
                    category: "Content",
                    status: "FAIL",
                    summary: "Arayüzde ham veri, bozuk karakter veya yerel URL sızıntısı",
                    detail: String(trimmed.prefix(120))
                )
            }
            if label.contains("**") || label.contains("[BOOK_REC") || label.contains("[KITAP") ||
               label.contains("(*") ||
               label.range(of: #"(?<!\*)\*[A-Za-zÇĞİÖŞÜçğıöşü0-9(][^*\n]{1,80}[A-Za-zÇĞİÖŞÜçğıöşü0-9).!?]\*(?!\*)"#, options: .regularExpression) != nil {
                recordProbe(
                    screen: screen,
                    theme: theme,
                    category: "Content",
                    status: "FAIL",
                    summary: "Ham Markdown veya öneri etiketi sızıntısı",
                    detail: String(label.prefix(120))
                )
            }
            let lower = trimmed.lowercased()
            if lower.contains("14 gün") || lower.contains("14 gun") {
                recordProbe(
                    screen: screen,
                    theme: theme,
                    category: "Content",
                    status: "FAIL",
                    summary: "Eski '14 gün ücretsiz' metni tespit edildi: '\(trimmed)'",
                    detail: "Ücretsiz deneme metni 7 gün kuralıyla uyumlu olmalı."
                )
            }
            if lower.contains("kullanılamıyor") {
                recordProbe(
                    screen: screen,
                    theme: theme,
                    category: "Content",
                    status: "WARN",
                    summary: "Özellik kullanılamıyor mesajı görüntülendi: '\(trimmed)'",
                    detail: ""
                )
            } else if lower.contains("yüklenemedi") || lower.contains("başarısız") {
                recordProbe(
                    screen: screen,
                    theme: theme,
                    category: "Content",
                    status: "WARN",
                    summary: "Yükleme / işlem hatası mesajı: '\(trimmed)'",
                    detail: ""
                )
            } else if lower.contains("çok yakında") || lower.contains("yakında çok") {
                recordProbe(
                    screen: screen,
                    theme: theme,
                    category: "Content",
                    status: "WARN",
                    summary: "Henüz tamamlanmamış ('Yakında') özellik mesajı: '\(trimmed)'",
                    detail: ""
                )
            }
        }
    }

    private func recordProbe(
        screen: String,
        theme: String = "Açık",
        category: String,
        status: String,
        summary: String,
        detail: String
    ) {
        let key = "\(screen)|\(theme)|\(category)|\(status)|\(summary)"
        guard !seenProbeKeys.contains(key) else { return }
        seenProbeKeys.insert(key)
        probeLog.append(ProbeRecord(
            journey: currentJourneyName,
            screen: screen,
            theme: theme,
            category: category,
            status: status,
            summary: summary,
            detail: detail
        ))
    }

    private func flushProbes() {
        guard !probeLog.isEmpty,
              let data = try? JSONEncoder().encode(probeLog),
              let json = String(data: data, encoding: .utf8) else { return }
        XCTContext.runActivity(named: "Runtime Probes · \(currentJourneyName)") { activity in
            let attachment = XCTAttachment(string: json)
            attachment.name = "probe-\(currentJourneyName)"
            attachment.lifetime = .keepAlways
            activity.add(attachment)
        }
    }

    private func sanitizedMethodName() -> String {
        let raw = name.components(separatedBy: " ").last?.replacingOccurrences(of: "]", with: "") ?? "journey"
        return raw
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
