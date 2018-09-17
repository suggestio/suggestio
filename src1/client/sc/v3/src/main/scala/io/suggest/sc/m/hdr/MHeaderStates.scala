package io.suggest.sc.m.hdr

import enumeratum._
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.17 18:11
  * Description: Модель состояний заголовка.
  */

object MHeaderStates extends Enum[MHeaderState] {

  case object PlainGrid extends MHeaderState
  case object Search extends MHeaderState
  case object Menu extends MHeaderState

  override val values = findValues

}


/** Элемент модели состояний заголовка. */
sealed abstract class MHeaderState extends EnumEntry

object MHeaderState {

  @inline implicit def univEq: UnivEq[MHeaderState] = UnivEq.derive

}
