package models.blk

import io.suggest.model.n2.ad.blk.BlockMeta
import play.api.data.Mapping

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.04.15 11:01
 * Description: web-утиль для модели BlockMeta.
 */

class BlockMetaUtil {

  import play.api.data.Forms._

  /** Маппинг для интерфейса IBlockMeta. */
  def imapping: Mapping[BlockMeta] = {
    mapping(
      "width"   -> BlockWidths.idMapping,
      "height"  -> BlockHeights.idMapping,
      "wide"    -> boolean
    )
    { BlockMeta.apply }
    { BlockMeta.unapply }
  }

}
