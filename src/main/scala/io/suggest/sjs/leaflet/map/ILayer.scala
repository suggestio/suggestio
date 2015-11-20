package io.suggest.sjs.leaflet.map

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.11.15 17:00
  * Description: ILayer API.
  */
@js.native
class ILayer extends js.Object {

  def onAdd(lmap: LMap): Unit = js.native

  def onRemove(lmap: LMap): Unit = js.native

}
