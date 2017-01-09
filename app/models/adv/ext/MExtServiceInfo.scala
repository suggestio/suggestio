package models.adv.ext

import io.suggest.adv.ext.model.MServices.{APP_ID_FN, NAME_FN}
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.08.16 17:53
  * Description: Модель передачи инфы по внешнему сервису для js.
  */
object MExtServiceInfo {

  /** Поддержка сериализации/десериализации JSON. */
  implicit val FORMAT: OFormat[MExtServiceInfo] = (
    (__ \ NAME_FN).format[String] and
    (__ \ APP_ID_FN).formatNullable[String]
  )(apply, unlift(unapply))

}


/**
  * Класс-контейнер данных по внешнему сервису для js.
  * @param name Идентификатор-название сервиса.
  * @param appId Идентификатор приложения s.io, зареганного на стороне сервиса, если есть.
  */
case class MExtServiceInfo(
  name  : String,
  appId : Option[String]
)
