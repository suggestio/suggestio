package models.merr

import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.err.ErrorConstants.Json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.03.16 17:55
  * Description: Модель для короткой передачи ошибок между js и сервером.
  */
object MError {

  implicit val FORMAT: OFormat[MError] = (
    (__ \ CODE_FN).formatNullable[String] and
    (__ \ MSG_FN).formatNullable[String]
  )(apply, unlift(unapply))

}


case class MError(
  code  : Option[String]  = None,
  msg   : Option[String]  = None
)
