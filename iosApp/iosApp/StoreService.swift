import Foundation
import StoreKit
import ComposeApp

/// Only StoreKit-verified, unexpired transactions grant access.
@MainActor
final class StoreService {
    static let shared = StoreService()
    private let productIDs = ["premium_1_month", "premium_3_months"]
    private var products: [Product] = []
    private var transactionTask: Task<Void, Never>?
    private var purchaseInProgress = false

    func configure() {
        guard transactionTask == nil else { return }
        IosBillingBridge.shared.loadProducts = { [weak self] id in
            Task { @MainActor in await self?.loadProducts(requestID: id) }
        }
        IosBillingBridge.shared.purchase = { [weak self] id, productID in
            Task { @MainActor in await self?.purchase(requestID: id, productID: productID) }
        }
        IosBillingBridge.shared.restore = { [weak self] id in
            Task { @MainActor in await self?.restore(requestID: id) }
        }
        transactionTask = Task { [weak self] in
            for await result in Transaction.updates {
                guard let self else { return }
                if case .verified(let transaction) = result,
                   self.productIDs.contains(transaction.productID) {
                    await self.refreshEntitlements()
                    await transaction.finish()
                }
            }
        }
        Task { await refreshEntitlements() }
    }

    func refreshEntitlements() async {
        var active = false
        for await result in Transaction.currentEntitlements {
            guard case .verified(let transaction) = result,
                  productIDs.contains(transaction.productID),
                  transaction.revocationDate == nil,
                  !transaction.isUpgraded,
                  let expiry = transaction.expirationDate,
                  expiry > Date() else { continue }
            active = true
        }
        IosBillingBridge.shared.updateEntitlement(active: active)
    }

    private func loadProducts(requestID: String) async {
        do {
            products = try await Product.products(for: productIDs).filter { $0.type == .autoRenewable }
            let rows: [[String: String]] = products.sorted { $0.price < $1.price }.map {
                ["id": $0.id, "title": $0.displayName, "price": $0.displayPrice,
                 "period": periodText($0.subscription?.subscriptionPeriod)]
            }
            reply(requestID, status: "success", products: rows)
        } catch {
            reply(requestID, status: "error", message: "Abonelikler yüklenemedi. Bağlantınızı kontrol edip tekrar deneyin.")
        }
    }

    private func purchase(requestID: String, productID: String) async {
        guard !purchaseInProgress else {
            reply(requestID, status: "error", message: "Devam eden satın alma işlemini tamamlayın.")
            return
        }
        guard productIDs.contains(productID) else {
            reply(requestID, status: "error", message: "Bu abonelik bulunamadı.")
            return
        }
        purchaseInProgress = true
        defer { purchaseInProgress = false }
        do {
            guard let product = try await Product.products(for: [productID]).first,
                  product.type == .autoRenewable else {
                reply(requestID, status: "error", message: "Bu abonelik şu anda satın alınamıyor.")
                return
            }
            switch try await product.purchase() {
            case .success(let verification):
                guard case .verified(let transaction) = verification else {
                    reply(requestID, status: "error", message: "Satın alma Apple tarafından doğrulanamadı.")
                    return
                }
                await refreshEntitlements()
                await transaction.finish()
                reply(requestID, status: "success")
            case .userCancelled:
                reply(requestID, status: "cancelled")
            case .pending:
                reply(requestID, status: "pending")
            @unknown default:
                reply(requestID, status: "error", message: "Satın alma tamamlanamadı.")
            }
        } catch {
            reply(requestID, status: "error", message: "Satın alma tamamlanamadı. Lütfen tekrar deneyin.")
        }
    }

    private func restore(requestID: String) async {
        do {
            try await AppStore.sync()
            await refreshEntitlements()
            reply(requestID, status: "success")
        } catch {
            reply(requestID, status: "error", message: "Satın alımlar geri yüklenemedi. Lütfen tekrar deneyin.")
        }
    }

    private func periodText(_ period: Product.SubscriptionPeriod?) -> String {
        guard let period else { return "" }
        switch period.unit {
        case .day: return "\(period.value) gün"
        case .week: return "\(period.value) hafta"
        case .month: return "\(period.value) ay"
        case .year: return "\(period.value) yıl"
        @unknown default: return "abonelik dönemi"
        }
    }

    private func reply(_ requestID: String, status: String, message: String? = nil,
                       products: [[String: String]] = []) {
        var payload: [String: Any] = ["status": status, "products": products]
        if let message { payload["message"] = message }
        guard let data = try? JSONSerialization.data(withJSONObject: payload),
              let json = String(data: data, encoding: .utf8) else { return }
        IosBillingBridge.shared.complete(requestId: requestID, responseJson: json)
    }
}
