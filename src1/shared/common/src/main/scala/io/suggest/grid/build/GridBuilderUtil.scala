package io.suggest.grid.build

import io.suggest.ad.blk.{BlockHeights, BlockMeta, BlockWidths}
import io.suggest.common.empty.OptionUtil
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

  /** Кросс-платформенный код сборки плитки с нетривиальной рекурсией, имеющей состояние.
    *
    * @param args Аргументы для рендера.
    * @return Результат сборки.
    */
  def buildGrid[Coords_t](args: MGridBuildArgs[Coords_t]): MGridBuildResult[Coords_t] = {
    // Чисто самоконтроль, потом можно выкинуть.
    if (args.jdConf.gridColumnsCount < BlockWidths.max.relSz)
      throw new IllegalArgumentException( ErrorMsgs.GRID_CONFIGURATION_INVALID + HtmlConstants.SPACE + args.jdConf +
        HtmlConstants.SPACE + args.jdConf.gridColumnsCount )

    println("==========================================")

    // Глобальный счётчик шагов рекурсии. Нужен как для поддержания порядка item'ов, так и для защиты от бесконечной рекурсии.
    var stepCounter = 0

    // Функция, строящая плитку. Может двигаться как вперёд по распределяемым блокам, так и отшагивать назад при необходимости.
    @tailrec
    def _stepper(s0: MGbStepState): MGbStepState = {
      // Защита от бесконечной рекурсии:
      if (stepCounter > 2000)
        throw new IllegalStateException( ErrorMsgs.ENDLESS_LOOP_MAYBE )

      stepCounter += 1

      // Вспоминаем, что надо сделать и на каком мы уровне сейчас:
      if (s0.levels.isEmpty) {
        // Опустел список уровней для обхода, выход из цикла рекурсии.
        s0

      } else {
        val currLvl = s0.levels.head

        // Есть хотя бы один очередной блок на текущем уровне, требующий позиционирования.
        // Это может быть как просто блок, так и под-группа блоков.
        if ( currLvl.currLineCol.column >= currLvl.ctx.colsCount ) {
          // Конец текущей строки -- перейти на следующую строку:
          val currLvl2 = currLvl.stepToNextLine
          _stepper(
            s0.withCurrLevel( currLvl2 )
          )

        } else if ( currLvl.ctx.getHeightUsed( currLvl.currLineCol.column) > currLvl.currLineCol.line) {
          // Текущая ячейка уже занята. Требуется переход на следующую ячейку.
          val currLvl2 = currLvl.stepToNextCell
          _stepper(
            s0.withCurrLevel( currLvl2 )
          )

        } else if (currLvl.reDoItems.nonEmpty) {
          // Есть хотя бы один item, требующий повторного позиционирования.
          val reDoItm = currLvl.reDoItems.head
          // Для повторного позиционирования создаём новый уровень и вертикальную проекцию.
          // Это поможет спозиционировать блок только по вертикали, не трогая горизонтальную координату.
          // Это сдвиг вниз, он удобен при конфликте с широкой карточкой за место на экране.
          val mgiProps = MGridItemProps(
            Left(reDoItm.bm),
            orderN = Some( reDoItm.orderN )
          ) :: Nil
          // Используем rootLvl для сборки под-контекста, т.к. reDoItm.topLeft задано в абсолютных (root) координатах.
          val rootLvl = s0.levels.last  // TODO Opt Использовать object RootCtx напрямую?
          val subLvl = MGbLevelState(
            ctx       = rootLvl.ctx.verticalSubGrid( reDoItm.topLeft ),
            restItems = mgiProps
          )
          // Обновить пока-текущий уровень, выкинув пройденный redo-элемент:
          val currLvl2 = currLvl.copy(
            reDoItems = currLvl.reDoItems.tail
          )
          val s2 = s0.withLevels(
            subLvl :: currLvl2 :: s0.levels.tail
          )
          _stepper( s2 )

        } else if (currLvl.restItems.nonEmpty) {
          val itemExt = currLvl.restItems.head
          if (itemExt.blockMetaOrChildren.isLeft) {
            // Это block-meta. Позиционируем ровно один (текущий) блок:
            val bm = itemExt.blockMetaOrChildren.left.get
            def _currAsWide(line: Int = currLvl.currLineCol.line) = MWideLine(line, bm.h)

            if (
              // Текущий неширокий блок как-то пересекается (по высоте) с широкой карточкой?
              (!bm.wide && currLvl.ctx.isWideLineBusy(_currAsWide())) ||
              // Ширина текущего блока влезает в текущую строку?
              bm.w.relSz > currLvl._getMaxCellWidthCurrLine()
            ) {
              // Здесь нет места для текущего блока.
              val currLvl2 = currLvl.stepToNextLine
              _stepper(
                s0.withCurrLevel( currLvl2 )
              )

            } else {
              // Здесь влезет блок текущей ширины и высоты.
              // Отработать wide-карточку: разместить её в аккамуляторе wide-строк.

              val xy = currLvl.currLineCol

              // TODO XXX Wide-строки надо переписать: Сделать простой List-акк прямо в состоянии, ВНЕ контекста,
              //     и использовать только абсолютные координаты для MWideLine.
              //     На фоне относительных координат тут происходит какая-то путаница. MWideLines можно будет выкинуть.

              // Инфа wide-аккамулятора, если это wide-блок.
              val mwlOpt = OptionUtil.maybe(bm.wide) {
                // Это wide-карточка. Вместо colsInfo заполняем данными wide-аккамулятор.
                // Перейти на след.строку, если в текущая строка уже занята хотя бы одним элементом.
                val isWideRenderNextLine = xy.column > 0
                val wideStartLine = if (isWideRenderNextLine)
                  xy.line + 1
                else
                  xy.line

                // Занять текущую строку под wide-карточку.
                val mwl0 = _currAsWide(wideStartLine)
                val mwl2 = currLvl.ctx.getWideLine( mwl0 )
                println("WIDE: " + mwl0 + " => " + mwl2)
                // TODO Надо чинить currLine с учётом mwl? По идее, надо wide-line закинуть в состояние, и перенести в reDoAcc конфликтующие блоки.
                //if (mwl2.startLine > currLine)
                //  currLine = mwl2.startLine
                //mwl2.height
                mwl2
              }

              val itemCellHeight = bm.h.relSz
              // Обновить состояние: проинкрементить col/line курсоры:
              val heightUsed = xy.line + itemCellHeight

              val endColumnIndex = xy.column + bm.w.relSz
              for ( ci <- xy.column until endColumnIndex ) {
                currLvl.ctx.setHeightUsed( ci, heightUsed )
              }

              val currLvl2 = currLvl.copy(
                restItems = currLvl.restItems.tail,
                currLineCol = currLvl.currLineCol.withX( endColumnIndex )
              )
              val xyAbs = currLvl.ctx.colLineToAbs( xy )
              println("xy: " + xy + " => " + xyAbs + " wide=" + mwlOpt.orNull)

              val res = MGbItemRes(
                // Восстановить порядок, если индекс был передан из reDo-ветви.
                orderN      = itemExt.orderN
                  .getOrElse( stepCounter ),
                topLeft     = xyAbs,
                bm          = bm
              )

              // Если wide, то надо извлечь из results-аккамулятора элементы, конфликтующие по высоте с данным wide-блоком и запихать их в reDo-аккамулятор.
              val wideS2Opt = for {
                // Если у нас wide-блок
                mwl <- mwlOpt
                // Оттранслировать его в абсолютные координаты.
                mwlAbs = mwl.withStartLine( currLvl2.ctx.lineToAbs( mwl.startLine ) )
                (conflicting, ok) = s0.resultsAccRev.partition { res =>
                  // TODO >= или > -- уточнить. Если нижняя граница блока будет наезжать сверху на wide-блок, то надо >=
                  val isConflict = res.toWideLine overlaps mwlAbs
                  if (isConflict)
                    println("conflict: " + res + " vs wide=" + mwlAbs + " mwlXY=" + xy)
                  isConflict
                }
                if conflicting.nonEmpty
              } yield {
                println( conflicting.size, ok.size )
                // Есть конфликтующие item'ы. Надо закинуть их в reDoItems на родительском уровне.
                val parentLvlOpt = s0.levels.tail.headOption
                val modLvl0 = parentLvlOpt.getOrElse(currLvl2)
                val modLvl2 = modLvl0.withReDoItems {
                  conflicting reverse_::: modLvl0.reDoItems
                }
                s0.copy(
                  resultsAccRev = res :: ok,
                  levels = parentLvlOpt.fold {
                    // Родительского уровня не было, работа была на верхнем уровне.
                    modLvl2 :: s0.levels.tail
                  } { _ =>
                    // Был родительский уровень. Обновить оба уровня.
                    currLvl2 :: modLvl2 :: s0.levels.tail.tail
                  }
                )
              }

              val s2 = wideS2Opt.getOrElse {
                // Не-wide блок. Просто закинуть данные в состояние.
                s0.copy(
                  levels        = currLvl2 :: s0.levels.tail,
                  resultsAccRev = res :: s0.resultsAccRev
                )
              }

              _stepper(s2)

            }

          } else {
            // ПодСписок блоков, значит это открытая focused-карточка. Позиционируем эти блоки вертикально:
            val subItems = itemExt.blockMetaOrChildren.right.get

            // Создаём виртуальный контекст рекурсивного рендера плитки, и погружаемся в новый уровень рендера.
            // Это нужно, чтобы раскрыть одну карточку вниз, а не слева-направо.
            val nextLvl = MGbLevelState(
              ctx = currLvl.ctx.verticalSubGrid( currLvl.currLineCol ),
              restItems = subItems
            )
            // Выкидываем текущий пройденный элемент с текущего уровня.
            val currLvl2 = currLvl.copy(
              restItems = currLvl.restItems.tail
            )
            val s2 = s0.withLevels(
              nextLvl :: currLvl2 :: s0.levels.tail
            )

            _stepper( s2 )
          }

        } else {
          // Нет на текущем уровне ничего, что следует делать. Подняться уровнем выше.
          val s2 = s0.withLevels(
            s0.levels.tail
          )
          _stepper( s2 )
        }
      }

    }   // def _stepper()


    /** Контекст рендера плитки на самом верхнем уровне.
      * Все подконтексты проецируются на основе этого контекста.
      */
    object RootCtx extends IGridBuildCtx {

      // Инициализация аккамулятора wide-строк.
      var wideLinesAcc = MWideLines()

      // Инициализация состояния плитки, в котором будет хранится инфа по высоте колонок.
      val colsInfo: Array[MColumnState] = {
        val mcs0 = MColumnState()
        Array.fill( args.columnsCount )(mcs0)
      }

      override def columnToAbs(ci: Int) = ci
      override def lineToAbs(cl: Int) = cl
      override def colLineToAbs(xy: MCoords2di) = xy

      override def colsCount: Int = {
        args.columnsCount
      }

      /** Прочитать состояние уже использованной высоты для указанной колонки. */
      override def getHeightUsed(ci: Int) = {
        colsInfo(ci).heightUsed
      }

      override def setHeightUsed(ci: Int, heightUsed: Int): Unit = {
        val mcs2 = colsInfo(ci)
          .withHeightUsed( heightUsed )
        colsInfo(ci) = mcs2
      }

      // TODO Нужна реорганизация wide-line-аккамулятора: перенести его в состояние stepper'а.
      override def getWideLine(mwl0: MWideLine): MWideLine = {
        // Поиск и резервирование доступных wide-строк в wide-аккамуляторе.
        // 1. Собрать все overlapping-элементы.
        // 2. Впихнуть в них всё необходимое.
        val (mwls2, mwl2) = wideLinesAcc.push(mwl0)
        wideLinesAcc = mwls2
        mwl2
      }

      override def isWideLineBusy(args: MWideLine) = {
        val isBusy = wideLinesAcc.isBusy(args)
        println( "WIDES === " + wideLinesAcc.lines.mkString(" | ") + "  busy?" + args + "??" + isBusy )
        isBusy
      }

    }


    // Начальное состояние цикла.
    val s9 = _stepper(
      MGbStepState(
        levels = MGbLevelState(
          ctx       = RootCtx,
          restItems = args.itemsExtDatas
        ) :: Nil
      )
    )

    val blkSzMultD   = args.jdConf.blkSzMult.toDouble
    val cellWidthPx  = Math.round(BlockWidths.min.value * blkSzMultD).toInt // props.columnWidth
    val cellHeightPx = BlockHeights.min.value * blkSzMultD

    val szMultD = args.jdConf.szMult.toDouble
    val paddingMultedPx = Math.round(args.jdConf.blockPadding.value * szMultD).toInt
    val cellPaddingWidthPx  = paddingMultedPx // _orZero( props.gutterWidth )
    val cellPaddingHeightPx = paddingMultedPx // _orZero( props.gutterHeight )

    val paddedCellHeightPx = cellHeightPx + cellPaddingHeightPx
    val paddedCellWidthPx  = cellWidthPx  + cellPaddingWidthPx

    // Скомпилировать финальные координаты.
    val coordsFinal = args.iter2coordsF {
      s9
        .resultsAccRev
        // Восстановить исходный порядок. Сначала быстрый реверс, затем досортировка.
        .reverse
        // Доп.сортировка требуется, т.к. мелкие нарушения порядка происходят при конфликтах wide-блоков с
        // предшествующими им блоками в соседних колонках. После реверса тут сравнителньо немного перестановок.
        .sortBy(_.orderN)
        .iterator
        // Заменить колонки и строки на пиксели.
        .map { res =>
          MCoords2di(
            x = res.topLeft.x * paddedCellWidthPx,
            y = Math.round(res.topLeft.y * paddedCellHeightPx).toInt + args.offY
          )
        }
    }

    // Рассчёт финальных габаритов плитки: высота.
    val maxCellHeight = RootCtx.colsInfo
      .iterator
      .map(_.heightUsed)
      .max
    val gridHeightPx = Math.round(maxCellHeight * paddedCellHeightPx).toInt

    // Ширина всей плитки:
    val gridWidthPx = {
      val busyColsCount = RootCtx.colsInfo.count(_.heightUsed > 0)
      val width0 = busyColsCount * paddedCellWidthPx - cellPaddingWidthPx
      Math.max(0, width0)
    }

    MGridBuildResult(
      coords = coordsFinal,
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
trait IGridBuildCtx { outer =>

  /** Кол-во колонок в текущей проекции. */
  def colsCount: Int

  /** Трансляция cell-координаты из контекстной координаты в абсолютную координату плитки. */
  def colLineToAbs(xy: MCoords2di): MCoords2di

  def columnToAbs(ci: Int): Int
  def lineToAbs(cl: Int): Int

  /** Прочитать состояние уже использованной высоты для указанной колонки. */
  def getHeightUsed(ci: Int): Int

  /** Обновить состояние использованной высоты у указанной колонки. */
  def setHeightUsed(ci: Int, heightUsed: Int): Unit

  /** Поиск первой полностью свободной (от края до края) строки.
    * Очевидно, что после этой строки всё свободно.
    *
    * @return Исходный или иной экземпляр [[MWideLine]].
    */
  def getWideLine(mwl0: MWideLine): MWideLine

  /** Проверка, занята ли указанная строка? */
  def isWideLineBusy(args: MWideLine): Boolean


  /** Сборка нового под-контекста для рендера под-плитки.
    * Используется для вертикального рендера раскрытых карточек, чтобы выстраивать плитку внутри некоторых сжатых границ.
    *
    * @param outerLineCol cell-координата верхнего левого угла портала.
    * @return Новый контекст [[IGridBuildCtx]].
    */
  def verticalSubGrid(outerLineCol: MCoords2di): IGridBuildCtx = {
    new IGridBuildCtx {
      override def colsCount: Int = BlockWidths.max.relSz  // subItems.max?

      override def colLineToAbs(xy: MCoords2di): MCoords2di = {
        xy.copy(
          x = columnToAbs( xy.x ),
          y = lineToAbs( xy.y )
        )
      }

      // TODO Если элемент крайний правый с минимальной шириной, то нужен сдвиг влево на 1 ячейку
      override def columnToAbs(ci: Int) = {
        if (ci >= outer.colsCount)
          throw new IllegalArgumentException("out of ci bounds = " + ci)
        ci + outerLineCol.column
      }

      override def lineToAbs(cl: Int) = cl + outerLineCol.line

      override def getHeightUsed(ci: Int) = {
        val hostHeightUsed = outer.getHeightUsed( columnToAbs(ci) )
        Math.max(0, hostHeightUsed - outerLineCol.line)
      }

      override def setHeightUsed(ci: Int, heightUsed: Int): Unit = {
        val hostHeightUsed = heightUsed + outerLineCol.line
        val hostCi = columnToAbs( ci )
        outer.setHeightUsed( hostCi, hostHeightUsed )
      }

      override def getWideLine(args: MWideLine): MWideLine = {
        val hostWlWanted = args.withStartLine( lineToAbs(args.startLine) )
        val hostWlAssiged = outer.getWideLine(hostWlWanted)
        hostWlAssiged.withStartLine( hostWlAssiged.startLine - outerLineCol.line )
      }

      override def isWideLineBusy(args: MWideLine): Boolean = {
        val hostWlWanted = args.withStartLine( lineToAbs(args.startLine) )
        outer.isWideLineBusy( hostWlWanted )
      }
    }
  }

}


/** Класс состояния функции/цикла построения плитки.
  * Основная цель модели: дать возможность функции выборочно откатываться в шагах назад
  * и влиять на свои последующие шаги.
  *
  * @param levels Описание погружения на уровне. head - текущий уровень.
  * @param resultsAccRev Обратный аккамулятор результатов
  */
case class MGbStepState(
                         levels             : List[MGbLevelState],
                         resultsAccRev      : List[MGbItemRes]        = Nil
                       ) {

  def withLevels(levels: List[MGbLevelState])             = copy(levels = levels)
  def withCurrLevel(level: MGbLevelState)                 = withLevels( level :: levels.tail )
  def withResultsAccRev(resultsAccRev: List[MGbItemRes])  = copy(resultsAccRev = resultsAccRev)

}


/**
  * @param restItems Прямой список данных для обработки. Т.е. и блоки, и коллекции блоков.
  * @param currLineCol Текущая координата в контексте текущей плитки. Т.е. она может быть неабсолютной.
  *
  * @param reDoItems Аккамулятор элементов, которые требуется заново отпозиционировать.
  */
case class MGbLevelState(
                          ctx         : IGridBuildCtx,
                          restItems   : List[MGridItemProps],
                          currLineCol : MCoords2di              = MCoords2di(0, 0),
                          reDoItems   : List[MGbItemRes]        = Nil,
                        ) {

  def withCurrLineCol(currLineCol: MCoords2di)    = copy(currLineCol = currLineCol)
  def withReDoItems(reDoItems: List[MGbItemRes])  = copy(reDoItems = reDoItems)


  /** Детектирование текущей максимальной ширины в сетке в текущей строке. */
  def _getMaxCellWidthCurrLine(): Int = {
    // TODO Следует выкинуть var, занеся её в args, например.
    var mw = 1
    @tailrec def __detect(i: Int): Int = {
      if (i < ctx.colsCount && ctx.getHeightUsed(i) <= currLineCol.line ) {
        mw += 1
        __detect(i + 1)
      } else {
        mw - 1
      }
    }
    __detect( currLineCol.column )
  }

  //def getHeightUsedCurrColumn = ctx.getHeightUsed( currLineCol.column )

  /**
    * step() не может подобрать подходящий блок в текущей строке и хочет просто шагнуть в следующую ячейку,
    * оставив пустоту за собой.
    * Этот метод вносит одношаговые изменения в состояние.
    */
  def stepToNextCell = {
    withCurrLineCol(
      currLineCol
        .withX( currLineCol.x + 1 )
    )
  }

  /** step() переходит на следующую строку. Нужно внести изменения в состояние. */
  def stepToNextLine = {
    withCurrLineCol(
      currLineCol.copy(
        x = 0,
        y = currLineCol.y + 1
      )
    )
  }

}


/** Результат позиционирования одного блока.
  *
  * @param orderN Порядковый номер блока.
  * @param topLeft Отпозиционированные координаты.
  * @param bm Инфа по текущему блоку.
  */
case class MGbItemRes(
                       orderN           : Int,
                       topLeft          : MCoords2di,
                       bm               : BlockMeta
                     ) {

  lazy val toWideLine = MWideLine(topLeft.y, bm.h)

}
