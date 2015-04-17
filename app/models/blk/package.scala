package models

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.14 17:29
 * Description: Ускоренный доступ к типам значений моделей этого пакета.
 */
package object blk {

  type BlockWidth  = BlockWidths.BlockWidth
  type BlockHeight = BlockHeights.BlockHeight

  // TODO Наверное надо перенести SzMult_t на уровень models.
  type SzMult_t    = Float

  type AdColorFn   = AdColorFns.AdColorFn

  type FontSize    = FontSizes.FontSize


  def szMulted(origPx: Int, szMult: SzMult_t): Int = {
    szMulted(origPx.toFloat, szMult)
  }
  def szMulted(origPx: SzMult_t, szMult: SzMult_t): Int = {
    Math.round(origPx * szMult)
  }

}
