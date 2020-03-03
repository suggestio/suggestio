package io.suggest.app.ios

import japgolly.univeq._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.02.2020 14:50
  * Description: Контейнер метаданных для item'а в манифесте.
  */
object MIosItemMeta {

  implicit def iosItemMetaJson: OFormat[MIosItemMeta] = (
    (__ \ "bundle-identifier").format[String] and
    (__ \ "bundle-version").format[String] and
    (__ \ "kind").format[String] and
    (__ \ "platform-identifier").formatNullable[String] and
    (__ \ "title").format[String]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MIosItemMeta] = UnivEq.derive

}


case class MIosItemMeta(
                         bundleId       : String,
                         bundleVersion  : String,
                         kind           : String,
                         platformId     : Option[String] = None,
                         title          : String,
                       )
