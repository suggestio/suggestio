package io.suggest.grid.build

import com.github.dantrain.react.stonecutter.{ItemProps, LayoutFunRes, PropsCommon}
import io.suggest.ad.blk.{BlockHeights, BlockWidths}
import io.suggest.common.geom.d2.MSize2di
import io.suggest.common.html.HtmlConstants
import io.suggest.grid.MWideLine
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
  *
  *
  * 2017-12-14:
  * 1. Возможность принудительного вертикального выстраивания карточек в произвольных вертикальных рамках
  * внутри более широкой исходной плитки реализуется через зуммирование общего состояния главной плики на под-плитки.
  * Переменные состояния каждой под-плитки скрыто транслируются в общее состояние и обратно.
  * Таким образом, помимо основной рекурсии по выстраиванию блоков, есть ещё рекурсивное погружение в под-уровни
  * для рендера суб-блоков текущего блока.
  * Для прослойки используется интерфейс [[IGridLevel]], который позволяет делать это всё.
  *
  * 2. Поддержку широких карточек внутри плитки можно реализовать с помощью двухфазного прохода по
  * исходным item'ам:
  * - Сначала просто стоим плитку исходя из начальной доступности всех строк и неограниченной высоты.
  * - Если на первом шаге была хотя бы одна wide-карточка и ширина плитки > 2 (или 3?) ячеек,
  * то строим плитку заново с учётом wide-занятых строк, полученных на первом шаге, чтобы распихать карточки по
  * с учётом возникших ограничений по высоте.
  */
class GridBuilder {

