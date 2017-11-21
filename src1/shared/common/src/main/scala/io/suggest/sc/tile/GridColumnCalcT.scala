package io.suggest.sc.tile

import io.suggest.ad.blk.BlockWidths
import io.suggest.common.geom.d2.IWidth
import io.suggest.dev.MSzMults

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

object GridColumnCalc {

  /** Посчитать оптимальное кол-во колонок плитки под указанный
    *
    * @param contSz Размер контейнера плитки.
    * @param conf Конфиг рассчёта плитки. Там сохраняются разные константы.
    * @return Целое кол-во колонок.
    */
  def getColumnsCount(contSz: IWidth, conf: MGridColumnsCalcConf): Int = {
    val minSzMult = MSzMults.GRID_MIN_SZMULT_D
    val padding = conf.cellPadding * minSzMult
    val targetCount = ((contSz.width - padding) / (conf.cellWidth * minSzMult + padding)).toInt
    Math.min(conf.maxColumns,
      Math.max(1, targetCount))
  }

}

/** Модель конфига для рассчёта колонок сетки.
  *
  * @param cellPadding Расстоянием между ячейками.
  * @param cellWidth Ширина одной рассчётной ячейки в css-пикселях.
  * @param maxColumns Максимально допустимое кол-во колонок.
  */
case class MGridColumnsCalcConf(
                                 cellPadding    : Int     = TileConstants.PADDING_CSSPX,
                                 cellWidth      : Int     = BlockWidths.NARROW.value,
                                 maxColumns     : Int     = TileConstants.CELL140_COLUMNS_MAX
                               )

object MGridColumnsCalcConf {

  /** Настройки для дефолтовой сетки. */
  def PLAIN_GRID = MGridColumnsCalcConf()

  /** Чётная сетка -- сетка с чётным кол-вом колонок. */
  def EVEN_GRID = MGridColumnsCalcConf(
    maxColumns  = TileConstants.CELL300_COLUMNS_MAX,
    cellPadding = TileConstants.PADDING_CSSPX,
    cellWidth   = BlockWidths.NORMAL.value
  )

}

