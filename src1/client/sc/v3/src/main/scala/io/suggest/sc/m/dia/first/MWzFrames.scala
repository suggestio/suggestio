package io.suggest.sc.m.dia.first

import enumeratum.{Enum, EnumEntry}
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.01.19 21:31
  * Description: Типы фреймов в визарде.
  */
object MWzFrames extends Enum[MWzFrame] {

  /** Фрейм с запросом разрешения/доступа. */
  case object AskPerm extends MWzFrame

  /** Фрейм с инфой. */
  case object Info extends MWzFrame

  /** Блокирующий фрейм ожидания реакции юзера или ОС (на какое-то действие).
    * Не называем "Pending", чтобы не путаться с pot.Pending().
    */
  //case object InProgress extends MWzFrame


  override def values = findValues

}


sealed abstract class MWzFrame extends EnumEntry

object MWzFrame {

  implicit def univEq: UnivEq[MWzFrame] = UnivEq.derive

}
