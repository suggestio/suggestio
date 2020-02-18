package io.suggest.app.ios

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.02.2020 15:50
  * @see [[https://stackoverflow.com/a/56378044]] -  Пример выхлопа для установочного манифеста.
  */
object MIosManifest {

  implicit def iosManifestJson: OFormat[MIosManifest] = {
    (__ \ "items").format[Seq[MIosItem]]
      .inmap[MIosManifest]( apply, _.items )
  }

  @inline implicit def univEq: UnivEq[MIosManifest] = UnivEq.derive

}


case class MIosManifest(
                         items: Seq[MIosItem]
                       )
