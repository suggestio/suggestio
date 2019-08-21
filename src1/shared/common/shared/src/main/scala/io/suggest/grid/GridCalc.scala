package io.suggest.grid

import io.suggest.ad.blk.{BlockHeights, BlockMeta, BlockPaddings, BlockWidth, BlockWidths, MBlockExpandModes}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.geom.d2.IWidth
import io.suggest.dev.{MSzMult, MSzMults}
import io.suggest.jd.MJdConf
import io.suggest.jd.tags.{JdTag, MJdTagNames}
import japgolly.univeq._
import scalaz.Tree

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


  /** Определение szMult для wide-рендера одного блока.
    *
    * @param bm Метаданные исходного блока.
    * @param gridColumnsCount Конфиг рендера плитки.
    * @return Избранный szMult.
    *         None, если szMult изменять нет необходимости.
    */
  def wideSzMult(bm: BlockMeta, gridColumnsCount: Int): Option[MSzMult] = {
    // Мультипликатор размера рендера нужен только для full-expand.
    // Обычный wide-режим просто растягивает только фон силами ScWideMaker, и дополнительный szMult не нужен.
    OptionUtil.maybeOpt( bm.expandMode contains MBlockExpandModes.Full ) {
      Option {
        bm.h match {
          // Малый блок можно увеличивать в 1,2,3,4 раза:
          case BlockHeights.H140 =>
            val szMultI = gridColumnsCount / bm.w.relSz
            val szMultI2 = Math.min(4, szMultI)
            MSzMult.fromInt( szMultI2 )
          // Обычный блок-300 можно в 1 и 2 раза только:
          case BlockHeights.H300 =>
            val szMultI = gridColumnsCount / bm.w.relSz
            val szMultI2 = Math.min(2, szMultI)
            MSzMult.fromInt( szMultI2 )
          // Остальное - без растяжки.
          case BlockHeights.H460 =>
            val szMultI = gridColumnsCount.toDouble / bm.w.relSz.toDouble
            if (szMultI >= 1.5) MSzMults.`1.5`
            else null
          // Макс.блок - слишком жирен, чтобы ужирнять ещё:
          case _ =>
            null
        }
      }
    }
  }


  /** Сборка карты MSzMults для списка шаблонов блоков.
    *
    * @param tpls Список шаблонов блоков.
    * @param jdConf Конфиг рендера плитки.
    * @return Карта szMults по тегам.
    */
  def wideSzMults(tpls: TraversableOnce[Tree[JdTag]], jdConf: MJdConf): Map[JdTag, MSzMult] = {
    (for {
      tpl         <- tpls.toIterator
      tplJdt = tpl.rootLabel
      // Посчитать wideSzMult блока, если wide
      if tplJdt.name ==* MJdTagNames.STRIP
      bm          <- tplJdt.props1.bm.iterator
      expandMode  <- bm.expandMode.iterator
      if expandMode ==* MBlockExpandModes.Full
      wideSzMult  <- GridCalc.wideSzMult( bm, jdConf.gridColumnsCount ).iterator
      jdt         <- tpl.flatten
    } yield {
      jdt -> wideSzMult
    })
      .toMap
  }

}



/** Модель конфига для рассчёта колонок сетки.
  *
  * @param cellWidth Ширина одной рассчётной ячейки в css-пикселях.
  * @param maxColumns Максимально допустимое кол-во колонок.
  */
case class MGridCalcConf(
                          cellWidth      : BlockWidth,
                          maxColumns     : Int
                        ) {

  def colUnitWidthPx = cellWidth.value

  /** Ширина обрамления вокруг одного блока. */
  def cellPadding = BlockPaddings.Bp20.outlinePx

}

object MGridCalcConf {

  /** Настройки для дефолтовой сетки. */
  def PLAIN_GRID: MGridCalcConf = {
    MGridCalcConf(
      cellWidth    = BlockWidths.NARROW,
      maxColumns   = GridConst.CELL140_COLUMNS_MAX
    )
  }

  /** Чётная сетка -- сетка с чётным кол-вом колонок. */
  def EVEN_GRID: MGridCalcConf = {
    val bw = BlockWidths.NORMAL
    MGridCalcConf(
      maxColumns  = GridConst.CELL300_COLUMNS_MAX,
      cellWidth   = bw
    )
  }

}

