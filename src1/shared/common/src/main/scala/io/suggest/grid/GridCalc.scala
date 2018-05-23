package io.suggest.grid

import io.suggest.ad.blk.{BlockWidth, BlockWidths}
import io.suggest.common.geom.d2.IWidth
import io.suggest.dev.{MSzMult, MSzMults}

import scala.annotation.tailrec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.05.15 12:03
 * Description: Утиль для вычисления оптимального кол-ва колонок в плитке выдачи.
 * Рассчет используется на клиенте и сервере.
 *
 * Следует помнить, что рассчет колонок может идти в масштабе 300px и 140px.
 * По дефолту используется масштаб в 140px и 8 колонок max.
 */

object GridCalc {

  /** Посчитать оптимальное кол-во колонок плитки под указанную ширину экрана.
    *
    * @param contSz Размер контейнера плитки.
    * @param conf Конфиг рассчёта плитки. Там сохраняются разные константы.
    * @return Целое кол-во колонок.
    */
  def getColumnsCount(contSz: IWidth, conf: IGridCalcConf, minSzMult: Double = MSzMults.GRID_MIN_SZMULT_D): Int = {
    val padding = conf.cellPadding * minSzMult
    val targetCount = ((contSz.width - padding) / (conf.cellWidthPx * minSzMult + padding)).toInt
    Math.min(conf.maxColumns,
      Math.max(1, targetCount))
  }


  // Вынос из ShowcaseUtil:

  /**
   * Вычислить мультипликатор размера для плиточной выдачи с целью подавления лишних полей по бокам.
   * @param colsCount Кол-во колонок в плитке.
   * @param dscr Экран.
   * @return Оптимальное значение SzMult_t выбранный для рендера.
   */
  def getSzMult4tilesScr(colsCount: Int, dscr: IWidth, conf: IGridCalcConf): MSzMult = {
    val blockWidthPx = conf.cellWidthPx
    // Считаем целевое кол-во колонок на экране.
    @tailrec def detectSzMult(restSzMults: List[MSzMult]): MSzMult = {
      val nextSzMult = restSzMults.head
      if (restSzMults.tail.isEmpty) {
        nextSzMult
      } else {
        // Вычислить остаток ширины за вычетом всех отмасштабированных блоков, с запасом на боковые поля.
        val w1 = getW1(nextSzMult, colsCount, blockWidth = blockWidthPx, scrWidth = dscr.width, paddingPx = conf.cellPadding)
        if (w1 >= MIN_W1)
          nextSzMult
        else
          detectSzMult(restSzMults.tail)
      }
    }
    detectSzMult( MSzMults.GRID_TILE_MULTS )
  }

  def MIN_W1 = -1d

  /**
   * w1 - это ширина экрана за вычетом плитки блоков указанной ширины в указанное кол-во колонок.
   * Этот метод запускает формулу рассчета оставшейся ширины.
   * @param szMult Предлагаемый мультипликатор размера.
   * @param colCnt Кол-во колонок.
   * @param blockWidth Ширина блока.
   * @param scrWidth Доступная для размещения плитки блоков ширина экрана.
   * @return Остаток ширины.
   */
  private def getW1(szMult: MSzMult, colCnt: Int, blockWidth: Int, scrWidth: Int, paddingPx: Int): Double = {
    scrWidth - (colCnt * blockWidth + paddingPx * (colCnt + 1)) * szMult.toDouble
  }

}


trait IGridCalcConf {
  def cellPadding    : Int
  def cellWidthPx    : Int
  def maxColumns     : Int
}


/** Модель конфига для рассчёта колонок сетки.
  *
  * @param cellPadding Расстоянием между ячейками.
  * @param cellWidth Ширина одной рассчётной ячейки в css-пикселях.
  * @param maxColumns Максимально допустимое кол-во колонок.
  */
case class MGridCalcConf(
                          cellPadding    : Int          = GridConst.PADDING_CSSPX,
                          cellWidth      : BlockWidth   = BlockWidths.NARROW,
                          maxColumns     : Int          = GridConst.CELL140_COLUMNS_MAX
                        )
  extends IGridCalcConf
{
  override def cellWidthPx = cellWidth.value
}

object MGridCalcConf {

  /** Настройки для дефолтовой сетки. */
  def PLAIN_GRID = MGridCalcConf()

  /** Чётная сетка -- сетка с чётным кол-вом колонок. */
  def EVEN_GRID: MGridCalcConf = {
    val bw = BlockWidths.NORMAL
    MGridCalcConf(
      maxColumns  = GridConst.CELL300_COLUMNS_MAX,
      cellPadding = GridConst.PADDING_CSSPX,
      cellWidth   = bw
    )
  }

}

