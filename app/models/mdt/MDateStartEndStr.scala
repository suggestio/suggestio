package models.mdt

import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.dt.interval.DatesIntervalConstants.Json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.12.16 14:22
  * Description: JSON-модель интервала дат, заданных в виде строк.
  */
object MDateStartEndStr {

  implicit val FORMAT: OFormat[MDateStartEndStr] = (
    (__ \ START_FN).format[MDateStrInfo] and
    (__ \ END_FN).format[MDateStrInfo]
  )(apply, unlift(unapply))

}

/**
  * Класс модели строкового интервала дат.
  * @param start Отформатированная дата начала.
  * @param end Отформатированная дата окончания.
  */
case class MDateStartEndStr(
  start : MDateStrInfo,
  end   : MDateStrInfo
)



/** Модель описания одной даты. */
object MDateStrInfo {

  implicit val FORMAT: OFormat[MDateStrInfo] = (
    (__ \ DOW_FN).format[String] and
    (__ \ DATE_FN).format[String]
  )(apply, unlift(unapply))

}
case class MDateStrInfo(
  dow  : String,
  date : String
)
