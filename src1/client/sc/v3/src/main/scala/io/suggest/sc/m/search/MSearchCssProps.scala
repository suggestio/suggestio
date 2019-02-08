package io.suggest.sc.m.search

import diode.FastEq
import diode.data.Pot
import io.suggest.dev.MScreenInfo
import io.suggest.maps.nodes.MGeoNodesResp
import io.suggest.sc.index.MSc3IndexResp
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
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
      (a.req ===* b.req) &&
      (a.screenInfo ===* b.screenInfo)
    }
  }

  @inline implicit def univEq: UnivEq[MSearchCssProps] = UnivEq.derive

  val req = GenLens[MSearchCssProps](_.req)
  val screenInfo = GenLens[MSearchCssProps](_.screenInfo)

}


case class MSearchCssProps(
                            req          : Pot[MSearchRespInfo[MGeoNodesResp]]   = Pot.empty,
                            screenInfo   : MScreenInfo,
                          ) {

  def withScreenInfo(screenInfo: MScreenInfo) = copy(screenInfo = screenInfo)

  /** Карта узлов. */
  def nodesMap: Map[String, MSc3IndexResp] =
    req.fold(Map.empty[String, MSc3IndexResp])(_.resp.nodesMap)

}
