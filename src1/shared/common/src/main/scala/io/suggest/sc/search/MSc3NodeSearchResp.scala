package io.suggest.sc.search

import io.suggest.maps.nodes.MGeoNodePropsShapes
import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.11.17 16:15
  * Description: Модель ответа сервера по тегам выдачи.
  */
object MSc3NodeSearchResp {

  /** Поддержка play-json для инстансов [[MSc3NodeSearchResp]]. */
  implicit def msc3NodesSearchRespFormat: OFormat[MSc3NodeSearchResp] = {
    (__ \ "t").format[Seq[MGeoNodePropsShapes]]
      .inmap(apply, _.results)
  }

  implicit def univEq: UnivEq[MSc3NodeSearchResp] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

}
case class MSc3NodeSearchResp(
                               results: Seq[MGeoNodePropsShapes]
                             )

