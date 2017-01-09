package models.blk

import play.api.data.Mapping
import util.blocks.BlocksConf

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.04.15 11:01
 * Description: web-утиль для модели BlockMeta.
 */

class BlockMetaUtil {

  import play.api.data.Forms._

  /** Маппинг для интерфейса IBlockMeta. */
  def imapping: Mapping[IBlockMeta] = {
    mapping(
      "width"   -> BlockWidths.idMapping,
      "height"  -> BlockHeights.idMapping,
      "blockId" -> BlocksConf.idMapping,
      "wide"    -> boolean
    )
    { BlockMeta.apply(_, _, _, _) : IBlockMeta }
    { ibm => Some((ibm.width, ibm.height, ibm.blockId, ibm.wide)) }
  }

}
