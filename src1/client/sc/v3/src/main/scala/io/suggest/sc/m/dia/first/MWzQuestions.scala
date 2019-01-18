package io.suggest.sc.m.dia.first

import enumeratum.{Enum, EnumEntry}
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.01.19 12:53
  * Description: Модель страниц визарда.
  */
object MWzQuestions extends Enum[MWzQuestion] {

  /** Описание запроса доступа к геолокации. */
  case object GeoLocPerm extends MWzQuestion

  /** Описание запроса доступа к bluetooth. */
  case object BlueToothPerm extends MWzQuestion

  /** Писулька с окончанием писанины и кнопкой завершения настройки. */
  case object Finish extends MWzQuestion


  override def values = findValues

}


sealed abstract class MWzQuestion extends EnumEntry

object MWzQuestion {

  implicit def univEq: UnivEq[MWzQuestion] = UnivEq.derive

}