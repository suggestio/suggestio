package io.suggest.bill.tf.daily

import boopickle.Default._
import io.suggest.bill.{MCurrency, MPrice}
import io.suggest.cal.m.{MCalType, MCalTypes}
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._

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

  @inline implicit def univEq: UnivEq[MTfDailyInfo] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

  implicit def tfClausesMapFormat: Format[Map[MCalType, MPrice]] = {
    import io.suggest.enum2.EnumeratumUtil.MapJson._
    //import enumeratum.values.StringEnum._   // TODO Не пашет. Вместо него возвращается не тот object: MCalType.type вместо MCalTypes.
    Format[Map[MCalType, MPrice]](
      stringEnumKeyMapReads[MCalType, MPrice](MCalTypes, implicitly),
      // Для writes здесь нельзя implicitly[Map[]], будет бесконечная рекурсия об tfClausesMapFormat()
      stringEnumKeyMapWrites[MCalType, MPrice]
    )
  }

  implicit def mTfDailyInfoFormat: OFormat[MTfDailyInfo] = (
    (__ \ "m").format[ITfDailyMode] and
    (__ \ "l").format[Map[MCalType, MPrice]] and
    (__ \ "o").format[Int] and
    (__ \ "u").format[MCurrency]
  )(apply, unlift(unapply))

  def clauses = GenLens[MTfDailyInfo](_.clauses)

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
                       )
