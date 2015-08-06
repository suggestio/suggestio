package io.suggest.sc.sjs.c.scfsm

import io.suggest.sc.sjs.m.magent.IMScreen
import io.suggest.sc.sjs.m.mgrid.{MGridData, IGridData}
import io.suggest.sc.sjs.m.msc.fsm.{MStData, IStData}
import io.suggest.sc.sjs.util.grid.builder.V1Builder
import io.suggest.sc.sjs.vm.grid.{GContent, GBlock}
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
  protected def withAnim: Boolean = true

  /** Частичная реализация grid builder под нужды FSM-MVM-архитектуры. */
  protected trait GridBuilderT extends V1Builder with SjsLogger {
    override type BI = GBlock
    def browser: IBrowser

    // Собираем функцию перемещения блока. При отключенной анимации не будет лишней сборки ненужного
    // списка css-префиксов и проверки значения withAnim.
    val _moveBlockF: (Int, Int, BI) => Unit = {
      if (withAnim) {
        // Включена анимация. Собрать необходимые css-префиксы. {} нужно для защиты от склеивания с последующей строкой.
        val animCssPrefixes = { browser.CssPrefixing.transforms3d }
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
          colsInfo = grid0.state.newColsInfo(),
          blocksLoaded = 0
        ),
        builderStateOpt = None
      )
      val _addedBlocks = grid0.state.getBlocks
      GridBuilder(grid, browser, _addedBlocks)
    }
  }


}


/** Поддержка ребилда сетки при изменении состояния боковой панели. */
trait PanelGridRebuilder extends GridBuild {

  /** Акт ребилда плитки карточек, когда изменилось состояние какой-то панели. */
  protected def _rebuildGridOnPanelChange(sd0: MStData, screen: IMScreen, calc: GridOffsetCalc): MStData = {
    if (sd0.grid.state.isDesktopView) {
      // Внести поправки в состояние плитки.
      val mgs2 = calc.GridOffsetter(sd0).execute()
      val gData2 = sd0.grid.copy(
        state = mgs2
      )
      val csz = gData2.getGridContainerSz(screen)
      for (gcontent <- GContent.find()) {
        gcontent.setContainerSz(csz)
      }
      val mgs3 = mgs2.withContParams(csz)
      val gData3 = sd0.grid.copy(
        state = mgs3
      )
      // Отребилдить сетку
      val gBuilder = GridRebuilder(gData3, sd0.browser)
      val gbState2 = gBuilder.execute()
      // Собрать новые данные состояния FSM
      sd0.copy(
        search = sd0.search.copy(
          opened = true
        ),
        grid = gData3.copy(
          builderStateOpt = Some(gbState2)
        )
      )

    } else {
      // Сетку не надо ребилдить на узких экранах.
      sd0
    }
  }

}

