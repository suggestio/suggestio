package models.blk.ed

import models.AOBlock
import models.blk._
import util.blocks.BlockDataImpl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.11.15 11:51
 * Description: Аккамулятор данных биндинга динамической формы блока.
 * Постепенно сюда закидываются значения в ходе биндинга значений.
 */

case class BindAcc(
  offers  : List[AOBlock]         = Nil,
  height  : Int                   = BlockHeights.default.heightPx,
  width   : Int                   = BlockWidths.default.widthPx,
  isWide  : Boolean               = false,
  bim     : List[BlockImgEntry]   = Nil
) {

  /**
   * Данные этого аккб, относящиеся к метаданным блока, скомпилить в экземпляр BlockMeta.
   * @param blockId id блока.
   * @return Неизменяемый экземпляр BlockMeta.
   */
  def toBlockMeta(blockId: Int): BlockMeta = {
    BlockMeta(blockId = blockId, height = height, width = width, wide = isWide)
  }

  def toBindResult(blockId: Int): BindResult = {
    val bd = BlockDataImpl(
      blockMeta = toBlockMeta(blockId),
      offers    = offers
    )
    BindResult(bd, bim.toMap)
  }

}

