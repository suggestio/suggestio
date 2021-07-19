package io.suggest.dt

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

// TODO Rewrite MAdvPeriod, IPerioInfo (and all YmdHelpers, stuff) to one simple cross-platform model AdvPeriod(daysCount: Int, dateStart: Option[LocalDate]).

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.12.16 11:23
  * Description: Модель описания временнОго периода размещения.
  */

object MAdvPeriod {

  def default = apply()

  implicit def mAdvPeriodFormat: OFormat[MAdvPeriod] = {
    (__ \ "i")
      .format[IPeriodInfo]
      .inmap[MAdvPeriod](apply, _.info)
  }

  @inline implicit def univEq: UnivEq[MAdvPeriod] = UnivEq.derive

}


/**
  * Класс модели периода размещения.
  * @param info Данные периода времени. Могут быть заданы по-разному.
  */
case class MAdvPeriod(
                       info           : IPeriodInfo         = IPeriodInfo.default,
                     ) {

  def withInfo(pi: IPeriodInfo) = copy(info = pi)

}


