package io.suggest.sc.sjs.v.render.direct.tile

import org.scalajs.dom
import io.suggest.sc.ScConstants.Tile.TILE_DIV_ID

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.05.15 17:15
 * Description: view плитки рекламный карточек. Он получает тычки от контроллеров или других view'ов,
 * и влияет на отображаемую плитку.
 */
class AdsTile {

  def adjustDom(): Unit = {
    val d = dom.document
    val div = d.getElementById(TILE_DIV_ID)
    ???
  }


}
