package models

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.14 17:29
 * Description: Ускоренный доступ к типам значений моделей этого пакета.
 */
package object blk {

  type BlockWidth  = BlockWidths.T
  type BlockHeight = BlockHeights.T

  // TODO Наверное надо перенести SzMult_t на уровень models.
  type SzMult_t    = Float

  type AdColorFn   = AdColorFns.AdColorFn

  type FontSize    = FontSizes.FontSize

  type IBlockMeta  = io.suggest.ym.model.common.IBlockMeta


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

}
