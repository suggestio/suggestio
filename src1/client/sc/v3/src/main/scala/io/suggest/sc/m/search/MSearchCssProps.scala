package io.suggest.sc.m.search

import diode.FastEq
import diode.data.Pot
import io.suggest.dev.MScreenInfo
import io.suggest.maps.nodes.{MAdvGeoMapNodeProps, MGeoNodePropsShapes}
import io.suggest.primo.id.IId
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq._

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

  implicit def univEq: UnivEq[MSearchCssProps] = UnivEq.derive

}


case class MSearchCssProps(
                            req          : Pot[MSearchRespInfo[Seq[MGeoNodePropsShapes]]]   = Pot.empty,
                            screenInfo   : MScreenInfo,
                          ) {

  def withScreenInfo(screenInfo: MScreenInfo) = copy(screenInfo = screenInfo)

  /** Карта узлов. */
  lazy val nodesMap: Map[String, MAdvGeoMapNodeProps] = {
    val iter = req.iterator
      .flatMap(_.resp)
      .map(_.props)
    IId.els2idMap( iter )
  }

}
