package io.suggest.grid.build

import io.suggest.ad.blk.BlockWidths
import io.suggest.common.coll.Lists.Implicits._
import io.suggest.common.empty.OptionUtil
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.common.geom.d2.MSize2di
import io.suggest.common.html.HtmlConstants
import io.suggest.dev.MSzMult
import io.suggest.img.ImgCommonUtil
import io.suggest.jd.tags.MJdTagNames
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
  *
  *
  * 2019-07-26:
  * По вертикали использовать пиксели вместо ячеек. Это позволяет реализовать "резиновые" по вертикали блоки,
  * в т.ч. блоки контента описания динамической высоты, полоски с растянутым контентом, и динамический padding ячеек.
  */
object GridBuilderUtil {

  /** Кросс-платформенный код сборки плитки с нетривиальной рекурсией, имеющей состояние.
    *
    * @param args Аргументы для рендера.
    * @return Результат сборки.
    */
  def buildGrid(args: MGridBuildArgs): MGridBuildResult = {
    // MColumnState перевести на список пиксельных промежутков с указателем на оккупирующие промежутки теги.

    // Чисто самоконтроль, потом можно выкинуть.
    if (args.jdConf.gridColumnsCount < BlockWidths.max.relSz)
      throw new IllegalArgumentException( ErrorMsgs.GRID_CONFIGURATION_INVALID + HtmlConstants.SPACE + args.jdConf +
        HtmlConstants.SPACE + args.jdConf.gridColumnsCount )

    val szMultedF = MSzMult.szMultedF( args.jdConf.szMult )

    val cellWidthPx  = szMultedF( BlockWidths.min.value )
    //val cellHeightPx = szMultedF( BlockHeights.min.value )

    val paddingMultedPx = szMultedF( args.jdConf.blockPadding.fullBetweenBlocksPx )
    val cellPaddingWidthPx  = paddingMultedPx
    //val cellPaddingHeightPx = paddingMultedPx

    //val paddedCellHeightPx = cellHeightPx + cellPaddingHeightPx
    // TODO Тут проблема: если padding != 20, то эта примитивная формула рассчёта средней ширины ячейки ошибается, т.к. нет такой ширины:
    // внутри блока неЕдиничной ширины всегда есть постоянный padding=20.
    val paddedCellWidthPx  = cellWidthPx + cellPaddingWidthPx


    // Глобальный счётчик шагов рекурсии. Нужен как для поддержания порядка item'ов, так и для защиты от бесконечной рекурсии.
    var stepCounter = 0

    // Функция, строящая плитку. Может двигаться как вперёд по распределяемым блокам, так и отшагивать назад при необходимости.
    @tailrec
    def _stepper(s0: MGbStepState): MGbStepState = {
      // Защита от бесконечной рекурсии:
      if (stepCounter > 5000)
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
          //println(s"step to next line: ${xy.line} => $nextLinePx - column overflow")
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
          //println(s"reDo item re-position: ${reDoItm.gbBlock.nodeId.orNull}")
          // Для повторного позиционирования создаём новый уровень и вертикальную проекцию.
          // Это поможет спозиционировать блок только по вертикали, не трогая горизонтальную координату.
          // Это сдвиг вниз, он удобен при конфликте с широкой карточкой за место на экране.
          val gb2 = Tree.Leaf {
            val gb0 = reDoItm.gbBlock
            if (gb0.orderN contains reDoItm.orderN) gb0
            else MGbBlock.orderN.set( Some(reDoItm.orderN) )( gb0 )
          }

          // Используем rootLvl для сборки под-контекста, т.к. reDoItm.topLeft задано в абсолютных (root) координатах.
          val subLvl = MGbLevelState(
            ctx       = rootLvl.ctx.verticalSubGrid( reDoItm.topLeft ),
            restItems = gb2 #:: LazyList.empty,
          )
          // Обновить пока-текущий уровень, выкинув пройденный redo-элемент:
          val currLvl2 = MGbLevelState.reDoItems.modify(_.tail)(currLvl)
          val s2 = MGbStepState.levels
            .modify { levels0 =>
              subLvl :: currLvl2 :: levels0.tail
            }(s0)
          _stepper( s2 )

        } else if (currLvl.restItems.nonEmpty) {
          val gbTree = currLvl.restItems.head

          // Здесь был match{Tree.Node/Tree.Leaf}, но scalac-2.13.1 ругается на unreachable code, поэтому используется if.
          val subItems = gbTree.subForest
          if (subItems.isEmpty) {
            // Это block-meta. Позиционируем ровно один (текущий) блок:
            val gb = gbTree.rootLabel

            lazy val wideSzMultOpt = args.jdtWideSzMults.get( gb.jdId )

            val gbSize = gb.size.get

            val blockHeightPx =
              if (gbSize.heightPx.isSzMulted) gbSize.heightPx.sizePx
              else szMultedF( gbSize.heightPx.sizePx, wideSzMultOpt )

            val topPxAbs = currLvl.ctx.lineToAbs( xy.line )
            val currWide = MWideLine(
              topPx     = topPxAbs,
              heightPx  = blockHeightPx,
            )

            if (
              // Текущий неширокий блок как-то пересекается (по высоте) с широкой карточкой?
              (s0.wides overlaps currWide) ||
              // Широкий блок подразумевает пустую строку для себя, в т.ч. на самом ВЕРХНЕМ уровне.
              (if (gbSize.expandMode.nonEmpty) rootLvl.ctx.getMaxCellWidth(topPxAbs) < rootLvl.ctx.colsCount
               // !wide: Ширина текущего блока влезает в текущую строку?
               else gbSize.widthCells > currLvl.getMaxCellWidthCurrLineCol() )
            ) {
              // Здесь нет места для текущего блока.
              val (searchLvl, searchTopPx) =
                // Для wide-блока надо считать место с помощью root-уровня.
                if (gbSize.expandMode.nonEmpty) rootLvl -> currWide.topPx
                // Для обычного блока ищем место на текущем уровне.
                else currLvl -> xy.line

              // Рассчитать следующую строку для перехода. Для wide-блока рассчёт идёт на root-уровне, поэтому требуется обратный маппинг координату назад в проекцию текущего уровня.
              var nextLinePx = searchLvl.ctx.minHeightUsedAfterLine( searchTopPx )
              if (searchLvl.ctx.isRoot && !currLvl.ctx.isRoot)
                nextLinePx = currLvl.ctx.absLineToRel( nextLinePx )
              nextLinePx += paddingMultedPx

              //println(s"step to next line, $xy => $nextLinePx, overlaps?${s0.wides overlaps currWide} wideLine=${currWide.topPx} root.maxWidthCL=${rootLvl.ctx.getMaxCellWidth(currWide.topPx, 0)} root.colsCount=${rootLvl.ctx.colsCount}")
              val currLvlUpdF = MGbLevelState.stepToNextLine( nextLinePx )
              _stepper(
                MGbStepState.replaceCurrLevel( currLvlUpdF(currLvl) )(s0)
              )

            } else {
              // Здесь влезет блок текущей ширины и высоты.
              // Отработать wide-карточку: разместить её в аккамуляторе wide-строк.
              val endColumnIndex = xy.column + gbSize.widthCells
              val pxIvl2 = MPxInterval(
                startPx = topPxAbs,
                sizePx  = blockHeightPx,
                block   = gb,
              )
              // Обновить состояние: проинкрементить col/line курсоры:
              val updateColumnStateF = MColumnState.occupiedRev.modify( pxIvl2 :: _ )
              for ( ci <- xy.column until endColumnIndex )
                currLvl.ctx.updateHeightUsed( ci )(updateColumnStateF)

              val currLvl2 = currLvl.copy(
                restItems   = currLvl.restItems.tail,
                currLineCol = MCoords2di.x.set(endColumnIndex)(xy),
              )
              val xyAbs = currLvl.ctx.colLineToAbs( xy )

              val orderN = gb.orderN
                .getOrElse( stepCounter )

              // Т.к. фон wide-блока центруется независимо от контента, для этого используется искусственный wide-блок,
              // идущий перед wide-блоком с контентом. Надо закинуть wide-фоновый-блок в res-аккамулятор.
              val wideBgWidthOpt = gb.wideBgSz
                .map { wideBgSz =>
                  // Задан размер широкого фона. Берём его как есть без лишних манипуляций.
                  val r = ImgCommonUtil.isUseWidthForBlockBg(
                    blockHeightPx = gb.jdt.props1.heightPx.get,
                    origWh        = wideBgSz,
                    jdt           = gb.jdt,
                    jdConf        = args.jdConf,
                    wideSzMultOpt = wideSzMultOpt
                  )
                  // При горизонтальном выравнивании - берётся wideBgSz.width
                  if (r.isUseWidth) args.jdConf.plainWideBlockWidthPx
                  // При вертикальном выравнивании вычислить ширину на основе коэфф.высоты картинки к блоку:
                  else (r.img2BlockRatioH * wideBgSz.width).toInt
                }
                .orElse[Int] {
                  if (gb.jdt.name ==* MJdTagNames.QD_CONTENT) {
                    val r = gbSize
                      .widthUnRotatedPx
                      .fold (gbSize.widthCells * paddedCellWidthPx) { sideSzPx =>
                        if (sideSzPx.isSzMulted) sideSzPx.sizePx
                        else szMultedF(sideSzPx.sizePx, wideSzMultOpt)
                      }

                    Some(r)

                  } else {
                    // wide-блок без фоновой картинки. Взять ширину такого блока из jdConf:
                    OptionUtil.maybe( gbSize.expandMode.nonEmpty && !args.jdConf.isEdit ) {
                      args.jdConf.plainWideBlockWidthPx
                    }
                  }
                }

              val res = MGbItemRes(
                // Восстановить порядок, если индекс был передан из reDo-ветви.
                orderN        = orderN,
                topLeft       = xyAbs,
                forceCenterX  = wideBgWidthOpt,
                gbBlock       = gb,
                wide          = currWide,
              )
              //println(s"RES: Node#${itemExt.nodeId.orNull} topLeft=" + xyAbs + " orderN=" + orderN + " gb=" + gb.size + (if(gb.size.expandMode.nonEmpty) " WIDE" else ""))

              val mwlAbsOpt = OptionUtil.maybe(gbSize.expandMode.nonEmpty)( currWide )

              // Если wide, то надо извлечь из results-аккамулятора элементы, конфликтующие по высоте с данным wide-блоком и запихать их в reDo-аккамулятор.
              val s1DeConflictedOpt = for {
                // Если у нас wide-блок
                mwlAbs <- mwlAbsOpt

                if s0.resultsAccRev.nonEmpty

                // Найти id карточек, которые конфликтуют с указанным блоком.
                conflictingNodeIds = (
                  for {
                    gbRes0      <- s0.resultsAccRev.iterator
                    gbResNodeId <- gbRes0.gbBlock.jdId.nodeId
                    if !(gbRes0.gbBlock.jdId.nodeId contains gbResNodeId) &&
                       (mwlAbs overlaps gbRes0.wide)
                  } yield
                    gbResNodeId
                )
                  .toSet

                if conflictingNodeIds.nonEmpty

                (conflicting, ok) = s0.resultsAccRev.partition { gbRes0 =>
                  gbRes0.gbBlock.jdId.nodeId
                    // Если блок без id, то надо проверить, не пересекается ли он с текущей wide-карточкой.
                    // Если блок с id, то проверить его id по множеству конфликтующих id.
                    .fold( gbRes0.wide overlaps mwlAbs )( conflictingNodeIds.contains )
                }
                // conflicting.nonEmpty не проверяем, т.к. conflictingNodeIds.nonEmpty выше - эквивалентен по сути

              } yield {
                //println("conflicting = " + conflictingNodeIds)
                // Есть конфликтующие item'ы. Надо закинуть их в reDoItems на родительском уровне.
                val parentLvlOpt = s0.levels.tail.headOption
                val modLvl0 = parentLvlOpt getOrElse currLvl2
                val modLvl2 = MGbLevelState.reDoItems
                  .modify(conflicting reverse_::: _)( modLvl0 )

                // В связи с извлечением некоторых item'ов снизу, надо откатить значения в колонках.
                for {
                  e  <- conflicting
                  updateLens = MColumnState.occupiedRev.modify { mcs0 =>
                    mcs0.filter { pxIvl =>
                      pxIvl.block !===* e.gbBlock
                    }
                  }
                  eGbSize <- e.gbBlock.size
                  lastConflictColumn = eGbSize.expandMode
                    .fold { e.topLeft.left + eGbSize.widthCells } { _ => args.jdConf.gridColumnsCount }

                  ci <- e.topLeft.left until lastConflictColumn
                } {
                  rootLvl.ctx.updateHeightUsed(ci)( updateLens )
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

              val s1 = s1DeConflictedOpt getOrElse {
                // Не-wide блок. Просто закинуть данные в состояние.
                s0.copy(
                  levels        = currLvl2 :: s0.levels.tail,
                  resultsAccRev = res :: s0.resultsAccRev,
                  wides         = mwlAbsOpt :?: s0.wides,
                )
              }

              // 2018-01-24 Решено, что после wide-карточки на focused-уровне надо рендерить дальнейшую карточку не в столбик,
              // а в обычном порядке по ширине всей плитки. Для этого можно объеденить два последних уровня, но лучше
              // подменить контекст на текущем уровне на root, чтобы не нарушать порядок рендера.
              val s2 = if (
                gbSize.expandMode.nonEmpty &&
                !currLvl2.ctx.isRoot &&
                // Нельзя делать сброс, если последующий элемент тоже wide. Это решает только последний из первых wide-элементов.
                !currLvl2.restItems
                  .headOption
                  .exists(
                   _.flatten
                    .headOption
                    .exists(_.size.exists(_.expandMode.nonEmpty))
                  ) &&
                // 2018-01-30 Запретить этот сброс, если остался только один элемент: один болтающийся элемент выглядит не очень.
                (currLvl2.restItems.lengthCompare(1) > 0)
              ) {
                //println( itemExt, currLvl2.restItems.headOption, !currLvl2.restItems.headOption.exists(_.headIsWide) )
                val currLvl3 = s1.levels.head.copy(
                  ctx = rootLvl.ctx,
                  // Надо, чтобы оставшиеся блоки рендерились строго ПОСЛЕ wide-блока.
                  // Если не подправить curr.line, то возможно, что мелкий блок отрендерится в пустоте перед широким блоком.
                  // Поэтому надо подправить currLineCol с учётом высоты вставленного wide-блока.
                  currLineCol = rootLvl.currLineCol.copy(
                    x = 0,
                    y = currWide.bottomPx + paddingMultedPx,
                  ),
                )
                MGbStepState.replaceCurrLevel( currLvl3 )(s1)
              } else {
                s1
              }

              _stepper(s2)
            }

          // ПодСписок блоков, значит это открытая focused-карточка. Позиционируем эти блоки вертикально:
          } else {
            // Создаём виртуальный контекст рекурсивного рендера плитки, и погружаемся в новый уровень рендера.
            // Это нужно, чтобы раскрыть одну карточку вниз, а не слева-направо.
            val nextLvl = MGbLevelState(
              ctx = currLvl.ctx.verticalSubGrid( currLvl.currLineCol ),
              restItems = subItems.to( LazyList ),    // TODO Opt конвертация Stream=>LazyList
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
      override def absLineToRel(clAbs: Int) = clAbs

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

    // Запуск step-цикла. Обновляет переменные в root-контекст, поэтому НЕ ленивый.
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
        val finalCoords = MCoords2di(
          // Эксплуатация костыля по абсолютной центровке какого-то блока вместо расположения в плитке:
          x = res.forceCenterX.fold {
            res.topLeft.x * paddedCellWidthPx
          } { widthOrigPx =>
            // Отцентровать используя указанный сдвиг относительно центра плитки. И никакого szMult тут быть не должно, иначе опять всё пойдёт вразнос.
            (gridWidthPx - widthOrigPx) / 2
          },
          y = res.topLeft.y + args.offY,
        )
        res.gbBlock.jdId -> finalCoords
      }
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
  def absLineToRel(clAbs: Int): Int

  /** Прочитать состояние уже использованной высоты для указанной колонки. */
  def getHeightUsed(ci: Int): Int

  /** Обновить состояние использованной высоты у указанной колонки.
    *
    * @param columnIndexRel Относительный индекс колонки в контексте данной проекции.
    * @param updateF Обновление АБСОЛЮТНЫХ данных по проекции.
    */
  def updateHeightUsed(columnIndexRel: Int)(updateF: MColumnState => MColumnState): Unit

  def isRoot: Boolean

  override def toString = getClass.getSimpleName
}
object IGridBuildCtx {
  implicit class GridBuildCtxApi( val ctx: IGridBuildCtx ) extends AnyVal {

    /** Поиск следующей пустой строки после указанной. */
    def minHeightUsedAfterLine(linePx: Int): Int = {
      val iter = for {
        ci <- (0 until ctx.colsCount).iterator
        h = ctx.getHeightUsed( ci )
        if h > linePx
      } yield h

      if (iter.isEmpty) linePx
      else iter.min
    }

    /** Детектирование текущей максимальной ширины в сетке в текущей строке. */
    def getMaxCellWidth(afterLine: Int, afterColumn: Int = 0): Int = {
      // TODO Следует выкинуть var, занеся её в args, например.
      var mw = 1
      @tailrec def __detect(i: Int): Int = {
        if (i < ctx.colsCount && ctx.getHeightUsed(i) <= afterLine ) {
          mw += 1
          __detect(i + 1)
        } else {
          mw - 1
        }
      }
      __detect( afterColumn )
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
        Math.min( ctx.colsCount - cellWidth, outerLineCol.column )
      )
      val line2 = outerLineCol.line
      //println("vertical subGrid = " + outerLineCol)

      new IGridBuildCtx {
        override def colsCount = cellWidth
        // Использовать subItems.max width?

        override def colLineToAbs(xy: MCoords2di): MCoords2di = {
          xy.copy(
            x = columnToAbs( xy.x ),
            y = lineToAbs( xy.y )
          )
        }

        override def columnToAbs(ci: Int) =
          ci + column2

        override def lineToAbs(cl: Int) =
          cl + line2

        override def absLineToRel(clAbs: Int) =
          clAbs - line2

        override def getHeightUsed(ci: Int) = {
          val hostHeightUsed = ctx.getHeightUsed( columnToAbs(ci) )
          Math.max(0, absLineToRel(hostHeightUsed) )
        }

        override def updateHeightUsed(ci: Int)(updateF: MColumnState => MColumnState): Unit = {
          val hostCi = columnToAbs( ci )
          ctx.updateHeightUsed( hostCi )( updateF )
        }

        override def toString: String = {
          ctx.toString +
            HtmlConstants.PLUS + line2 +
            HtmlConstants.PLUS + column2
        }

        override final def isRoot = false

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
  * @param wides Акк накопления инфы о широких блоках.
  */
case class MGbStepState(
                         levels             : List[MGbLevelState],
                         resultsAccRev      : List[MGbItemRes]        = Nil,
                         wides              : List[MWideLine]         = Nil,
                       )
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

  def stepToCellLens = currLineCol
    .composeLens(MCoords2di.x)

  /**
    * step() не может подобрать подходящий блок в текущей строке и хочет просто шагнуть в следующую ячейку,
    * оставив пустоту за собой.
    * Этот метод вносит одношаговые изменения в состояние.
    */
  val stepToNextCell = stepToCellLens.modify(_ + 1)

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
                          restItems   : LazyList[Tree[MGbBlock]],
                          currLineCol : MCoords2di              = MCoords2di(0, 0),
                          reDoItems   : List[MGbItemRes]        = Nil,
                        ) {

  /** Детектирование текущей максимальной ширины в сетке в текущей строке. */
  def getMaxCellWidthCurrLineCol(): Int = {
    ctx.getMaxCellWidth(
      afterLine   = currLineCol.line,
      afterColumn = currLineCol.column
    )
  }

}


/** Результат позиционирования одного блока.
  *
  * @param orderN Порядковый номер блока.
  * @param topLeft Отпозиционированные координаты.
  * @param forceCenterX Костыль для принудительной центровки по X вместо координат по сетке.
  */
case class MGbItemRes(
                       orderN           : Int,
                       topLeft          : MCoords2di,
                       forceCenterX     : Option[Int]   = None,
                       gbBlock          : MGbBlock,
                       wide             : MWideLine,
                     )
