package io.suggest.mbill2.m.txn.comis

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.12.15 15:19
 * Description: Модель комиссии по платежам.
 */

trait ComissionOptSlick
  extends ComissionAmountOptSlick
  with ComissionBalanceIdOptSlick
{

  import driver.api._

  trait ComissionOpt
    extends ComissionAmountOpt
    with ComissionBalanceIdOpt
  { that: Table[_] =>

    def comissionOpt = (comissionAmountOpt, comissionBalanceIdOpt) <> (
      (MComission.applyOpt _).tupled, MComission.unapplyOpt
    )

  }

}
