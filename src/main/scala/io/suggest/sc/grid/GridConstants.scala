package io.suggest.sc.grid

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.05.15 16:08
 * Description: Констатны для cbca_grid.
 * Используется для передачи параметров сетки между сервером и клиентом выдачи.
 */
object GridConstants {
  
  /** Название json-поля с размером ячейки. */
  def CELL_SIZE_CSSPX_FN    = "cs"

  /** Название json с расстоянием между ячейками. */
  def CELL_PADDING_CSSPX_FN = "cp"

}
