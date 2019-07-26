package io.suggest.grid.build

import io.suggest.ad.blk.{BlockMeta, BlockWidths}
import io.suggest.common.coll.Lists
import io.suggest.common.empty.OptionUtil
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.common.geom.d2.MSize2di
import io.suggest.common.html.HtmlConstants
import io.suggest.dev.MSzMult
import io.suggest.msg.ErrorMsgs
import monocle.macros.GenLens
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import scalaz.Tree

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
  def buildGrid(args: MGridBuildArgs): MGridBuildResult = {
    // TODO XXX args.jdtWideSzMults содежит данные, которые надо тут задействовать для рассчёта wide height:
    // MColumnState перевести на список пиксельных промежутков с указателем на оккупирующие промежутки теги.

    // Чисто самоконтроль, потом можно выкинуть.
    if (args.jdConf.gridColumnsCount < BlockWidths.max.relSz)
      throw new IllegalArgumentException( ErrorMsgs.GRID_CONFIGURATION_INVALID + HtmlConstants.SPACE + args.jdConf +
        HtmlConstants.SPACE + args.jdConf.gridColumnsCount )

    val szMultedF = MSzMult.szMultedF( args.jdConf.szMult )
    val szMultD = szMultedF.szMultBaseOpt.get

    val cellWidthPx  = szMultedF( BlockWidths.min.value )
    //val cellHeightPx = szMultedF( BlockHeights.min.value )

    val paddingMultedPx = szMultedF( args.jdConf.blockPadding.fullBetweenBlocksPx )
    val cellPaddingWidthPx  = paddingMultedPx // _orZero( props.gutterWidth )
    //val cellPaddingHeightPx = paddingMultedPx // _orZero( props.gutterHeight )

    //val paddedCellHeightPx = cellHeightPx + cellPaddingHeightPx
    val paddedCellWidthPx  = cellWidthPx  + cellPaddingWidthPx

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
          val nextLinePx = currLvl.ctx.minHeightUsedAfterLine( xy.line ) + paddingMultedPx
          val currLvl2 = MGbLevelState.stepToNextLine(nextLinePx)( currLvl )
          _stepper(
            MGbStepState.replaceCurrLevel(currLvl2)(s0)
          )

        } else if ( currLvl.ctx.getHeightUsed( xy.column) > xy.line) {
          // Текущая ячейка уже занята. Требуется переход на следующую ячейку.
          val currLvl2 = MGbLevelState.stepToNextCell( currLvl )
          _stepper(
            MGbStepState.replaceCurrLevel(currLvl2)(s0)
          )

        } else if (currLvl.reDoItems.nonEmpty) {
          // Есть хотя бы один item, требующий повторного позиционирования.
          val reDoItm = currLvl.reDoItems.head
          // Для повторного позиционирования создаём новый уровень и вертикальную проекцию.
          // Это поможет спозиционировать блок только по вертикали, не трогая горизонтальную координату.
          // Это сдвиг вниз, он удобен при конфликте с широкой карточкой за место на экране.
          val gb2 = Tree.Leaf {
            val gb0 = reDoItm.gbBlock
            if (gb0.orderN contains reDoItm.orderN)
              reDoItm.gbBlock
            else
              MGbBlock.orderN.set( Some(reDoItm.orderN) )( gb0 )
          }

          // Используем rootLvl для сборки под-контекста, т.к. reDoItm.topLeft задано в абсолютных (root) координатах.
          val subLvl = MGbLevelState(
            ctx       = rootLvl.ctx.verticalSubGrid( reDoItm.topLeft ),
            restItems = gb2 #:: Stream.empty,
          )
          // Обновить пока-текущий уровень, выкинув пройденный redo-элемент:
          val currLvl2 = MGbLevelState.reDoItems.modify(_.tail)(currLvl)
          val s2 = MGbStepState.levels.modify { levels0 =>
            subLvl :: currLvl2 :: levels0.tail
          }(s0)
          _stepper( s2 )

        } else if (currLvl.restItems.nonEmpty) {
          currLvl.restItems.head match {

            // Это block-meta. Позиционируем ровно один (текущий) блок:
            case Tree.Leaf(itemExt) =>
              val bm = itemExt.bmOpt.get
              val wideSzMultOpt = itemExt.jdtOpt.flatMap( args.jdtWideSzMults.get )
              val blockHeightPx = szMultedF( bm.h.value, wideSzMultOpt )
              val currWide = MWideLine(
                startLine = currLvl.ctx.lineToAbs( xy.line ),
                heightPx  = blockHeightPx,
              )

              if (
                // Текущий неширокий блок как-то пересекается (по высоте) с широкой карточкой?
                s0.isWideOverlaps(currWide) ||
                  // Ширина текущего блока влезает в текущую строку?
                  (!bm.wide  && bm.w.relSz > currLvl.getMaxCellWidthCurrLine()) // ||
                  // Широкий блок подразумевает пустую строку для себя: это не надо, т.к. используется reDo-акк. TODO А может тоже надо?
                  //(bm.wide   && rootLvl.currLineCol.column > 0)
              ) {
                // Здесь нет места для текущего блока.
                val nextLinePx = currLvl.ctx.minHeightUsedAfterLine( xy.line ) + paddingMultedPx
                val currLvl2 = MGbLevelState.stepToNextLine(nextLinePx)( currLvl )
                _stepper(
                  MGbStepState.replaceCurrLevel(currLvl2)(s0)
                )

              } else {
                // Здесь влезет блок текущей ширины и высоты.
                // Отработать wide-карточку: разместить её в аккамуляторе wide-строк.
                val endColumnIndex = xy.column + bm.w.relSz
                val pxIvl2 = MPxInterval(
                  startPx = currLvl.ctx.lineToAbs( xy.line ),
                  sizePx  = blockHeightPx,
                  block   = itemExt,
                )
                // Обновить состояние: проинкрементить col/line курсоры:
                val updateColumnStateF = MColumnState.occupiedRev.modify( pxIvl2 :: _ )
                for ( ci <- xy.column until endColumnIndex ) {
                  currLvl.ctx.updateHeightUsed( ci )(updateColumnStateF)
                }

                val currLvl2 = currLvl.copy(
                  restItems   = currLvl.restItems.tail,
                  currLineCol = MCoords2di.x.set(endColumnIndex)(xy),
                )
                val xyAbs = currLvl.ctx.colLineToAbs( xy )

                val orderN = itemExt.orderN
                  .getOrElse( stepCounter )

                val mwlAbsOpt = OptionUtil.maybe(bm.wide)( currWide )

                // Т.к. фон wide-блока центруется независимо от контента, для этого используется искусственный wide-блок,
                // идущий перед wide-блоком с контентом. Надо закинуть wide-фоновый-блок в res-аккамулятор.
                val wideBgWidthOpt = itemExt.wideBgSz
                  .map { wideBgSz =>
                    // Есть размер фона. Надо совместить горизонтальную середины плитки и изображения.
                    // Поправочный szMult вычисляется через отношение высот картинки и самого блока. В норме должен быть == 1. Из проблем: он пережевывает и скрывает ошибки.
                    // TODO Im Кажется, будто поправка img2blkSzMult не нужна на новых версиях ImageMagick (7.0.7+), но нужна на старых (6.8.9).
                    val img2blkSzMult = blockHeightPx / wideBgSz.height.toDouble
                    wideBgSz.width * img2blkSzMult
                  }
                  .orElse {
                    OptionUtil.maybe( bm.wide && !args.jdConf.isEdit ) {
                      // wide-блок без фоновой картинки. Взять ширину такого блока из jdConf:
                      args.jdConf.plainWideBlockWidthPx
                    }
                  }

                val res = MGbItemRes(
                  // Восстановить порядок, если индекс был передан из reDo-ветви.
                  orderN        = orderN,
                  topLeft       = xyAbs,
                  bm            = bm,
                  forceCenterX  = wideBgWidthOpt,
                  gbBlock       = itemExt,
                  wide          = currWide,
                )

                // Если wide, то надо извлечь из results-аккамулятора элементы, конфликтующие по высоте с данным wide-блоком и запихать их в reDo-аккамулятор.
                val s1DeConflictedOpt = for {
                  // Если у нас wide-блок
                  mwlAbs <- mwlAbsOpt
                  // Оттранслировать его в абсолютные координаты.
                  (conflicting, ok) = s0.resultsAccRev.partition { res =>
                    res.wide overlaps mwlAbs
                  }
                  if conflicting.nonEmpty
                } yield {
                  // Есть конфликтующие item'ы. Надо закинуть их в reDoItems на родительском уровне.
                  val parentLvlOpt = s0.levels.tail.headOption
                  val modLvl0 = parentLvlOpt getOrElse currLvl2
                  val modLvl2 = MGbLevelState.reDoItems
                    .modify(conflicting reverse_::: _)( modLvl0 )

                  // В связи с извлечением некоторых item'ов снизу, надо откатить значения в колонках.
                  val rootLvl1 = rootLvl
                  for {
                    e  <- conflicting
                    updateLens = MColumnState.occupiedRev.modify { mcs0 =>
                      mcs0.filter { pxIvl =>
                        pxIvl.block !===* e.gbBlock
                      }
                    }
                    ci <- e.topLeft.left until (e.topLeft.left + e.bm.w.relSz)
                  } {
                    rootLvl1.ctx.updateHeightUsed(ci)( updateLens )
                  }

                  // Обновить состояние билдера:
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

                val s1 = s1DeConflictedOpt.getOrElse {
                  // Не-wide блок. Просто закинуть данные в состояние.
                  s0.copy(
                    levels        = currLvl2 :: s0.levels.tail,
                    resultsAccRev = res :: s0.resultsAccRev,
                    wides         = Lists.prependOpt(mwlAbsOpt)(s0.wides)
                  )
                }

                // 2018-01-24 Решено, что после wide-карточки на focused-уровне надо рендерить дальнейшую карточку не в столбик,
                // а в обычном порядке по ширине всей плитки. Для этого можно объеденить два последних уровня, но лучше
                // подменить контекст на текущем уровне на root, чтобы не нарушать порядок рендера.
                val s2 = if (
                  bm.wide && !currLvl2.ctx.isRoot &&
                    // Нельзя делать сброс, если последующий элемент тоже wide. Это решает только последний из первых wide-элементов.
                    !currLvl2.restItems.headOption.exists(_.headIsWide) &&
                    // 2018-01-30 Запретить этот сброс, если остался только один элемент: один болтающийся элемент выглядит не очень.
                    currLvl2.restItems.lengthCompare(1) > 0
                ) {
                  //println( itemExt, currLvl2.restItems.headOption, !currLvl2.restItems.headOption.exists(_.headIsWide) )
                  val currLvl3 = s1.levels.head.copy(
                    ctx = rootLvl.ctx,
                    currLineCol = rootLvl.currLineCol
                  )
                  MGbStepState.replaceCurrLevel( currLvl3 )(s1)
                } else {
                  s1
                }

                _stepper(s2)

              }

            // ПодСписок блоков, значит это открытая focused-карточка. Позиционируем эти блоки вертикально:
            case Tree.Node(_, subItems)  =>

              // Создаём виртуальный контекст рекурсивного рендера плитки, и погружаемся в новый уровень рендера.
              // Это нужно, чтобы раскрыть одну карточку вниз, а не слева-направо.
              val nextLvl = MGbLevelState(
                ctx = currLvl.ctx.verticalSubGrid( currLvl.currLineCol ),
                restItems = subItems
              )
              // Выкидываем текущий пройденный элемент с текущего уровня.
              val currLvl2 = MGbLevelState.restItems.modify(_.tail)(currLvl)
              val s2 = MGbStepState.levels.modify { levels0 =>
                nextLvl :: currLvl2 :: levels0.tail
              }(s0)

              _stepper( s2 )
          }


        } else {
          // Нет на текущем уровне ничего, что следует делать. Подняться уровнем выше.
          val s2 = MGbStepState.levels.modify(_.tail)(s0)
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
        Array.fill( args.jdConf.gridColumnsCount )(mcs0)
      }

      override def columnToAbs(ci: Int) = ci
      override def lineToAbs(cl: Int) = cl
      override def colLineToAbs(xy: MCoords2di) = xy

      override def colsCount = args.jdConf.gridColumnsCount

      /** Прочитать состояние уже использованной высоты для указанной колонки. */
      override def getHeightUsed(ci: Int) = {
        colsInfo(ci).heightUsed
      }

      override def updateHeightUsed(ci: Int)(updateF: MColumnState => MColumnState): Unit =
        colsInfo(ci) = updateF(colsInfo(ci))

      override def isRoot = true
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

    // Рассчёт финальных габаритов плитки: высота.
    val maxCellHeight = RootCtx.colsInfo
      .iterator
      .map(_.heightUsed)
      .max

    // Ширина всей плитки:
    val gridWidthPx = {
      val busyColsCount = RootCtx.colsInfo.count(_.heightUsed > 0)
      val width0 = busyColsCount * paddedCellWidthPx - cellPaddingWidthPx
      Math.max(0, width0)
    }

    val gridWh = MSize2di(
      width   = gridWidthPx,
      height  = maxCellHeight + args.offY,
    )

    // Скомпилировать финальные координаты.
    val coordsFinal = s9
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
          // Эксплуатация костыля по абсолютной центровке какого-то блока вместо расположения в плитке:
          x = res.forceCenterX.fold {
            res.topLeft.x * paddedCellWidthPx
          } { widthOrigPx =>
            // Отцентровать используя указанный сдвиг относительно центра плитки.
            ((gridWidthPx - widthOrigPx) * szMultD / 2).toInt // ((gridWidthPx * szMultD / 2).toInt + centerOffsetX) / 2
          },
          y = res.topLeft.y + args.offY, //Math.round(res.topLeft.y * paddedCellHeightPx).toInt + args.offY
        )
      }
      // НЕ ЛЕНИВАЯ коллекция, но это скорее дань традициям. Раньше на toStream/toSeq всё ломалось, т.к. переменные обновлялись прямо внутри итератора. Сейчас уже наверное можно и toStream делать.
      .toList

    MGridBuildResult(
      coords = coordsFinal,
      gridWh = gridWh
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

  /** Обновить состояние использованной высоты у указанной колонки.
    *
    * @param columnIndexRel Относительный индекс колонки в контексте данной проекции.
    * @param updateF Обновление АБСОЛЮТНЫХ данных по проекции.
    */
  def updateHeightUsed(columnIndexRel: Int)(updateF: MColumnState => MColumnState): Unit

  def isRoot: Boolean

  /** Поиск следующей пустой строки после указанной. */
  def minHeightUsedAfterLine(linePx: Int): Int = {
    val iter = (0 until colsCount)
      .iterator
      .map( getHeightUsed )
      .filter(_ > linePx)

    if (iter.isEmpty) linePx
    else iter.min
  }

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
      Math.min( outer.colsCount - cellWidth, outerLineCol.column )
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

      override def updateHeightUsed(ci: Int)(updateF: MColumnState => MColumnState): Unit = {
        val hostCi = columnToAbs( ci )
        outer.updateHeightUsed( hostCi)( updateF )
      }

      override def toString: String = {
        outer.toString +
          HtmlConstants.PLUS + line2 +
          HtmlConstants.PLUS + column2
      }

      override final def isRoot = false

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


}
object MGbStepState { outer =>
  val levels = GenLens[MGbStepState](_.levels)

