package io.suggest.mbill2.m.txn.comis

import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.price.Amount_t

object MComission {

  def applyOpt(amountOpt: Option[Amount_t], balanceIdOpt: Option[Gid_t]): Option[MComission] = {
    for (amount <- amountOpt; balanceId <- balanceIdOpt) yield {
      apply(amount, balanceId)
    }
  }

  def unapplyOpt(mcOpt: Option[MComission]): Option[(Option[Amount_t], Option[Gid_t])] = {
    for (mc <- mcOpt) yield {
      (Some(mc.amount), Some(mc.balanceId))
    }
  }

}


/**
 * Инстанс модели комисии.
 * @param amount Кол-во списанных денег в валюте платежа.
 * @param balanceId id баланса комиссионера.
 */
case class MComission(
  amount        : Amount_t,
  balanceId     : Gid_t
)
