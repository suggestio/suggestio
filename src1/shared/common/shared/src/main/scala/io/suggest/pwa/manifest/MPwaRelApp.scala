package io.suggest.pwa.manifest

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.02.18 18:33
  * Description: Модель описания логически-связанного мобильного приложения.
  */
object MPwaRelApp {

  @inline implicit def univEq: UnivEq[MPwaRelApp] = UnivEq.derive

  implicit def MPWA_REL_APP_FORMAT: OFormat[MPwaRelApp] = (
    (__ \ "platform").format[MAppPlatform] and
    (__ \ "url").format[String]
  )(apply, unlift(unapply))

}


/** Класс описания одного соотнесённого приложения.
  *
  * @param platform Платформа (play).
  * @param url Ссылка.
  */
case class MPwaRelApp(
                       platform   : MAppPlatform,
                       url        : String
                     )
