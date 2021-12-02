package io.suggest.n2.edge.payout

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

object MEdgePayoutTypes extends StringEnum[MEdgePayOutType] {

  /** Payment system internal wallet inside paysystem's account.
    * payout.data contains wallet requisities (usually, wallet long id).
    */
  case object Wallet extends MEdgePayOutType("wallet")

  /** Banking card payout target.
    * payout.data contains banking card data/token, and related info.
    */
  case object BankCard extends MEdgePayOutType("bank_card")


  override def values = findValues

}


sealed abstract class MEdgePayOutType(override val value: String) extends StringEnumEntry

object MEdgePayOutType {

  @inline implicit def univEq: UnivEq[MEdgePayOutType] = UnivEq.derive

  implicit def nodePayoutTargetTypeJson: Format[MEdgePayOutType] =
    EnumeratumUtil.valueEnumEntryFormat( MEdgePayoutTypes )


  implicit final class PoTypeExt( private val poType: MEdgePayOutType ) extends AnyVal {

    /** messages code for internationalization purposes. */
    def singularI18n: String =
      "pay.out.type." + poType.value

  }

}
