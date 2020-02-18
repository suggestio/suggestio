package io.suggest.app.ios

import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.02.2020 12:08
  * Description: Аргументы для манифеста установки iOS-приложения.
  */
object MIosItemAsset {

  implicit def iosAssetJson: OFormat[MIosItemAsset] = (
    (__ \ "kind").format[String] and
    (__ \ "url").formatNullable[String] and
    (__ \ "needs-shine").formatNullable[Boolean]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MIosItemAsset] = UnivEq.derive

}


case class MIosItemAsset(
                          kind         : String,
                          url          : Option[String]    = None,
                          needsShine   : Option[Boolean]   = None,
                        )
