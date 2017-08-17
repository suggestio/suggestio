package models.blk.ed

import io.suggest.model.n2.ad.blk.BlockMeta
import io.suggest.model.n2.ad.ent.MEntity
import models.blk._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.11.15 11:51
 * Description: Аккамулятор данных биндинга динамической формы блока.
 * Постепенно сюда закидываются значения в ходе биндинга значений.
 */

case class BindAcc(
                    offers  : List[MEntity]         = Nil,
                    height  : Int                   = BlockHeights.default.value,
                    width   : Int                   = BlockWidths.default.value,
                    isWide  : Boolean               = false,
                    href    : Option[String]        = None,
                    bim     : List[BlockImgEntry]   = Nil
) {

  /**
   * Данные этого аккб, относящиеся к метаданным блока, скомпилить в экземпляр BlockMeta.
   * @param blockId id блока.
   * @return Неизменяемый экземпляр BlockMeta.
   */
  def toBlockMeta(blockId: Int): BlockMeta = {
    BlockMeta(height = height, width = width, wide = isWide)
  }

  def toBindResult(blockId: Int): BindResult = {
    BindResult(offers, toBlockMeta(blockId), bim.toMap, href)
  }

}

