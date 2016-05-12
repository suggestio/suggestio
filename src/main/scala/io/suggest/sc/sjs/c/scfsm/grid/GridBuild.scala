package io.suggest.sc.sjs.c.scfsm.grid

import io.suggest.sc.sjs.m.magent.IMScreen
import io.suggest.sc.sjs.m.mgrid.{IGridData, MGridData}
import io.suggest.sc.sjs.m.msc.fsm.IStData
import io.suggest.sc.sjs.util.grid.builder.V1Builder
import io.suggest.sc.sjs.vm.grid.{GBlock, GContainer, GContent}
import io.suggest.sc.sjs.vm.util.GridOffsetCalc
import io.suggest.sjs.common.model.browser.IBrowser
import io.suggest.sjs.common.util.SjsLogger

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.08.15 10:53
 * Description: Бывают состояния ScFsm, которым нужна поддержка перестоения сетки.
 * Тут утиль для сборки таких состояний. К самому FSM тут привязки нет.
 */
trait GridBuild {

  /** Использовать ли анимацию для перемещения блоков? Обычно да, но не всегда. */
  protected def _gridAppendAnimated: Boolean = true

  /** Частичная реализация grid builder под нужды FSM-MVM-архитектуры. */
  protected trait GridBuilderT extends V1Builder with SjsLogger {
    override type BI = GBlock
    def browser: IBrowser

    // Собираем функцию перемещения блока. При отключенной анимации не будет лишней сборки ненужного
    // списка css-префиксов и проверки значения withAnim.
    val _moveBlockF: (Int, Int, BI) => Unit = {
      if (_gridAppendAnimated) {
        // Включена анимация. Собрать необходимые css-префиксы. {} нужно для защиты от склеивания с последующей строкой.
        val animCssPrefixes = { browser.Prefixing.transforms3d }
        {(leftPx: Int, topPx: Int, b: BI) =>
          b.moveBlockAnimated(leftPx, topPx, animCssPrefixes)
        }

      } else {
        // Анимация отключена.
        {(leftPx: Int, topPx: Int, b: BI) =>
          b.moveBlock(leftPx, topPx)
        }
      }
    }

    override protected def moveBlock(leftPx: Int, topPx: Int, b: BI): Unit = {
      _moveBlockF(leftPx, topPx, b)
    }
  }


  /** Дефолтовая реализация билдера сетки [[GridBuilderT]]. */
  protected case class GridBuilder(override val grid: IGridData,
                                   override val browser: IBrowser,
                                   override val _addedBlocks: List[GBlock])
    extends GridBuilderT


  /** Реализация grid builder'а для билда сетки через State Data. */
  protected case class GridBuilderSd(sd: IStData,
                                     override val _addedBlocks: List[GBlock] = Nil) extends GridBuilderT {
    override def browser = sd.browser
    override def grid = sd.grid
  }


  /** Сборка ребилдера плитки карточек.
    * В [[GridBuilder]] передаётся почищенное состояние и все имеющиеся блоки. */
  protected object GridRebuilder {
    def apply(grid0: MGridData, browser: IBrowser): GridBuilder = {
      val grid = grid0.copy(
        state = grid0.state.copy(
          blocksLoaded = 0
        ),
        builderStateOpt = None
      )
      val _addedBlocks = GContainer.find()
        .iterator
        .flatMap(_.fragmentsIterator)
        .flatMap(_.blocksIterator)
        .toList
      GridBuilder(grid, browser, _addedBlocks)
    }
  }


}


/** Поддержка ребилда сетки при изменении состояния боковой панели. */
trait PanelGridRebuilder extends GridBuild {

  // TODO Это эталонный говнокод, нужно сделать нормальный пересчет сетки.
  // Этот код по сути статический, может его просто вынести куда-нить?

  /** Пересчет основных данных плитки под экран. */
  protected def _refreshGridData(sd0: IStData, screen: IMScreen, calc: GridOffsetCalc): MGridData = {
    // Внести поправки в состояние плитки.
    val mgs2 = calc.GridOffsetter(sd0).execute()
    val gData2 = sd0.grid.copy(
      state = mgs2
    )
    val csz = gData2.getGridContainerSz(screen)
    val mgs3 = mgs2.withContParams(csz)
    sd0.grid.copy(
      state = mgs3
    )
  }

  /** Исполнить ребилдер сетки и обновить состояние сетки, вернув его. */
  protected def _doRebuildGrid(mgd3: MGridData, forBrowser: IBrowser): MGridData = {
    // Обновить размер контейнера.
    for {
      csz      <- mgd3.state.contSz
      gcontent <- GContent.find()
    } {
      gcontent.setContainerSz( csz )
    }
    // Отребилдить сетку
    val gBuilder = GridRebuilder(mgd3, forBrowser)
    val gbState2 = gBuilder.execute()
    // Собрать новые данные состояния FSM
    mgd3.copy(
      builderStateOpt = Some(gbState2)
    )
  }

  /** Акт ребилда плитки карточек, когда изменилось состояние какой-то панели.
    *
    * @param isOpen Сейчас происходит (true) открытие, или (false) сокрытие панели.
    */
  protected def _rebuildGridOnPanelChange(sd0: IStData, screen: IMScreen, calc: GridOffsetCalc, isOpen: Boolean): MGridData = {
    // Методом проб и ошибок выяснено, что при раскрытии панели не надо обновлять состояние.
    // А при сокрытии панели сначала надо сетку пересчитать, а только потом принимать решения.
    // Этот код надо перепилить, просто потому что непонятно почему он работает именно так.
    // Косяки скорее всего появились после появления поддержки resize, запиленой спустя больше полугода после основного кода sc (2016.apr.27-28).

    val mgd1 = if (isOpen)
      sd0.grid
    else
      _refreshGridData(sd0, screen, calc)

    if (mgd1.state.isDesktopView) {
      val mgd2 = if (isOpen)
        _refreshGridData(sd0, screen, calc)
      else
        mgd1

      _doRebuildGrid(mgd2, sd0.browser)

    } else {
      // Сетку не надо ребилдить на узких экранах.
      mgd1
    }
  }

}

