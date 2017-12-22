package io.suggest.grid.build

import io.suggest.ad.blk.{BlockHeights, BlockWidths}
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.common.geom.d2.MSize2di
import io.suggest.common.html.HtmlConstants
import io.suggest.msg.ErrorMsgs

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
  * то строим плитку заново с учётом wide-занятых строк, полученных на первом шаге, чтобы распихать карточки
  * с учётом возникших ограничений по высоте.
  */
object GridBuilderUtil {


  /** Кросс-платформенный код сборки плитки.
    *
    * @param args Аргументы для рендера.
    * @return Результат сборки.
    */
  def buildGrid[Coords_t](args: MGridBuildArgs[Coords_t]): MGridBuildResult[Coords_t] = {
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

    /** Выполнение работы по размещению карточек на текущем уровне. */
    def _processGridLevel(ctx: IGridBuildCtx): Iterator[MCoords2di] = {
      // line и column -- это координата текущей ячейки
      var currLine, currColumn = 0

      /** Детектирование текущей максимальной ширины в сетке в текущей строке. */
      def _getMaxCellWidthCurrLine(): Int = {
        // TODO Следует выкинуть var, занеся её в args, например.
        var mw = 1
        @tailrec def __detect(i: Int): Int = {
          if (i < ctx.colsCount && ctx.getHeightUsed(i) <= currLine ) {
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
      def stepToNextCell(): Unit = {
        currColumn += 1
      }

      /** step() переходит на следующую строку. Нужно внести изменения в состояние. */
      def stepToNextLine(): Unit = {
        currColumn = 0
        currLine += 1
      }

      // Наконец, пройтись по блокам.
      ctx
        .itemsExtDatas
        .toIterator
        .flatMap { itemExt =>
          // Новые функции надо впихивать внутрь рекурсии, иначе это всё закончится перевпихиванием функции внутрь рекурсии.
          @tailrec
          def step(i: Int): Iterator[MCoords2di] = {
            // В оригинале был for-цикл с ограничением кол-ва итераций на всю плитку. Тут -- ограничение итераций на каждый item.
            if (i >= 50) {
              // return -- слишком много итераций. Обычно это симптом зависона из-за ЛОГИЧЕСКОЙ ошибки в быдлокоде.
              throw new IllegalStateException(ErrorMsgs.ENDLESS_LOOP_MAYBE + HtmlConstants.SPACE + i)

            } else if ( currColumn >= ctx.colsCount ) {
              // Конец текущей строки -- перейти на следующую строку:
              stepToNextLine()
              step(i + 1)

            } else if (ctx.getHeightUsed(currColumn) > currLine) {
              // Текущая ячейка уже занята. Требуется переход на следующую ячейку.
              stepToNextCell()
              step(i + 1)

            } else if (itemExt.blockMetaOrChildren.isLeft) {
              val bm = itemExt.blockMetaOrChildren.left.get
              def _currAsWide(line: Int = currLine) = MWideLine(line, bm.h)

              if (
                // Текущий блок как-то пересекается (по высоте) с широкой карточкой?
                ctx.isWideLineBusy(_currAsWide()) ||
                // Ширина текущего блока влезает в текущую строку?
                bm.w.relSz > _getMaxCellWidthCurrLine()
              ) {
                // Здесь нет места для текущего блока.
                stepToNextLine()
                step(i + 1)

              } else {
                // Здесь влезтет блок текущей ширины и высоты.
                // Отработать wide-карточку: разместить её в аккамуляторе wide-строк.
                if (bm.wide) {
                  // Это wide-карточка. Вместо colsInfo заполняем данными wide-аккамулятор.
                  // Перейти на след.строку, если в текущая строка уже занята хотя бы одним элементом.
                  val isWideRenderNextLine = currColumn > 0
                  val wideStartLine = if (isWideRenderNextLine) currLine + 1 else currLine

                  // Занять текущую строку под wide-карточку.
                  val mwl = ctx.getWideLine( _currAsWide(wideStartLine) )
                  //println("WIDE: " + bm + " => " + mwl + " cl=" + currLine + " col=" + currColumn)
                }

                // Собрать новые координаты для блока:
                val xy = MCoords2di(x = currColumn, y = currLine)

                val itemCellHeight = bm.h.relSz
                // Обновить состояние: проинкрементить col/line курсоры:
                val heightUsed = currLine + itemCellHeight
                for {
                  ci <- (currColumn until (currColumn + bm.w.relSz)).iterator
                  if ci < ctx.colsCount
                } {
                  ctx.setHeightUsed( ci, heightUsed )
                  //incrHeightUsed(ci, itemCellHeight)
                  currColumn += 1
                }

                // Вернуть полученные px-координаты блока.
                Iterator.single( xy )
              }

            } else if (itemExt.blockMetaOrChildren.isRight) {
              // Focused-карточка. Рендерим все блоки вертикально.
              val subItems = itemExt.blockMetaOrChildren.right.get

              val _currLine = currLine
              val _currColumn = currColumn
              // Создаём виртуальный контекст рекурсивного рендера плитки, и погружаемся в новый уровень рендера.
              // Это нужно, чтобы раскрыть одну карточку вниз, а не слева-направо.
              _processGridLevel {
                new IGridBuildCtx {
                  override def colsCount: Int = BlockWidths.max.relSz  // subItems.max?

                  override def itemsExtDatas = subItems

                  // TODO Если элемент крайний правый с минимальной шириной, то нужен сдвиг влево на 1 ячейку
                  def _translateColumn2Host(ci: Int) = {
                    if (ci >= ctx.colsCount)
                      throw new IllegalArgumentException("out of ci bounds = " + ci)
                    ci + _currColumn
                  }

                  def _translateLine2Host(cl: Int) = cl + _currLine

                  override def getHeightUsed(ci: Int) = {
                    val hostHeightUsed = ctx.getHeightUsed( _translateColumn2Host(ci) )
                    Math.max(0, hostHeightUsed - _currLine)
                  }

                  override def setHeightUsed(ci: Int, heightUsed: Int): Unit = {
                    val hostHeightUsed = heightUsed + _currLine
                    val hostCi = _translateColumn2Host( ci )
                    ctx.setHeightUsed( hostCi, hostHeightUsed )
                  }

                  override def getWideLine(args: MWideLine): MWideLine = {
                    val hostWlWanted = args.withStartLine( _translateLine2Host(args.startLine) )
                    val hostWlAssiged = ctx.getWideLine(hostWlWanted)
                    hostWlAssiged.withStartLine( hostWlAssiged.startLine - _currLine )
                  }

                  override def wideLines(): MWideLines = ctx.wideLines()

                  override def isWideLineBusy(args: MWideLine): Boolean = {
                    val hostWlWanted = args.withStartLine( _translateLine2Host(args.startLine) )
                    ctx.isWideLineBusy( hostWlWanted )
                  }
                }
              }
                // Исправить координаты в результатах.
                .map { xy =>
                  xy.copy(
                    x = xy.x + currColumn,
                    y = xy.y + currLine
                  )
                }

            } else {
              // should never happen
              println( ErrorMsgs.SHOULD_NEVER_HAPPEN )
              Iterator.empty
            }
          }

          step(0)
        }
    }

    // Инициализация состояния плитки, в котором будет всё храниться.
    val colsInfo1: Array[MColumnState] = {
      val mcs0 = MColumnState()
      Array.fill( args.columnsCount )(mcs0)
    }

    // Инициализация аккамулятора wide-строк.
    var wideLinesAcc = MWideLines()

    val coords = args.iter2coordsF {
      _processGridLevel {
        new IGridBuildCtx {
          override def colsCount: Int =
            args.columnsCount

          /** Прочитать состояние уже использованной высоты для указанной колонки. */
          override def getHeightUsed(ci: Int) = {
            colsInfo1(ci).heightUsed
          }

          override def setHeightUsed(ci: Int, heightUsed: Int): Unit = {
            val mcs2 = colsInfo1(ci)
              .withHeightUsed( heightUsed )
            colsInfo1(ci) = mcs2
          }

          override def wideLines() = wideLinesAcc

          override def itemsExtDatas = args.itemsExtDatas

          override def getWideLine(args: MWideLine): MWideLine = {
            // Поиск и резервирование доступных wide-строк в wide-аккамуляторе.
            // 1. Собрать все overlapping-элементы.
            // 2. Впихнуть в них всё необходимое.
            val (mwls2, mwl2) = wideLinesAcc.push(args)
            wideLinesAcc = mwls2
            mwl2
          }

          override def isWideLineBusy(args: MWideLine) =
            wideLinesAcc.isBusy(args)
        }
      }
        .map { colLine =>
          // Нужно перевести строки и столбцы в пиксели внутри контейнера
          MCoords2di(
            x = colLine.x * paddedCellWidthPx,
            y = Math.round(colLine.y * paddedCellHeightPx).toInt + args.offY
          )
        }
    }

    // TODO Если есть wide-строки, то надо снова выстроить плитку, но уже с опустошением имеющегося wide-аккамулятора.

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



/** Интерфейс для взаимодействия с состоянием плитки.
  * Позволяет зуммировать состояние над-плитки.
  */
trait IGridBuildCtx {

  /** Элементы для обработки на текущем уровне. */
  def itemsExtDatas: TraversableOnce[MGridItemProps]

  /** Кол-во колонок в текущей проекции. */
  def colsCount: Int

  /** Прочитать состояние уже использованной высоты для указанной колонки. */
  //def colsInfo(ci: Int): MColumnState
  def getHeightUsed(ci: Int): Int

  /** Обновить состояние использованной высоты у указанной колонки. */
  //def updateColsInfo(ci: Int, mcs: MColumnState): Unit
  def setHeightUsed(ci: Int, heightUsed: Int): Unit

  def wideLines(): MWideLines

  /** Поиск первой полностью свободной (от края до края) строки.
    * Очевидно, что после этой строки всё свободно.
    *
    * @return Исходный или иной экземпляр [[MWideLine]].
    */
  def getWideLine(args: MWideLine): MWideLine

  /** Проверка, занята ли указанная строка? */
  def isWideLineBusy(args: MWideLine): Boolean

}

