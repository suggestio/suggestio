package io.suggest.sjs.leaflet.marker.icon

import io.suggest.sjs.leaflet.LEAFLET_IMPORT
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.11.15 15:53
  * Description: API for icons.
  */
@js.native
@JSImport(LEAFLET_IMPORT, "icon")
object Icon extends js.Function1[IconOptions, Icon] {
  override def apply(arg1: IconOptions = js.native): Icon = js.native
}


@js.native
@JSImport(LEAFLET_IMPORT, "Icon")
class Icon(val options: IconOptions) extends js.Object
