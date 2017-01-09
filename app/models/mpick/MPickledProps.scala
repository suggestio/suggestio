package models.mpick

import io.suggest.pick.PickleUtil
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.12.16 17:07
  * Description: Server-side JSON-модель MPickledProps.
  */
object MPickledProps {

  implicit val FORMAT: OFormat[MPickledProps] = {
    (__ \ PickleUtil.PICKED_FN).format[String]
      .inmap [MPickledProps] (apply, _.pickled)
  }

}

case class MPickledProps(pickled: String)
