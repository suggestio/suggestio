package io.suggest.sc.m.search

import diode.{FastEq, UseValueEq}
import io.suggest.spa.DiodeUtil
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.11.18 13:08
  * Description: Состояние компонента панели для панели поиска.
  */
object MSearchPanelS {

  def default = apply()

  /** Костыль для поддержки FastEq. */
  implicit def MSearchPanelSFastEq: FastEq[MSearchPanelS] =
    DiodeUtil.FastEqExt.RefValFastEq
      .asInstanceOf[FastEq[MSearchPanelS]]

  @inline implicit def univEq: UnivEq[MSearchPanelS] = UnivEq.derive

}

/**
  *
  * @param opened Открыта ли панель?
  * @param fixed Форсировать, что панель открыта и сворачивается.
  *              Нужно, чтобы таскание карты не конфликтовало с touch-событиями панели.
  */
case class MSearchPanelS(
                          opened : Boolean = false,
                        )
  extends UseValueEq
{

  def withOpened(opened: Boolean) = copy(opened = opened)

}
