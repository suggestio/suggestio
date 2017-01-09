package io.suggest.sc.tile

import io.suggest.common.geom.d2.ISize2di

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.05.15 12:03
 * Description: Поддержка вычисления кол-ва колонок в плитке выдачи.
 * Рассчет используется на клиенте и сервере.
 *
 * Следует помнить, что рассчет колонок может идти в масштабе 300px и 140px.
 * По дефолту используется масштаб в 140px и 8 колонок max.
 */
trait ColumnsCountT {

  /**
   * Самый минимальный мультипликатор размера плитки в выдаче.
   *
   * Значение вычислено через решение уравнения для экрана w=640csspx (pxRatio=3.0).
   * Уравнение было получено из формулы, описанной в getTileColsCountFor() для двух ШИРОКИХ (300csspx) колонок:
   *
   *      640 - 20x
   * 2 = -----------
   *     300x + 20x
   *
   * где x -- это искомый szMult.
   */
  protected def MIN_SZ_MULT = 32F/33F

  /** Паддинг -- базовый интервал между ячейками. */
  protected def TILE_PADDING_CSSPX = TileConstants.PADDING_CSSPX

  /** Максимальное кол-во ячеек по горизонтали. */
  protected def TILE_MAX_COLUMNS = TileConstants.CELL140_COLUMNS_MAX

  /** Минимально число ячеек по горизонтали. */
  protected def TILE_MIN_COLUMNS = 1

  /** Размер одной ячейки в плитке. */
  protected def CELL_WIDTH_CSSPX = TileConstants.CELL_WIDTH_140_CSSPX

  /**
   * Рассчитать целевое кол-во колонок в плитке. Считаем это по минимальному padding'у.
   * @param dscr Экран устройства.
   * @return Число от 1 до 4.
   */
  def getTileColsCountScr(dscr: ISize2di): Int = {
    val minSzMult = MIN_SZ_MULT
    val padding = TILE_PADDING_CSSPX * minSzMult
    val targetCount = ((dscr.width - padding) / (CELL_WIDTH_CSSPX * minSzMult + padding)).toInt
    Math.min(TILE_MAX_COLUMNS,
      Math.max(TILE_MIN_COLUMNS, targetCount))
  }

}
