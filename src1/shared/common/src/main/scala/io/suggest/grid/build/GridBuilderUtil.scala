package io.suggest.grid.build

import io.suggest.ad.blk.{BlockHeights, BlockMeta, BlockWidths}
import io.suggest.common.coll.Lists
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

    //println("==========================================")

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
        val xy = currLvl.currLineCol
        lazy val rootLvl = s0.levels.last  // TODO Opt Использовать object RootCtx напрямую?

        // Есть хотя бы один очередной блок на текущем уровне, требующий позиционирования.
        // Это может быть как просто блок, так и под-группа блоков.
        if ( xy.column >= currLvl.ctx.colsCount ) {
          // Конец текущей строки -- перейти на следующую строку:
          val currLvl2 = currLvl.stepToNextLine
          //println(stepCounter, "no more cols, next LINE: " + xy + " => " + currLvl2.currLineCol)
          _stepper(
            s0.withCurrLevel( currLvl2 )
          )

        } else if ( currLvl.ctx.getHeightUsed( xy.column) > xy.line) {
          // Текущая ячейка уже занята. Требуется переход на следующую ячейку.
          val currLvl2 = currLvl.stepToNextCell
          //println(stepCounter, "next COLUMN: " + xy + " => " + currLvl2.currLineCol)
          _stepper(
            s0.withCurrLevel( currLvl2 )
          )

        } else if (currLvl.reDoItems.nonEmpty) {
          // Есть хотя бы один item, требующий повторного позиционирования.
          val reDoItm = currLvl.reDoItems.head
          // Для повторного позиционирования создаём новый уровень и вертикальную проекцию.
          // Это поможет спозиционировать блок только по вертикали, не трогая горизонтальную координату.
          // Это сдвиг вниз, он удобен при конфликте с широкой карточкой за место на экране.
          val mgiProps = MGbBlock(
            bm = reDoItm.bm,
            orderN = Some( reDoItm.orderN )
          ) :: Nil
          // Используем rootLvl для сборки под-контекста, т.к. reDoItm.topLeft задано в абсолютных (root) координатах.
          val subLvl = MGbLevelState(
            ctx       = rootLvl.ctx.verticalSubGrid( reDoItm.topLeft ),
            restItems = mgiProps
          )
          //println("reDO: " + reDoItm + " => +subLvl=" + subLvl)
          // Обновить пока-текущий уровень, выкинув пройденный redo-элемент:
          val currLvl2 = currLvl.copy(
            reDoItems = currLvl.reDoItems.tail
          )
          val s2 = s0.withLevels(
            subLvl :: currLvl2 :: s0.levels.tail
          )
          _stepper( s2 )

        } else if (currLvl.restItems.nonEmpty) {
          //println(stepCounter, itemExt)
          currLvl.restItems.head match {

            // Это block-meta. Позиционируем ровно один (текущий) блок:
            case itemExt: MGbBlock =>
              val bm = itemExt.bm
              val currWide = MWideLine( currLvl.ctx.lineToAbs(xy.line), bm.h )
              //println( stepCounter, bm, currWide, s0.wides.mkString )

              if (
                // Текущий неширокий блок как-то пересекается (по высоте) с широкой карточкой?
                s0.isWideOverlaps(currWide) ||
                  // Ширина текущего блока влезает в текущую строку?
                  (!bm.wide  && bm.w.relSz > currLvl._getMaxCellWidthCurrLine()) //||
                  // Широкий блок подразумевает пустую строку для себя: TODO А надо ли это, если у нас reDo-акк?
                  //(bm.wide   && rootLvl.currLineCol.column > 0)
              ) {
                // Здесь нет места для текущего блока.
                val currLvl2 = currLvl.stepToNextLine
                //println(stepCounter, "newLine: " + xy.line + " => " + currLvl2.currLineCol.line)
                _stepper(
                  s0.withCurrLevel( currLvl2 )
                )

              } else {
                // Здесь влезет блок текущей ширины и высоты.
                // Отработать wide-карточку: разместить её в аккамуляторе wide-строк.

                val itemCellHeight = bm.h.relSz
                // Обновить состояние: проинкрементить col/line курсоры:
                val endColumnIndex = xy.column + bm.w.relSz
                val heightUsed = xy.line + itemCellHeight
                for ( ci <- xy.column until endColumnIndex ) {
                  currLvl.ctx.setHeightUsed( ci, heightUsed )
                }

                val currLvl2 = currLvl.copy(
                  restItems = currLvl.restItems.tail,
                  currLineCol = xy.withX( endColumnIndex )
                )
                val xyAbs = currLvl.ctx.colLineToAbs( xy )

                val res = MGbItemRes(
                  // Восстановить порядок, если индекс был передан из reDo-ветви.
                  orderN      = itemExt.orderN
                    .getOrElse( stepCounter ),
                  topLeft     = xyAbs,
                  bm          = bm
                )

                val mwlAbsOpt = OptionUtil.maybe(bm.wide)( currWide )

                // Если wide, то надо извлечь из results-аккамулятора элементы, конфликтующие по высоте с данным wide-блоком и запихать их в reDo-аккамулятор.
                val wideS2Opt = for {
                  // Если у нас wide-блок
                  mwlAbs <- mwlAbsOpt
                  // Оттранслировать его в абсолютные координаты.
                  (conflicting, ok) = s0.resultsAccRev.partition { res =>
                    /*val isConflict =*/ res.toWideLine overlaps mwlAbs
                    /*
                    if (isConflict)
                      println("conflict: " + res + " vs wide=" + mwlAbs + " mwlXY=" + xy)
                    isConflict
                    */
                  }
                  if conflicting.nonEmpty
                } yield {
                  //println( conflicting.size, ok.size )
                  // Есть конфликтующие item'ы. Надо закинуть их в reDoItems на родительском уровне.
                  val parentLvlOpt = s0.levels.tail.headOption
                  val modLvl0 = parentLvlOpt.getOrElse(currLvl2)
                  val modLvl2 = modLvl0.withReDoItems {
                    conflicting reverse_::: modLvl0.reDoItems
                  }

                  // В связи с извлечением некоторых item'ов снизу, надо откатить значения в колонках.
                  val rootLvl1 = rootLvl
                  for {
                    e  <- conflicting
                    ci <- e.topLeft.left until (e.topLeft.left + e.bm.w.relSz)
                  } {
                    val h0 = rootLvl1.ctx.getHeightUsed(ci)
                    rootLvl1.ctx.setHeightUsed(ci, h0 - e.bm.h.relSz)
                  }

                  s0.copy(
                    resultsAccRev = res :: ok,
                    levels = parentLvlOpt.fold {
                      // Родительского уровня не было, работа была на верхнем уровне.
                      modLvl2 :: s0.levels.tail
                    } { _ =>
                      // Был родительский уровень. Обновить оба уровня.
                      currLvl2 :: modLvl2 :: s0.levels.tail.tail
                    },
                    wides = mwlAbs :: s0.wides
                  )
                }

                val s2 = wideS2Opt.getOrElse {
                  // Не-wide блок. Просто закинуть данные в состояние.
                  s0.copy(
                    levels        = currLvl2 :: s0.levels.tail,
                    resultsAccRev = res :: s0.resultsAccRev,
                    wides         = Lists.prependOpt(mwlAbsOpt)(s0.wides)
                  )
                }

                _stepper(s2)

              }

            // ПодСписок блоков, значит это открытая focused-карточка. Позиционируем эти блоки вертикально:
            case itemExt: MGbSubItems =>
              val subItems = itemExt.subItems

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


  /** Сборка нового под-контекста для рендера под-плитки.
    * Используется для вертикального рендера раскрытых карточек, чтобы выстраивать плитку внутри некоторых сжатых границ.
    *
    * @param outerLineCol cell-координата верхнего левого угла портала.
    * @return Новый контекст [[IGridBuildCtx]].
    */
  def verticalSubGrid(outerLineCol: MCoords2di): IGridBuildCtx = {
    val cellWidth = BlockWidths.max.relSz
    // Бывает, что запрашиваемая плитка вылезает за пределамы исходной.
    // Исправляем это прямо здесь.
    val column2 = Math.max(0,
      Math.min( outer.colsCount - cellWidth + 1, outerLineCol.column )
    )
    val line2 = outerLineCol.line

    new IGridBuildCtx {
      override def colsCount: Int = cellWidth
      // Использовать subItems.max width?

      override def colLineToAbs(xy: MCoords2di): MCoords2di = {
        xy.copy(
          x = columnToAbs( xy.x ),
          y = lineToAbs( xy.y )
        )
      }

      override def columnToAbs(ci: Int) =
        ci + column2

      override def lineToAbs(cl: Int) = cl + line2

      override def getHeightUsed(ci: Int) = {
        val hostHeightUsed = outer.getHeightUsed( columnToAbs(ci) )
        Math.max(0, hostHeightUsed - line2)
      }

      override def setHeightUsed(ci: Int, heightUsed: Int): Unit = {
        val hostHeightUsed = heightUsed + line2
        val hostCi = columnToAbs( ci )
        outer.setHeightUsed( hostCi, hostHeightUsed )
      }

      override def toString: String = {
        outer.toString +
          "+" + line2 +
          "+" + column2
      }

    }
  }

  override def toString = getClass.getSimpleName
}


/** Класс состояния функции/цикла построения плитки.
  * Основная цель модели: дать возможность функции выборочно откатываться в шагах назад
  * и влиять на свои последующие шаги.
  *
  * @param levels Описание погружения на уровне. head - текущий уровень.
  * @param resultsAccRev Обратный аккамулятор результатов
  * @param wides Акк накопления инфы о широких блоках.
  */
case class MGbStepState(
                         levels             : List[MGbLevelState],
                         resultsAccRev      : List[MGbItemRes]        = Nil,
                         wides              : List[MWideLine]         = Nil,
                       ) {

  def isWideOverlaps(mwl: MWideLine): Boolean = {
    wides.exists( mwl.overlaps )
  }

  def withLevels(levels: List[MGbLevelState])             = copy(levels = levels)
  def withCurrLevel(level: MGbLevelState)                 = withLevels( level :: levels.tail )
  def withResultsAccRev(resultsAccRev: List[MGbItemRes])  = copy(resultsAccRev = resultsAccRev)
  def withWides(wides: List[MWideLine])                   = copy(wides = wides)

}


/**
  * @param restItems Прямой список данных для обработки. Т.е. и блоки, и коллекции блоков.
  * @param currLineCol Текущая координата в контексте текущей плитки. Т.е. она может быть неабсолютной.
  * @param reDoItems Аккамулятор элементов, которые требуется заново отпозиционировать.
  */
case class MGbLevelState(
                          ctx         : IGridBuildCtx,
                          restItems   : List[IGbBlockPayload],
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
