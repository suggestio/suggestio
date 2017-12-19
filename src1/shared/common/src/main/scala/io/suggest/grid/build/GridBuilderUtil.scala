package io.suggest.grid.build

import io.suggest.ad.blk.{BlockHeights, BlockWidths}
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.common.geom.d2.MSize2di
import io.suggest.common.html.HtmlConstants
import io.suggest.msg.ErrorMsgs
import japgolly.univeq._

import scala.annotation.tailrec

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.12.17 14:17
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
  * Для прослойки используется интерфейс IGridLevel, который позволяет делать это всё.
  *
  * 2. Поддержку широких карточек внутри плитки можно реализовать с помощью двухфазного прохода по
  * исходным item'ам:
  * - Сначала просто стоим плитку исходя из начальной доступности всех строк и неограниченной высоты.
  * - Если на первом шаге была хотя бы одна wide-карточка и ширина плитки > 2 (или 3?) ячеек,
  * то строим плитку заново с учётом wide-занятых строк, полученных на первом шаге, чтобы распихать карточки по
  * с учётом возникших ограничений по высоте.
  */
object GridBuilderUtil {


  /** Кросс-платформенный код сборки плитки.
    *
    * @param args Аргументы для рендера.
    * @return Результат сборки.
    */
  def buildGrid(args: MGridBuildArgs): MGridBuildResult = {
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
    def _colLine2PxCoords(column: Int, line: Int): MCoords2di = {
      MCoords2di(
        x = column * paddedCellWidthPx,
        y = Math.round(line * paddedCellHeightPx).toInt + args.offY
      )
    }

    /** Выполнение работы по размещению карточек на текущем уровне. */
    def _processGridLevel(level: IGridLevel): Iterator[MCoords2di] = {
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
        def step(i: Int): MCoords2di = {
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
    val colsInfo1: Array[MColumnState] = {
      val mcs0 = MColumnState()
      Array.fill( args.columnsCount )(mcs0)
    }

    val coords = _processGridLevel {
      new IGridLevel {
        override def colsCount: Int = args.columnsCount
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
      // TODO Opt тут не совсем оптимально, но нужно неленивую коллекцию, чтобы посчитать высоту и ширину плитки.
      // Изначально, тут вызывался .toJSArray, но когда код стал кросс-платформенным, это вызвало проблемы...
      .toVector

    val maxCellHeight = colsInfo1
      .iterator
      .map(_.heightUsed)
      .max
    val gridHeightPx = Math.round(maxCellHeight * paddedCellHeightPx).toInt

    val gridWidthPx = {
      val width0 = colsInfo1.count(_.heightUsed > 0) * paddedCellWidthPx - cellPaddingWidthPx
      Math.max(0, width0)
    }

    MGridBuildResult(
      coords = coords,
      gridWh = MSize2di(
        width   = gridWidthPx,
        height  = gridHeightPx
      )
    )
  }


}


