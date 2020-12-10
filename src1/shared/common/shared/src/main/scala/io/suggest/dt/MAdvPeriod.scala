package io.suggest.dt

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.12.16 11:23
  * Description: Модель описания временнОго периода размещения.
  */

object MAdvPeriod {

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


