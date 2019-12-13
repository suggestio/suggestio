package io.suggest.pwa.manifest

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.02.18 18:33
  * Description: Модель описания логически-связанного мобильного приложения.
  * @see [[https://developer.mozilla.org/en-US/docs/Web/Manifest/related_applications]]
  */
object MAppPlatformData {

  @inline implicit def univEq: UnivEq[MAppPlatformData] = UnivEq.derive

  implicit def pwaRelAppJson: OFormat[MAppPlatformData] = (
    (__ \ "platform").format[MAppDistributor] and
    (__ \ "url").format[String] and
    (__ \ "id").formatNullable[String]
  )(apply, unlift(unapply))

}


/** Класс описания одного соотнесённого приложения.
  *
  * @param platform Платформа (play).
  * @param url Ссылка на приложение внутри платформы.
  * @param id id приложения внутри платформы.
  */
case class MAppPlatformData(
                             platform   : MAppDistributor,
                             url        : String,
                             id         : Option[String]    = None,
                           )
