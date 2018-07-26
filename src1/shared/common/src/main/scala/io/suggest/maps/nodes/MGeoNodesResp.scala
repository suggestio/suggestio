package io.suggest.maps.nodes

import io.suggest.geo.IGeoShape
import io.suggest.geo.IGeoShape.JsonFormats.internalMinFormat
import japgolly.univeq.UnivEq
// НЕ УДАЛЯТЬ, используется для обоих UnivEq.derive
import io.suggest.ueq.UnivEqUtil._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.06.17 10:58
  * Description: Кросс-платформенная модель ответа сервера на тему узлов-ресиверов на карте.
  * Данная модель содержит все данные, необходимые для рендера шейпов и маркеров ресиверов на гео.карте.
  */
object MGeoNodesResp {

  implicit def univEq: UnivEq[MGeoNodesResp] = UnivEq.derive

}

/** Класс модели ответа сервера с гео.инфой для рендера узлов-ресиверов на карте мира.
  *
  * @param nodes Мета-данные и гео-шейпы узлов.
  */
case class MGeoNodesResp(
                          nodes   : Seq[MGeoNodePropsShapes]
                        )


/** Контейнер props и shapes.
  *
  * @param props Инфа по узлу.
  * @param shapes Все гео-шейпы узла, которые надо отрендерить.
  */
case class MGeoNodePropsShapes(
                                props    : MAdvGeoMapNodeProps,
                                shapes   : Seq[IGeoShape]
                              )

object MGeoNodePropsShapes {

  implicit def MGeoNodePropsShapesFormat: OFormat[MGeoNodePropsShapes] = (
    (__ \ "p").format[MAdvGeoMapNodeProps] and
    (__ \ "s").format[Seq[IGeoShape]]
  )(apply, unlift(unapply))

  implicit def univEq: UnivEq[MGeoNodePropsShapes] = UnivEq.derive

}

