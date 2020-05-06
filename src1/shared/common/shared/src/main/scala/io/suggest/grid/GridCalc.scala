package io.suggest.grid

import io.suggest.ad.blk.{BlockHeights, BlockPaddings, BlockWidth, BlockWidths, MBlockExpandMode, MBlockExpandModes}
import io.suggest.common.geom.d2.{IWidth, MSize2di}
import io.suggest.dev.{MSzMult, MSzMults}
import io.suggest.jd.{MJdConf, MJdTagId}
import io.suggest.jd.tags.{JdTag, MJdTagNames}
import japgolly.univeq._
import scalaz.{EphemeralStream, Tree}

import scala.annotation.tailrec
import scala.collection.immutable.HashMap

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

    val targetCount = (
      (contSz.width - padding) /
      (conf.cellWidth.value * minSzMult + padding)
    ).toInt

    val evenGridColsCount = Math.min(
      conf.maxColumns,
      Math.max(1, targetCount)
    )
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
    * @param wh Метаданные исходного блока.
    * @param gridColumnsCount Конфиг рендера плитки.
    * @return Избранный szMult.
    *         None, если szMult изменять нет необходимости.
    */
  def wideSzMult(wh: MSize2di, gridColumnsCount: Int): Option[MSzMult] = {
    // Мультипликатор размера рендера нужен только для full-expand.
    // Обычный wide-режим просто растягивает только фон силами ScWideMaker, и дополнительный szMult не нужен.

    /* ~ bm.w.relSz = 1 | 2 | ... */
    val padOutLinePx = BlockPaddings.default.outlinePx

    // Тут min = 1, т.к. может быть и ноль в формуле, приводящий к division by zero.
    def hModulesCount = Math.max(
      1,
      wh.width / (BlockWidths.min.value + padOutLinePx)
    )

    Option {
      if (wh.height <= BlockHeights.H140.value + padOutLinePx) {
        // Малый блок можно увеличивать в 1,2,3,4 раза:
        val szMultI = gridColumnsCount / hModulesCount
        val szMultI2 = Math.min(4, szMultI)
        MSzMult.fromInt( szMultI2 )

      } else if (wh.height <= BlockHeights.H300.value + padOutLinePx) {
        // Обычный блок-300 можно в 1 и 2 раза только:
        val szMultI = gridColumnsCount / hModulesCount
        val szMultI2 = Math.min(2, szMultI)
        MSzMult.fromInt( szMultI2 )

      } else if (wh.height <= BlockHeights.H460.value + padOutLinePx) {
        // Остальное - без растяжки.
        val szMultI = gridColumnsCount.toDouble / hModulesCount
        if (szMultI >= 1.5) MSzMults.`1.5`
        else null

      } else {
        // Макс.блок - слишком жирен, чтобы ужирнять ещё:
        null
      }
    }
  }


  /** Быстрый тест на возможность наличия wideSzMult !только для блока!.
    * wide-мультипликаторы размера могут быть и для контента, но этот тест не поддерживает проверок по дереву.
    * @param jdt Тег блока.
    * @return true, если для блока возможно наличие
    */
  def mayHaveWideSzMult(jdt: JdTag): Boolean = {
    val p1 = jdt.props1
    (jdt.name ==* MJdTagNames.STRIP) &&
    (p1.expandMode contains[MBlockExpandMode] MBlockExpandModes.Full) &&
    p1.widthPx.nonEmpty && p1.heightPx.nonEmpty
  }

  /** Сборка карты MSzMults для списка шаблонов блоков.
    *
    * @param tpls Список шаблонов блоков.
    * @param jdConf Конфиг рендера плитки.
    * @return Карта szMults по тегам.
    */
  def wideSzMults(tpls: IterableOnce[Tree[(MJdTagId, JdTag)]], jdConf: MJdConf): HashMap[MJdTagId, MSzMult] = {
    (
      HashMap.newBuilder[MJdTagId, MSzMult] ++= (for {
        tpl         <- tpls.iterator
        tplJdtWithId = tpl.rootLabel
        tplJdt = tplJdtWithId._2
        // Посчитать wideSzMult блока, если wide
        if mayHaveWideSzMult(tplJdt)
        wh          <- tplJdt.props1.wh.iterator
        wideSzMult  <- GridCalc.wideSzMult( wh, jdConf.gridColumnsCount ).iterator
        (jdId, _)   <- EphemeralStream.toIterable( tpl.flatten ).iterator
      } yield {
        jdId -> wideSzMult
      })
    )
      .result()
  }

}



/** Модель конфига для рассчёта колонок сетки.
  *
  * @param cellWidth Ширина одной рассчётной ячейки в css-пикселях.
  * @param maxColumns Максимально допустимое кол-во колонок.
  * @param cellPadding Ширина обрамления вокруг одного блока.
  */
case class MGridCalcConf(
                          cellWidth      : BlockWidth,
                          maxColumns     : Int,
                          cellPadding    : Int,
                        ) {

  def colUnitWidthPx = cellWidth.value

}

object MGridCalcConf {

  /** Настройки для дефолтовой сетки. */
  private def PLAIN_GRID: MGridCalcConf = {
    MGridCalcConf(
      cellWidth    = BlockWidths.NARROW,
      maxColumns   = GridConst.CELL140_COLUMNS_MAX,
      cellPadding = BlockPaddings.Bp20.outlinePx,
    )
  }

  /** Чётная сетка -- сетка с чётным кол-вом колонок. */
  def EVEN_GRID: MGridCalcConf = {
    MGridCalcConf(
      cellWidth   = BlockWidths.NORMAL,
      maxColumns  = GridConst.CELL300_COLUMNS_MAX,
      // Тут для правильного рассчёта в GridCalc надо использовать
      cellPadding = BlockPaddings.Bp20.fullBetweenBlocksPx,
    )
  }

}

