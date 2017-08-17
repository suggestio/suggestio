package io.suggest.sc.tile

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.05.15 13:03
 * Description: Константы плитки выдачи suggest.io.
 */
object TileConstants {

  /** Узкий блок. */
  @deprecated("Use BlockWidths.NARROW.value", "2017.aug.17")
  final val CELL_WIDTH_140_CSSPX = 140

  def CELL140_COLUMNS_MAX = 8
  def CELL300_COLUMNS_MAX = 4

  def PADDING_CSSPX = 20

}
