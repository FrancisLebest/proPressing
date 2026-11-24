package com.example

import com.example.data.PaymentMethod
import com.example.ui.screens.buildPaymentMethodSlices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardAnalyticsTest {

    @Test
    fun paymentSlices_areEmptyWhenThereAreNoRealPayments() {
        assertTrue(buildPaymentMethodSlices(emptyList()).isEmpty())
    }

    @Test
    fun paymentSlices_aggregateAmountsBySupportedMethod() {
        val result = buildPaymentMethodSlices(
            listOf(
                PaymentMethod.ESPECES.name to 12_500L,
                PaymentMethod.WAVE.name to 5_000L,
                PaymentMethod.ESPECES.name to 2_500L,
                PaymentMethod.MTN_MOMO.name to 7_000L
            )
        )

        assertEquals(
            listOf(
                "Espèces" to 15_000f,
                "Wave" to 5_000f,
                "MTN MoMo" to 7_000f
            ),
            result
        )
    }

    @Test
    fun paymentSlices_ignoreUnknownAndNonPositiveEntries() {
        val result = buildPaymentMethodSlices(
            listOf(
                "UNKNOWN" to 99_000L,
                PaymentMethod.WAVE.name to 0L,
                PaymentMethod.ORANGE_MONEY.name to -500L
            )
        )

        assertTrue(result.isEmpty())
    }
}
