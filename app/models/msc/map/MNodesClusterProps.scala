package models.msc.map

import io.suggest.sc.map.ScMapConstants
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.04.16 18:39
  * Description: JSON-модель для описания кластера узлов на карте.
  */
object MNodesClusterProps {

  implicit val FORMAT: OFormat[MNodesClusterProps] = {
    (__ \ ScMapConstants.Nodes.COUNT_FN)
      .format[Long]
      .inmap [MNodesClusterProps] (apply, unlift(unapply))
  }

}

case class MNodesClusterProps(
  docCount: Long
)
