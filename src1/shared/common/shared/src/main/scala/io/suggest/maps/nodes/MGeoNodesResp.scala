package io.suggest.maps.nodes

import io.suggest.geo.IGeoShape
import io.suggest.geo.IGeoShape.JsonFormats.minimalFormat
import io.suggest.primo.id.OptStrId
import io.suggest.sc.index.MSc3IndexResp
import io.suggest.text.StringUtil
import io.suggest.xplay.json.PlayJsonUtil
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

  @inline implicit def univEq: UnivEq[MGeoNodesResp] = UnivEq.derive

    /** Поддержка play-json для инстансов [[MGeoNodesResp]]. */
  implicit def msc3NodesSearchRespFormat: OFormat[MGeoNodesResp] = {
    (__ \ "t")
      .format[Seq[MGeoNodePropsShapes]] {
        PlayJsonUtil.readsSeqNoErrorFormat[MGeoNodePropsShapes]
      }
      .inmap(apply, _.nodes)
  }

}

/** Класс модели ответа сервера с гео.инфой для рендера узлов-ресиверов на карте мира.
  *
  * @param nodes Мета-данные и гео-шейпы узлов.
  */
case class MGeoNodesResp(
                          nodes   : Seq[MGeoNodePropsShapes]
                        ) {

  def hasManyNodes = nodes.lengthCompare(1) > 0

  /** Карта узлов. */
  lazy val nodesMap: Map[String, MGeoNodePropsShapes] = {
    nodes
      .iterator
      .map { m =>
        m.props.idOrNameOrEmpty -> m
      }
      .toMap
  }

  override def toString: String = {
    StringUtil.toStringHelper( this, 32 ) { renderF =>
      if (nodes.nonEmpty)
        renderF("")( "[" + nodes.length + "]" )
    }
  }

}


/** Контейнер props и shapes.
  *
  * @param props Инфа по узлу.
  * @param shapes Все гео-шейпы узла, которые надо отрендерить.
  */
case class MGeoNodePropsShapes(
                                props    : MSc3IndexResp,
                                shapes   : Seq[IGeoShape] = Nil,
                              )
  extends OptStrId
{

  override def id = props.nodeId

  override def toString: String = {
    StringUtil.toStringHelper(this, 128) { renderF =>
      renderF("")( props )
      if (shapes.nonEmpty)
        renderF("gs")( shapes.length )
    }
  }

}

object MGeoNodePropsShapes {

  implicit def MGeoNodePropsShapesFormat: OFormat[MGeoNodePropsShapes] = (
    (__ \ "p").format[MSc3IndexResp] and
    (__ \ "s").format[Seq[IGeoShape]] {
      PlayJsonUtil.readsSeqNoErrorFormat[IGeoShape]
    }
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MGeoNodePropsShapes] = UnivEq.derive

}
