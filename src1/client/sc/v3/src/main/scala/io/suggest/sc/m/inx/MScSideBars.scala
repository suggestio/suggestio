package io.suggest.sc.m.inx

import enumeratum._
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.11.18 17:20
  * Description: Мини-множество для абстрактого обозначения сайдбаров.
  */
object MScSideBars extends Enum[MScSideBar] {

  /** Панель меню. */
  case object Menu extends MScSideBar

  /** Панель поиска. */
  case object Search extends MScSideBar


  /** Все значения модели. */
  override def values = findValues

}


/** Класс для обозначения одного сайд-бара. */
sealed abstract class MScSideBar extends EnumEntry

object MScSideBar {

  @inline implicit def univEq: UnivEq[MScSideBar] = UnivEq.derive

}
