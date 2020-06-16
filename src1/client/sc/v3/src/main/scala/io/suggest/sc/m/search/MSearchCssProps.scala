package io.suggest.sc.m.search

import diode.FastEq
import io.suggest.dev.MScreenInfo
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.08.18 14:32
  * Description: Модель аргументов для css-шаблона доп.данных гео-панели.
  */
object MSearchCssProps {

  implicit object MSearchCssPropsFastEq extends FastEq[MSearchCssProps] {
    override def eqv(a: MSearchCssProps, b: MSearchCssProps): Boolean = {
      (a.nodesFound ===* b.nodesFound) &&
      (a.screenInfo ===* b.screenInfo)
    }
  }

  @inline implicit def univEq: UnivEq[MSearchCssProps] = UnivEq.derive

  def nodesFound = GenLens[MSearchCssProps](_.nodesFound)
  def screenInfo = GenLens[MSearchCssProps](_.screenInfo)

}


case class MSearchCssProps(
                            nodesFound   : MNodesFoundS   = MNodesFoundS.empty,
                            screenInfo   : MScreenInfo,
                          )
