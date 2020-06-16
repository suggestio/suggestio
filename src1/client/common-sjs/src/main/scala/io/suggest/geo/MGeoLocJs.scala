package io.suggest.geo

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 9:50
 * Description: Модель данных о географическом местоположении на земле.
 */

object MGeoLocJs {

  import org.scalajs.dom.Position

  def apply(pos: Position): MGeoLoc = {
    MGeoLoc(
      point        = MGeoPointJs(pos.coords),
      accuracyOptM = Some(pos.coords.accuracy)
    )
  }

}
