package io.suggest.maps.nodes

import io.suggest.geo.IGeoShape
import boopickle.Default._
import japgolly.univeq.UnivEq
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.06.17 10:58
  * Description: Кросс-платформенная модель ответа сервера на тему узлов-ресиверов на карте.
  * Данная модель содержит все данные, необходимые для рендера шейпов и маркеров ресиверов на гео.карте.
  */
object MGeoNodesResp {

  implicit val MGEO_RCVR_NODES_RESP_PICKLER: Pickler[MGeoNodesResp] = {
    implicit val mGeoNodePropsShapesP = MGeoNodePropsShapes.MGEO_NODE_PROPS_SHAPES_PICKLER
    generatePickler[MGeoNodesResp]
  }

  implicit def univEq: UnivEq[MGeoNodesResp] = UnivEq.derive

}

/** Класс модели ответа сервера с гео.инфой для рендера узлов-ресиверов на карте мира.
  *
  * @param nodes Мета-данные и гео-шейпы узлов.
  */
case class MGeoNodesResp(
                          nodes   : Iterable[MGeoNodePropsShapes]
                        )


/** Контейнер props и shapes.
  *
  * @param props Инфа по узлу.
  * @param shapes Все гео-шейпы узла, которые надо отрендерить.
  */
case class MGeoNodePropsShapes(
                                props    : MAdvGeoMapNodeProps,
                                shapes   : Iterable[IGeoShape]
                              )

object MGeoNodePropsShapes {

  implicit val MGEO_NODE_PROPS_SHAPES_PICKLER: Pickler[MGeoNodePropsShapes] = {
    implicit val mAdvGeoMapNodePropsP = MAdvGeoMapNodeProps.mAdvGeoMapNodePropsPickler
    implicit val iGeoShapeP = IGeoShape.GEO_SHAPE_PICKLER
    generatePickler[MGeoNodePropsShapes]
  }

  implicit def univEq: UnivEq[MGeoNodePropsShapes] = UnivEq.derive

}

