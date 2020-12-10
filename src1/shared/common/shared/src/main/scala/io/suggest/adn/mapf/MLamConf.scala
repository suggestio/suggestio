package io.suggest.adn.mapf

import io.suggest.maps.nodes.MRcvrsMapUrlArgs
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.04.17 16:32
  * Description: Модель данных конфигурации Lam-формы.
  */
object MLamConf {

  @inline implicit def univEq: UnivEq[MLamConf] = UnivEq.derive

  implicit def lacConfJson: OFormat[MLamConf] = (
    (__ \ "n").format[String] and
    (__ \ "r").format[MRcvrsMapUrlArgs]
  )(apply, unlift(unapply))

}


case class MLamConf(
                     nodeId       : String,
                     rcvrsMap     : MRcvrsMapUrlArgs,
                   )