  def replaceCurrLevel(level: MGbLevelState) =
    levels.modify { lvs0 => level :: lvs0.tail }
}


object MGbLevelState {

  val restItems   = GenLens[MGbLevelState](_.restItems)
  val currLineCol = GenLens[MGbLevelState](_.currLineCol)
  val reDoItems   = GenLens[MGbLevelState](_.reDoItems)

  /** step() переходит на следующую строку. Нужно внести изменения в состояние. */
  def stepToNextLine(nextLinePx: Int) = {
    currLineCol.modify { clc0 =>
      clc0.copy(
        x = 0,
        y = nextLinePx,
      )
    }
  }

  /**
    * step() не может подобрать подходящий блок в текущей строке и хочет просто шагнуть в следующую ячейку,
    * оставив пустоту за собой.
    * Этот метод вносит одношаговые изменения в состояние.
    */
  val stepToNextCell = currLineCol
    .composeLens(MCoords2di.x)
    .modify(_ + 1)

}
/**
  * @param restItems Прямой список данных для обработки. Т.е. и блоки, и коллекции блоков.
  * @param currLineCol Текущая координата в контексте текущей плитки. Т.е. она может быть неабсолютной.
  *                    Y (строка) - в пикселях, начиная с 26 июля 2019 (до этого - был тоже в ячейках стандартного размера).
  *                    X (колонка) - номер текущей колонки.
  * @param reDoItems Аккамулятор элементов, которые требуется заново отпозиционировать.
  */
case class MGbLevelState(
                          ctx         : IGridBuildCtx,
                          restItems   : Stream[Tree[MGbBlock]],
                          currLineCol : MCoords2di              = MCoords2di(0, 0),
                          reDoItems   : List[MGbItemRes]        = Nil,
                        ) {


  /** Детектирование текущей максимальной ширины в сетке в текущей строке. */
  def getMaxCellWidthCurrLine(): Int = {
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

}


/** Результат позиционирования одного блока.
  *
  * @param orderN Порядковый номер блока.
  * @param topLeft Отпозиционированные координаты.
  * @param bm Инфа по текущему блоку.
  * @param forceCenterX Костыль для принудительной центровки по X вместо координат по сетке.
  */
case class MGbItemRes(
                       orderN           : Int,
                       topLeft          : MCoords2di,
                       bm               : BlockMeta,
                       forceCenterX     : Option[Double]   = None,
                       gbBlock          : MGbBlock,
                       wide             : MWideLine,
                     )
