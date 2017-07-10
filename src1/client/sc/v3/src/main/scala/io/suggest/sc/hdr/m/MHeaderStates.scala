package io.suggest.sc.hdr.m

import enumeratum._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.17 18:11
  * Description: Переключатель состояний заголовка.
  */

/** Элемент модели состояний заголовка. */
sealed class MHeaderState extends EnumEntry

/** Модель состояний заголовка. */
object MHeaderStates extends Enum[MHeaderState] {

  case object PlainGrid extends MHeaderState
  //case object Focused extends MHeaderState
  case object Search extends MHeaderState
  case object Menu extends MHeaderState

  override val values = findValues

}