  /** stateless вычисления координат для плитки для указанных основе исходных item'ов.
    * Создан, чтобы использовать как статическую layout-функцию, т.е. состояние билда живёт только внутри.
    *
    * @param flatJsItems Плоский массив элементов плитки, переданный через stonecutter.
    *                    Не используется напрямую: дерево данных по item'ам передаётся напрямую в args.
    * @param props Пропертисы компонента плитки.
    * @return Контейнер данных по расположению заданных элементов в плитке.
    */
  def stoneCutterLayout(args: GridBuildArgs)(flatJsItems: js.Array[ItemProps], props: PropsCommon): LayoutFunRes = {
    // Чисто самоконтроль, потом можно выкинуть.
    if (args.jdConf.gridColumnsCount < BlockWidths.max.relSz)
      throw new IllegalArgumentException( ErrorMsgs.GRID_CONFIGURATION_INVALID + HtmlConstants.SPACE + args.jdConf +
        HtmlConstants.SPACE + args.jdConf.gridColumnsCount )

    val blkSzMultD   = args.jdConf.blkSzMult.toDouble
    val cellWidthPx  = Math.round(BlockWidths.min.value * blkSzMultD).toInt // props.columnWidth
    val cellHeightPx = BlockHeights.min.value * blkSzMultD

    val szMultD = args.jdConf.szMult.toDouble
    val paddingMultedPx = Math.round(args.jdConf.blockPadding.value * szMultD).toInt
    val cellPaddingWidthPx  = paddingMultedPx // _orZero( props.gutterWidth )
    val cellPaddingHeightPx = paddingMultedPx // _orZero( props.gutterHeight )

    val paddedCellHeightPx = cellHeightPx + cellPaddingHeightPx
    val paddedCellWidthPx  = cellWidthPx  + cellPaddingWidthPx


    /** Конверсия координат ячейки плитки в css-пиксельные координаты. */
    def _colLine2PxCoords(column: Int, line: Int): js.Array[Int] = {
      js.Array(
        column * paddedCellWidthPx,
        Math.round(line * paddedCellHeightPx).toInt + args.offY
      )
    }

    /** Выполнение работы по размещению карточек на текущем уровне. */
    def _processGridLevel(level: IGridLevel): TraversableOnce[js.Array[Int]] = {
      // line и column -- это координата текущей ячейки
      var currLine, currColumn = 0

      /** Детектирование текущей максимальной ширины в сетке в текущей строке. */
      def _getMaxCellWidthCurrLine(): Int = {
        // TODO Следует выкинуть var, занеся её в args, например.
        var mw = 1
        @tailrec def __detect(i: Int): Int = {
          if (i < level.colsCount && level.colsInfo(i).heightUsed ==* currLine ) {
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
      }

      /** step() переходит на следующую строку. Нужно внести изменения в состояние. */
      def beforeStepNextLine(): Unit = {
        currColumn = 0
        currLine += 1
      }

      def incrHeightUsed(ci: Int, incrBy: Int): Unit = {
        val mcs2 = level.colsInfo(ci).addHeightUsed( incrBy )
        level.updateColsInfo(ci, mcs2)
      }

      // Наконец, пройтись по блокам.
      for {
        itemExt <- level.itemsExtDatas.toIterator
      } yield {
        val bm = itemExt.blockMeta
        val itemCellWidth  = bm.w.relSz
        val itemCellHeight = bm.h.relSz

        // Собрать функцию поиска места для одного элемента, модифицирующую текущее состояние.
        @tailrec
        def step(i: Int): js.Array[Int] = {
          // В оригинале был for-цикл с ограничением на 1000 итераций на всю плитку. Тут -- ограничение итераций на каждый item.
          if (i >= 20) {
            // return -- слишком много итераций. Обычно это симптом зависона из-за ЛОГИЧЕСКОЙ ошибки в быдлокоде.
            throw new IllegalStateException(ErrorMsgs.ENDLESS_LOOP_MAYBE + HtmlConstants.SPACE + i)

          } else if (currColumn >= level.colsCount) {
            // Конец текущей строки -- перейти на следующую строку.
            beforeStepNextLine()
            step(i + 1)

            // В оригинале была ещё ветка: if this.is_only_spacers() == true ; break
          } else if ( level.colsInfo(currColumn).heightUsed ==* currLine ) {
            // Высота текущей колонки равна currLine.
            // есть место хотя бы для одного блока с минимальной шириной, выясним блок с какой шириной может влезть.
            val cellWidthMax = _getMaxCellWidthCurrLine()

            if (itemCellWidth <= cellWidthMax) {
              // Собрать новые координаты для блока:
              val xy = _colLine2PxCoords(line = currLine, column = currColumn)

              // Обновить состояние: проинкрементить col/line курсоры:
              for {
                ci <- (currColumn until (currColumn + itemCellWidth)).iterator
                if ci < level.colsCount
              } {
                incrHeightUsed(ci, itemCellHeight)
                currColumn += 1
              }

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
        //.toJSArray снаружи вызывается поверх результатов всех итераторов со всех уровней.
    }


    // Инициализация состояния плитки, в котором будет всё храниться.
    val colsCount1 = props.columns

    val colsInfo1: Array[MColumnState] = {
      val mcs0 = MColumnState()
      Array.fill(colsCount1)(mcs0)
    }

    val coords = _processGridLevel {
      new IGridLevel {
        override def colsCount: Int = colsCount1
        override def colsInfo(ci: Int): MColumnState = colsInfo1(ci)
        override def updateColsInfo(i: Int, mcs: MColumnState): Unit = {
          colsInfo1(i) = mcs
        }
        override def itemsExtDatas = args.itemsExtDatas
        //override def getWideLine(args: MWideLine): MWideLine = {
          // Поиск и резервирование доступных wide-строк в wide-аккамуляторе.
          // 1. Собрать все overlapping-элементы.
          // 2. Впихнуть в них всё необходимое.
        //  ???
        //}
      }
    }
      .toJSArray

    val maxCellHeight = colsInfo1
      .iterator
      .map(_.heightUsed)
      .max
    val gridHeightPx = Math.round(maxCellHeight * paddedCellHeightPx).toInt

    val gridWidthPx = {
      val width0 = colsInfo1.count(_.heightUsed > 0) * paddedCellWidthPx - cellPaddingWidthPx
      Math.max(0, width0)
    }

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


/** Интерфейс для взаимодействия с состоянием плитки.
  * Позволяет зуммировать состояние над-плитки.
  */
trait IGridLevel {

  /** Элементы для обработки на текущем уровне. */
  def itemsExtDatas: TraversableOnce[ItemPropsExt]

  /** Кол-во колонок в текущей проекции. */
  def colsCount: Int

  /** Прочитать состояние указанной колонки */
  def colsInfo(ci: Int): MColumnState

  /** Обновить состояние указанной колонки. */
  def updateColsInfo(i: Int, mcs: MColumnState): Unit

  /** Поиск первой полностью свободной (от края до края) строки.
    * Очевидно, что после этой строки всё свободно.
    *
    * @return Исходный или иной экземпляр [[MWideLine]].
    */
  //def getWideLine(args: MWideLine): MWideLine

}

