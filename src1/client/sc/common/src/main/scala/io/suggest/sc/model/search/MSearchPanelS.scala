package io.suggest.sc.model.search

import diode.{FastEq, UseValueEq}
import io.suggest.spa.FastEqUtil
import japgolly.univeq.UnivEq
import monocle.macros.GenLens

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
    FastEqUtil.RefValFastEq
      .asInstanceOf[FastEq[MSearchPanelS]]

  @inline implicit def univEq: UnivEq[MSearchPanelS] = UnivEq.derive

  def opened = GenLens[MSearchPanelS](_.opened)

}

/**
  * @param opened Открыта ли панель?
  */
case class MSearchPanelS(
                          opened : Boolean = false,
                        )
  extends UseValueEq
