package io.suggest.geo

import io.suggest.geo.GeoConstants.GeoLocQs._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 9:50
 * Description: Модель данных о географическом местоположении на земле.
 */

object MGeoLocJs {

  import org.scalajs.dom.Position

  import scala.scalajs.js

  def apply(pos: Position): MGeoLoc = {
    MGeoLoc(
      point        = MGeoPointJs(pos.coords),
      accuracyOptM = Some(pos.coords.accuracy)
    )
  }

  def toJson(v: MGeoLoc): js.Dictionary[js.Any] = {
    val d = js.Dictionary [js.Any] (
      CENTER_FN     -> MGeoPointJs.toJsObject(v.point)
    )

    for (accur <- v.accuracyOptM)
      d(ACCURACY_M_FN) = accur

    d
  }

}
