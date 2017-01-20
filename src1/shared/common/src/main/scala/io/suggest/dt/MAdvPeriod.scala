package io.suggest.dt

import boopickle.Default._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.12.16 11:23
  * Description: Модель описания временнОго периода размещения.
  */

object MAdvPeriod {

  implicit val mAdvPeriodPickler: Pickler[MAdvPeriod] = {
    implicit val mappPickler = IPeriodInfo.periodInfoPickler
    generatePickler[MAdvPeriod]
  }

}


/**
  * Класс модели периода размещения.
  * @param info Данные периода времени. Могут быть заданы по-разному.
  * @param isProlongable Пролонгируемый?
  *                      Если да, то система попытается автоматом по окончанию периода размещения продлить его снова.
  */
case class MAdvPeriod(
                       info           : IPeriodInfo         = IPeriodInfo.default,
                       isProlongable  : Boolean             = false
                     ) {

  def withInfo(pi: IPeriodInfo) = copy(info = pi)
  def withProlongable(p: Boolean) = copy(isProlongable = p)

}


