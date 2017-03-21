package io.suggest.bill.tf.daily

import boopickle.Default._
import io.suggest.bill.MPrice
import io.suggest.cal.m.MCalType

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.03.17 18:37
  * Description: Инфа по тарифу для представления юзеру.
  */
object MTfDailyInfo {

  implicit val mTfDailyInfoPickler: Pickler[MTfDailyInfo] = {
    implicit val lkTfDailyModeP = ITfDailyMode.tfDailyModePickler
    implicit val mCalTypeP = MCalType.mCalTypePickler
    implicit val mPriceP = MPrice.mPricePickler
    generatePickler[MTfDailyInfo]
  }

}

case class MTfDailyInfo(
                         mode     : ITfDailyMode,
                         clauses  : Map[MCalType, MPrice]
                       )
