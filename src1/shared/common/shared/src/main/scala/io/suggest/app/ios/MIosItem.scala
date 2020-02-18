package io.suggest.app.ios

import japgolly.univeq._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.02.2020 15:46
  */
object MIosItem {

  implicit def iosItemJson: OFormat[MIosItem] = (
    (__ \ "assets").format[Seq[MIosItemAsset]] and
    (__ \ "metadata").format[MIosItemMeta]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MIosItem] = UnivEq.derive

}

case class MIosItem(
                     assets     : Seq[MIosItemAsset],
                     metadata   : MIosItemMeta,
                   )
