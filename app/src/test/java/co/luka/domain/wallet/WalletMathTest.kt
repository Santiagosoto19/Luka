package co.luka.domain.wallet

import org.junit.Assert.assertEquals
import org.junit.Test

class WalletMathTest {
    @Test
    fun expenseLowersTheOpeningBalance() {
        assertEquals(480_000L, WalletMath.balance(500_000, 0, 20_000, 0, 0, 0, 0))
    }

    @Test
    fun incomingAdjustmentRestoresTheBalance() {
        assertEquals(
            500_000L,
            WalletMath.balance(
                opening = 500_000,
                income = 0,
                expense = 20_000,
                adjustmentIn = 20_000,
                adjustmentOut = 0,
                transferOut = 0,
                transferIn = 0,
            ),
        )
    }

    @Test
    fun aTransferKeepsTheSumOfBothWallets() {
        val openingFrom = 500_000L
        val openingTo = 120_000L
        val from = WalletMath.balance(openingFrom, 0, 0, 0, 0, transferOut = 30_000, transferIn = 0)
        val to = WalletMath.balance(openingTo, 0, 0, 0, 0, transferOut = 0, transferIn = 30_000)
        assertEquals(openingFrom + openingTo, from + to)
    }
}
