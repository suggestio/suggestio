package io.suggest.sc.search

import io.suggest.maps.nodes.MAdvGeoMapNodeProps
import io.suggest.model.n2.node.MNodeType
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
    (__ \ "t").format[Seq[MSc3NodeInfo]]
      .inmap(apply, _.results)
  }

  implicit def univEq: UnivEq[MSc3NodeSearchResp] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

}
case class MSc3NodeSearchResp(
                               results: Seq[MSc3NodeInfo]
                             )


/** Модель данных по одному найденному тегу. */
object MSc3NodeInfo {

  /** Поддержка play-json для инстансов [[MSc3NodeInfo]]. */
  implicit def MSC3_TAG_FORMAT: OFormat[MSc3NodeInfo] = (
    (__ \ "p").format[MAdvGeoMapNodeProps] and
    (__ \ "t").format[MNodeType]
  )(apply, unlift(unapply))

  implicit def univEq: UnivEq[MSc3NodeInfo] = UnivEq.derive

}

/** Данные по одному узлу (изначально - найденному тегу).
  *
  * @param props Данные узла в формате узла гео-карты.
  *              Может включать в себя логотип узла, если есть.
  * @param nodeType Тип узла. Тег или adn-узел.
  */
case class MSc3NodeInfo(
                         props    : MAdvGeoMapNodeProps,
                         nodeType : MNodeType,
                         // Добавить сюда опциоальное кол-во карточек в теге или в узле?
                       )
