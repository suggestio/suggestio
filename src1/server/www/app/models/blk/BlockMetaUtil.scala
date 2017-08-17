package models.blk

import io.suggest.ad.blk.{BlockHeights, BlockWidths}
import io.suggest.enum2.EnumeratumJvmUtil
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

  def blockHeightMapping = EnumeratumJvmUtil.intIdMapping( BlockHeights )

  def blockWidthMapping = EnumeratumJvmUtil.intIdMapping( BlockWidths )

  /** Маппинг для интерфейса IBlockMeta. */
  def imapping: Mapping[BlockMeta] = {
    mapping(
      "width"   -> blockWidthMapping,
      "height"  -> blockHeightMapping,
      "wide"    -> boolean
    )
    { (w, h, wide) =>
      BlockMeta(height = h.value, width = w.value, wide = wide)
    }
    { bm =>
      Some((BlockWidths.withValue(bm.width), BlockHeights.withValue(bm.height), bm.wide ))
    }
  }

}
