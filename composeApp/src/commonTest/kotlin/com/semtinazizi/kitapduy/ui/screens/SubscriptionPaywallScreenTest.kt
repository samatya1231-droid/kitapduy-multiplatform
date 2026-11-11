package com.semtinazizi.kitapduy.ui.screens

import com.semtinazizi.kitapduy.platform.SubscriptionProduct
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SubscriptionPaywallScreenTest {

    @Test
    fun testPlanMappingMonthlyAndThreeMonths() {
        // "If a product id contains 'month' and not '3', map to 1-month plan. If it contains '3', map to 3-month plan."
        assertEquals(SubscriptionPlanType.MONTHLY, mapToSubscriptionPlan("premium_monthly"))
        assertEquals(SubscriptionPlanType.MONTHLY, mapToSubscriptionPlan("premium_1_month"))
        assertEquals(SubscriptionPlanType.MONTHLY, mapToSubscriptionPlan("monthly"))
        assertEquals(SubscriptionPlanType.MONTHLY, mapToSubscriptionPlan("MONTHLY_PASS"))

        assertEquals(SubscriptionPlanType.THREE_MONTHS, mapToSubscriptionPlan("premium_3months"))
        assertEquals(SubscriptionPlanType.THREE_MONTHS, mapToSubscriptionPlan("premium_3_months"))
        assertEquals(SubscriptionPlanType.THREE_MONTHS, mapToSubscriptionPlan("3months"))
        assertEquals(SubscriptionPlanType.THREE_MONTHS, mapToSubscriptionPlan("tier_3_month"))

        assertEquals(SubscriptionPlanType.OTHER, mapToSubscriptionPlan("premium_yearly"))
        assertEquals(SubscriptionPlanType.OTHER, mapToSubscriptionPlan("lifetime"))
    }

    @Test
    fun testIsSameSubscriptionPlan() {
        // Exact matches
        assertTrue(isSameSubscriptionPlan("premium_monthly", "premium_monthly"))
        assertTrue(isSameSubscriptionPlan("premium_3months", "premium_3months"))

        // Aliases between App Store Connect IDs and legacy IDs
        assertTrue(isSameSubscriptionPlan("premium_monthly", "premium_1_month"))
        assertTrue(isSameSubscriptionPlan("premium_1_month", "premium_monthly"))
        assertTrue(isSameSubscriptionPlan("premium_3months", "premium_3_months"))
        assertTrue(isSameSubscriptionPlan("premium_3_months", "premium_3months"))

        // Different plans should not match
        assertFalse(isSameSubscriptionPlan("premium_monthly", "premium_3months"))
        assertFalse(isSameSubscriptionPlan("premium_1_month", "premium_3_months"))
        assertFalse(isSameSubscriptionPlan("premium_monthly", "premium_yearly"))
    }

    @Test
    fun testFallbackProductsDefaults() {
        assertEquals(2, FALLBACK_PRODUCTS.size)
        assertEquals("premium_monthly", FALLBACK_PRODUCTS[0].id)
        assertEquals("premium_3months", FALLBACK_PRODUCTS[1].id)
    }

    @Test
    fun testFindFallbackProductSupportsCanonicalAndAliases() {
        val monthlyCanonical = findFallbackProduct("premium_monthly")
        assertNotNull(monthlyCanonical)
        assertEquals(SubscriptionPlanType.MONTHLY, mapToSubscriptionPlan(monthlyCanonical.id))

        val monthlyAlias = findFallbackProduct("premium_1_month")
        assertNotNull(monthlyAlias)
        assertEquals(SubscriptionPlanType.MONTHLY, mapToSubscriptionPlan(monthlyAlias.id))

        val threeMonthsCanonical = findFallbackProduct("premium_3months")
        assertNotNull(threeMonthsCanonical)
        assertEquals(SubscriptionPlanType.THREE_MONTHS, mapToSubscriptionPlan(threeMonthsCanonical.id))

        val threeMonthsAlias = findFallbackProduct("premium_3_months")
        assertNotNull(threeMonthsAlias)
        assertEquals(SubscriptionPlanType.THREE_MONTHS, mapToSubscriptionPlan(threeMonthsAlias.id))
    }

    @Test
    fun testResolveSelectedProductWithAliases() {
        val products = listOf(
            SubscriptionProduct("premium_monthly", "1 Aylık", "₺49,99", "Ay"),
            SubscriptionProduct("premium_3months", "3 Aylık", "₺119,99", "3 Ay")
        )

        // Exact match
        assertEquals("premium_monthly", resolveSelectedProduct(products, "premium_monthly")?.id)
        assertEquals("premium_3months", resolveSelectedProduct(products, "premium_3months")?.id)

        // Alias resolution
        assertEquals("premium_monthly", resolveSelectedProduct(products, "premium_1_month")?.id)
        assertEquals("premium_3months", resolveSelectedProduct(products, "premium_3_months")?.id)

        // Case-insensitive resolution
        assertEquals("premium_monthly", resolveSelectedProduct(products, "PREMIUM_MONTHLY")?.id)
        assertEquals("premium_monthly", resolveSelectedProduct(products, "PREMIUM_1_MONTH")?.id)
        assertEquals("premium_3months", resolveSelectedProduct(products, "PREMIUM_3MONTHS")?.id)
    }

    @Test
    fun testNormalizeSubscriptionProductsPrioritizesCanonicalIds() {
        val mixedProducts = listOf(
            SubscriptionProduct("premium_1_month", "1 Aylık Alias", "₺49,99", "Ay"),
            SubscriptionProduct("premium_3_months", "3 Aylık Alias", "₺119,99", "3 Ay"),
            SubscriptionProduct("premium_monthly", "1 Aylık Canonical", "₺49,99", "Ay"),
            SubscriptionProduct("premium_3months", "3 Aylık Canonical", "₺119,99", "3 Ay")
        )

        val normalized = normalizeSubscriptionProducts(mixedProducts)
        assertEquals(2, normalized.size)
        assertEquals("premium_monthly", normalized[0].id)
        assertEquals("premium_3months", normalized[1].id)
    }

    @Test
    fun testNormalizeSubscriptionProductsPreservesPlanOrderWhenAliasAndCanonicalMixed() {
        // When 1-month plan is an alias and 3-month plan is canonical, 1-month plan must still come first!
        val mixedProducts = listOf(
            SubscriptionProduct("premium_1_month", "1 Aylık Alias", "₺49,99", "Ay"),
            SubscriptionProduct("premium_3months", "3 Aylık Canonical", "₺119,99", "3 Ay")
        )

        val normalized = normalizeSubscriptionProducts(mixedProducts)
        assertEquals(2, normalized.size)
        assertEquals("premium_1_month", normalized[0].id)
        assertEquals("premium_3months", normalized[1].id)
    }
}
