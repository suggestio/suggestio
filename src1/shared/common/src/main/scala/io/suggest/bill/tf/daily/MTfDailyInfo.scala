package io.suggest.bill.tf.daily

import boopickle.Default._
import io.suggest.bill.{MCurrency, MPrice}
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


/** Класс модель инфы по тарифам узла.
  * В отличии от основной модели MTfDaily, тут скомпанованы разные данные воедино.
  *
  * @param mode Режим тарифа
  * @param clauses Упрощённые условия посуточного тарифа.
  * @param comissionPct Комиссия s.io в %%. Например, 100%.
  * @param currency Валюта тарифа. Совпадает с clausers(*).currency.
  */
case class MTfDailyInfo(
                         mode         : ITfDailyMode,
                         clauses      : Map[MCalType, MPrice],
                         comissionPct : Int,
                         currency     : MCurrency
                       ) {

  def withClauses(clauses2: Map[MCalType, MPrice]) = copy(clauses = clauses2)

}
