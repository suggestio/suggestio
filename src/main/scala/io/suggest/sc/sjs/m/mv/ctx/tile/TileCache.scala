package io.suggest.sc.sjs.m.mv.ctx.tile

import io.suggest.sc.ScConstants
import io.suggest.sc.sjs.m.mv.ctx.{IdFind, Div, ClearableCache}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 11:59
 * Description: Кеш для tile.
 */
trait TileCache extends ClearableCache {

  object tile
    extends ClearableCache
    with TileDiv

  override def cachesClear(): Unit = {
    super.cachesClear()
    tile.cachesClear()
  }

}


trait TileDiv extends ClearableCache {
  object tileDiv extends Div with IdFind {
    override def id: String = ScConstants.Tile.TILE_DIV_ID
  }

  override def cachesClear(): Unit = {
    super.cachesClear()
    tileDiv.cachesClear()
  }
}
