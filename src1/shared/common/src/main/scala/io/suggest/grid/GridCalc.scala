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
    * @return Целое кол-во колонок в рамках конфига.
    */
  def getColumnsCount(contSz: IWidth, conf: MGridCalcConf, minSzMult: Double = MSzMults.GRID_MIN_SZMULT_D): Int = {
    val padding = conf.cellPadding * minSzMult
    val targetCount = ((contSz.width - padding) / (conf.cellWidth.value * minSzMult + padding)).toInt
    val evenGridColsCount = Math.min(conf.maxColumns,
      Math.max(1, targetCount))
    //println(s"getColsCnt(contSz=$contSz, conf=$conf, minSzMult=$minSzMult): padding=$padding, tgCount=$targetCount, r=>$r")

    // Для EVEN_GRID результат надо домножить на 2 (cell-ширина одного блока).
    Math.max(2, evenGridColsCount * conf.cellWidth.relSz)
  }


  // Вынос из ShowcaseUtil:

  /**
   * Вычислить мультипликатор размера для плиточной выдачи с целью подавления лишних полей по бокам.
   * @param colsCount Кол-во колонок в плитке.
   * @param dscr Экран.
   * @return Оптимальное значение SzMult_t выбранный для рендера.
   */
  def getSzMult4tilesScr(colsCount: Int, dscr: IWidth, conf: MGridCalcConf): MSzMult = {
    val unitColsCount = colsCount / conf.cellWidth.relSz
    // Минимальная остаточная ширина экрана в пикселях. Это расстояние по бокам.
    val MIN_W1_PX = 0d

    // Считаем целевое кол-во колонок на экране.
    @tailrec def detectSzMult(restSzMults: List[MSzMult]): MSzMult = {
      val currSzMult = restSzMults.head
      if (restSzMults.tail.isEmpty) {
        currSzMult
      } else {
        // Вычислить остаток ширины за вычетом всех отмасштабированных блоков, с запасом на боковые поля.
        // Следует помнить, что "колонки" тут - в понятиях GridConf, т.е. могут быть и двойные колонки спокойно.
        val gridWidth0 = unitColsCount * conf.cellWidth.value + conf.cellPadding * (unitColsCount + 1)
        val gridWidthMulted = gridWidth0 * currSzMult.toDouble
        // w1 - это ширина экрана за вычетом плитки блоков указанной ширины в указанное кол-во колонок.
        val w1 = dscr.width - gridWidthMulted
        //println(s"gridWidth: blkW=${conf.cellWidth}+${conf.cellPadding} uCols=$unitColsCount => $gridWidth0 * $currSzMult = $gridWidthMulted ;; scrW=${dscr.width} ;; w1=$w1")
        if (w1 >= MIN_W1_PX)
          currSzMult
        else
          detectSzMult(restSzMults.tail)
      }
    }
    detectSzMult( MSzMults.GRID_TILE_MULTS )
  }

}



/** Модель конфига для рассчёта колонок сетки.
  *
  * @param cellPadding Расстоянием между ячейками.
  * @param cellWidth Ширина одной рассчётной ячейки в css-пикселях.
  * @param maxColumns Максимально допустимое кол-во колонок.
  */
case class MGridCalcConf(
                          cellPadding    : Int,
                          cellWidth      : BlockWidth,
                          maxColumns     : Int
                        ) {

  def colUnitWidthPx = cellWidth.value

}

object MGridCalcConf {

  /** Настройки для дефолтовой сетки. */
  def PLAIN_GRID: MGridCalcConf = {
    MGridCalcConf(
      cellPadding  = GridConst.PADDING_CSSPX,
      cellWidth    = BlockWidths.NARROW,
      maxColumns   = GridConst.CELL140_COLUMNS_MAX
    )
  }

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

