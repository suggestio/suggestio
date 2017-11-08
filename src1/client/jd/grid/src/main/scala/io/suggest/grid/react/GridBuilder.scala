package io.suggest.grid.react

import com.github.dantrain.react.stonecutter.{ItemProps, LayoutFunRes, PropsCommon}
import io.suggest.ad.blk.BlockHeights
import GridPropsExt._
import ItemPropsExt._
import io.suggest.common.html.HtmlConstants
import io.suggest.sjs.common.msg.ErrorMsgs
import japgolly.univeq._

import scala.annotation.tailrec
import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.11.17 14:46
  * Description: Утиль для билдинга плитки suggest.io на основе react-компонентов.
  *
  * Плитка s.io является гибкой и по вертикали, и по горизонтали.
  * С точки зрения использования react-stonecutter, это нечто среднее между
  * [[https://github.com/beijaflor-io/react-stonecutter/blob/master/src/layouts/horizontal.js]]
  * и [[https://github.com/dantrain/react-stonecutter/blob/master/src/layouts/pinterest.js]].
  */
class GridBuilder {

  /** Вернуть cell-ширину элемента. */
  private def _getCellWidth(item: ItemProps): Int = {
    item.ext.blockMeta.w.relSz
  }

  /** Вернуть cell-высоту элемента. */
  private def _getCellHeight(item: ItemProps): Int = {
    item.ext.blockMeta.h.relSz
  }

  private def _orZero(und: js.UndefOr[Int]) = und getOrElse 0


  /** stateless вычисления координат для плитки для указанных основе исходных item'ов.
    * Создан, чтобы использовать как статическую layout-функцию, т.е. состояние билда живёт только внутри.
    *
    * @param items Массив элементов плитки.
    * @param props Пропертисы компонента плитки.
    * @return Контейнер данных по расположению заданных элементов в плитке.
    */
  def stoneCutterLayout(items: js.Array[ItemProps], props: PropsCommon): LayoutFunRes = {
    // На основе массива items надо получить массив px-координат.
    // Это можно сделать через отображение с аккамуляторами.
    // Для упрощения и ускорения, аккамуляторы снаружи от map().

    val colsCount = props.columns

    val colsInfo: Array[MColumnState] = {
      val mcs0 = MColumnState()
      Array.fill(colsCount)(mcs0)
    }

    // leftPtrPx -- пиксельная координата левого угла. Возможно, она и не нужна.
    var leftPtrPx = 0
    // line и column -- это ячейки.
    var currLine = 0
    var currColumn = 0

    val szMultD = props.szMult.toDouble

    val cellWidthPx  = props.columnWidth
    val cellHeightPx = BlockHeights.min.value * szMultD

    val cellPaddingWidthPx  = _orZero( props.gutterWidth )
    val cellPaddingHeightPx = _orZero( props.gutterHeight )

    val paddedCellHeightPx = cellHeightPx + cellPaddingHeightPx
    val paddedCellWidthPx  = cellWidthPx  + cellPaddingWidthPx

    /** Детектирование текущей максимальной ширины в сетке в текущей строке. */
    def _getMaxCellWidthCurrLine(): Int = {
      var mw = 1
      @tailrec def __detect(i: Int): Int = {
        if (i < colsCount && colsInfo(i).heightUsed ==* currLine ) {
          mw += 1
          __detect(i + 1)
        } else {
          mw - 1
        }
      }
      __detect(currColumn)
    }

    // Посчитать cell-размер (в ячейках) на основе размера в пикселях.
    //def getCellSizeWidth(widthPx: Int)   = (widthPx + cellPaddingWidthPx) / paddedCellWidthPx
    //def getCellSizeHeight(heightPx: Int) = (heightPx + cellPaddingHeightPx) / paddedCellHeightPx

    /**
      * step() не может подобрать подходящий блок в текущей строке и хочет просто шагнуть в следующую ячейку,
      * оставив пустоту за собой.
      * Этот метод вносит одношаговые изменения в состояние.
      */
    def beforeStepToNextCell(): Unit = {
      currColumn += 1
      leftPtrPx += paddedCellWidthPx
    }

    /** step() переходит на следующую строку. Нужно внести изменения в состояние. */
    def beforeStepNextLine(): Unit = {
      currColumn = 0
      currLine += 1
      leftPtrPx = 0
    }

    def incrHeightUsed(ci: Int, incrBy: Int): Unit = {
      colsInfo(ci) = colsInfo(ci).addHeightUsed( incrBy )
    }

    // Наконец, пройтись по блокам.
    val coords = for (item <- items) yield {
      val itemCellWidth  = _getCellWidth( item )
      val itemCellHeight = _getCellHeight( item )

      // Собрать функцию поиска места для одного элемента, модифицирующую текущее состояние.
      @tailrec
      def step(i: Int): js.Array[Int] = {
        // В оригинале был for-цикл с ограничением на 1000 итераций на всю плитку. Тут 10 -- на один item.
        if (i >= 20) {
          // return -- слишком много итераций. Обычно это симптом зависона из-за ЛОГИЧЕСКОЙ ошибки в быдлокоде.
          throw new IllegalStateException(ErrorMsgs.ENDLESS_LOOP_MAYBE + HtmlConstants.SPACE + i)

        } else if (currColumn >= colsCount) {
          // Конец текущей строки -- перейти на следующую строку.
          beforeStepNextLine()
          step(i + 1)

          // В оригинале была ещё ветка: if this.is_only_spacers() == true ; break
        } else if ( colsInfo(currColumn).heightUsed ==* currLine ) {
          // Высота текущей колонки равна currLine.
          // есть место хотя бы для одного блока с минимальной шириной, выясним блок с какой шириной может влезть.
          val cellWidthMax = _getMaxCellWidthCurrLine()

          if (itemCellWidth <= cellWidthMax) {
            for {
              ci <- (currColumn until (currColumn + itemCellWidth)).iterator
              if ci < colsCount
            } {
              // TODO Тут было colsInfo(currColumn).heightUsed += itemCellHeight и ПОЧЕМУ-то это нормально работало. Удалить этот TODO, если всё будет работать после фикса.
              incrHeightUsed(ci, itemCellHeight)
              currColumn += 1
            }

            // Сообрать новые координаты для блока:
            val xy = js.Array(
              leftPtrPx,
              (currLine * paddedCellHeightPx).toInt
            )

            // Сдвинуть текущий left на ширину блока и padding
            leftPtrPx += itemCellWidth * cellWidthPx + cellPaddingWidthPx

            // Вернуть полученные px-координаты блока.
            xy

          } else {
            // Требуется переход в след.ячейку, оставив пустоту в этой ячейке.
            // TODO Opt может быть, тут требуется сразу переход на след.строку?
            incrHeightUsed(currColumn, 1)
            beforeStepToNextCell()
            step(i + 1)
          }
          // Если нет следующего блока - обход закончен.

        } else {
          // Требуется переход на следующую ячейку.
          beforeStepToNextCell()
          step(i + 1)
        }
      }

      // Запустить рассчёт координат для текущего блока:
      step(0)
    }

    val maxCellHeight = colsInfo
      .maxBy(_.heightUsed)
      .heightUsed
    val gridHeightPx = (maxCellHeight * paddedCellHeightPx).toInt

    val gridWidthPx = (colsInfo.count(_.heightUsed > 0) * paddedCellWidthPx).toInt

    // Помимо координат, надо вычислить итоговые размеры плитки.
    new LayoutFunRes {
      override val positions  = coords
      override val gridHeight = gridHeightPx
      override val gridWidth  = gridWidthPx
    }
  }

}
