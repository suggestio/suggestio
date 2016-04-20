package models.msc.map

import io.suggest.sc.map.ScMapConstants.Nodes.SOURCES_FN
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.04.16 10:18
  * Description: Контейнер ответа из экшена renderMapNodesLayer().
  * Содержит новое наполнение сорсов для mapbox-карты.
  */
object MNodesSources {

  /** Поддержка JSON. */
  implicit val FORMAT: OFormat[MNodesSources] = {
    (__ \ SOURCES_FN)
      .format[Seq[MNodesSource]]
      .inmap [MNodesSources] (apply, _.sources)
  }

}

case class MNodesSources(
  sources: Seq[MNodesSource]
)
