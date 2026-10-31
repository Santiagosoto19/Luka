package co.luka.domain.wallet

object WalletMath {
    fun balance(
        opening: Long,
        income: Long,
        expense: Long,
        adjustmentIn: Long,
        adjustmentOut: Long,
        transferOut: Long,
        transferIn: Long,
    ): Long = opening + income - expense + adjustmentIn - adjustmentOut - transferOut + transferIn
}
