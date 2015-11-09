package models

import models.im.MImgT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.14 17:29
 * Description: Ускоренный доступ к типам значений моделей этого пакета.
 */
package object blk {

  type BlockWidth  = BlockWidths.T
  type BlockHeight = BlockHeights.T

  type Font        = Fonts.T

  // TODO Наверное надо перенести SzMult_t на уровень models.
  type SzMult_t    = Float

  val  AdColorFns  = io.suggest.ym.model.ad.AdColorFns
  type AdColorFn   = AdColorFns.T

  type FontSize    = FontSizes.FontSize

  type IBlockMeta  = io.suggest.model.n2.ad.blk.IBlockMeta

  type BlockMeta   = io.suggest.model.n2.ad.blk.BlockMeta
  val  BlockMeta   = io.suggest.model.n2.ad.blk.BlockMeta


  // Т.к. SzMult_t является примитивным типом, то модели у него своей нет, и утиль выброшена прямо сюда.
  def szMulted(origPx: Int, szMult: SzMult_t): Int = {
    szMulted(origPx.toFloat, szMult)
  }
  def szMulted(origPx: SzMult_t, szMult: SzMult_t): Int = {
    szRounded( szMultedF(origPx, szMult) )
  }
  def szMultedF(origPx: Int, szMult: SzMult_t): SzMult_t = {
    szMultedF(origPx.toFloat, szMult)
  }
  def szMultedF(origPx: SzMult_t, szMult: SzMult_t): SzMult_t = {
    origPx * szMult
  }
  def szRounded(sz: SzMult_t): Int = {
    Math.round(sz)
  }


  /** Модель сохраненных картинок карточки-блока. */
  type Imgs_t               = io.suggest.ym.model.common.EMImg.Imgs_t

  /** Тип ключа в карте картинок блока. */
  type BimKey_t             = String
  type BlockImgEntry        = (BimKey_t, MImgT)
  type BlockImgMap          = Map[BimKey_t, MImgT]

}
