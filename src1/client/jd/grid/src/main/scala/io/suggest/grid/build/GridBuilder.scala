package io.suggest.grid.build

import com.github.dantrain.react.stonecutter.{ItemProps, LayoutFunRes, PropsCommon}
import io.suggest.ad.blk.{BlockHeights, BlockWidths}
import io.suggest.common.geom.d2.MSize2di
import io.suggest.common.html.HtmlConstants
import io.suggest.jd.MJdConf
import io.suggest.sjs.common.msg.ErrorMsgs
import japgolly.univeq._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.annotation.tailrec
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSConverters._

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

  /** stateless вычисления координат для плитки для указанных основе исходных item'ов.
    * Создан, чтобы использовать как статическую layout-функцию, т.е. состояние билда живёт только внутри.
    *
    * @param items Массив элементов плитки.
    * @param props Пропертисы компонента плитки.
    * @return Контейнер данных по расположению заданных элементов в плитке.
    */
  def stoneCutterLayout(args: GridBuildArgs)(items: js.Array[ItemProps], props: PropsCommon): LayoutFunRes = {
    // На основе массива items надо получить массив px-координат.
    // Это можно сделать через отображение с аккамуляторами.
    // Для упрощения и ускорения, аккамуляторы снаружи от map().

    val colsCount = props.columns

    val colsInfo: Array[MColumnState] = {
      val mcs0 = MColumnState()
      Array.fill(colsCount)(mcs0)
    }

    // leftPtrPx -- пиксельная координата левого угла. Возможно, она и не нужна.
    // line и column -- это координата текущей ячейки
    var leftPtrPx, currLine, currColumn = 0

    val blkSzMultD   = args.jdConf.blkSzMult.toDouble
    val cellWidthPx  = Math.round(BlockWidths.min.value * blkSzMultD).toInt // props.columnWidth
    val cellHeightPx = BlockHeights.min.value * blkSzMultD

    val szMultD = args.jdConf.szMult.toDouble
    val paddingMultedPx = Math.round(2 * args.jdConf.blockPadding.value * szMultD).toInt
    val cellPaddingWidthPx  = paddingMultedPx // _orZero( props.gutterWidth )
    val cellPaddingHeightPx = paddingMultedPx // _orZero( props.gutterHeight )

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
    val coords = items.iterator
      // zip() тут скорее для самоконтроля: чтобы при проблемах было лучше видно сами проблемы.
      .zip( args.itemsExtDatas.toIterator )
      .map { case (_, itemExt) =>
        val bm = itemExt.blockMeta
        val itemCellWidth  = bm.w.relSz
        val itemCellHeight = bm.h.relSz

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
                Math.round(currLine * paddedCellHeightPx).toInt + args.offY
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
      .toJSArray

    val maxCellHeight = colsInfo
      .iterator
      .map(_.heightUsed)
      .max
    val gridHeightPx = Math.round(maxCellHeight * paddedCellHeightPx).toInt

    val gridWidthPx = colsInfo.count(_.heightUsed > 0) * paddedCellWidthPx

    // Помимо координат, надо вычислить итоговые размеры плитки.
    val res = new LayoutFunRes {
      override val positions  = coords
      override val gridHeight = gridHeightPx
      override val gridWidth  = gridWidthPx
    }

    // Передать результат сборки плитки в side-effect-функцию, если она задана.
    for (notifyF <- args.onLayout) {
      Future {
        // На раннем этапе нужны были только фактические размеры плитки.
        // Поэтому собираем отдельный безопасный инстанс с этими размерами и отправляем в функцию.
        val gridSz2d = MSize2di(width = gridWidthPx, height = gridHeightPx)
        notifyF( gridSz2d )
      }
    }

    res
  }

  //private def _orZero(und: js.UndefOr[Int]) = und getOrElse 0

}


/** Модель доп.аргументов вызова функции, которые мы передаём вне react.
  *
  * В изначальной реализации была пародия на monkey-patching, что вызывало негодование
  * со стороны react, и добавляло неопределённости относительно надёжности и долговечности такого решения.
  *
  * @param itemsExtDatas Итератор или коллекция доп.данных для исходного массива ItemProps, длина должна совпадать.
  * @param onLayout Callback для сайд-эффектов по итогам рассчёта плитки.
  * @param offY Сдвиг сетки по вертикали, если требуется.
  */
case class GridBuildArgs(
                          itemsExtDatas : TraversableOnce[ItemPropsExt],
                          jdConf        : MJdConf,
                          onLayout      : Option[MSize2di => _] = None,
                          offY          : Int = 0
                        )

