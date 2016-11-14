package io.suggest.sc.sjs.c.scfsm.grid.build

import io.suggest.sc.sjs.m.mgrid.{IGridData, MGridData}
import io.suggest.sc.sjs.m.msc.{IScSd, MScSd}
import io.suggest.sc.sjs.util.grid.builder.V1Builder
import io.suggest.sc.sjs.vm.grid.{GBlock, GContainer, GContent}
import io.suggest.sc.sjs.vm.util.GridOffsetCalc
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.model.browser.IBrowser

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.08.15 10:53
 * Description: Бывают состояния ScFsm, которым нужна поддержка перестоения сетки.
 * Тут утиль для сборки таких состояний. К самому FSM тут привязки нет.
 */

/** Частичная реализация grid builder под нужды FSM-MVM-архитектуры. */
trait GridBuilderT extends V1Builder with Log {

  override type BI = GBlock

  def browser: IBrowser

  /** Использовать ли анимацию для перемещения блоков? Обычно да, но не всегда. */
  protected def _gridAppendAnimated: Boolean = true

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
case class GridBuilder(override val grid: IGridData,
                       override val browser: IBrowser,
                       override val _addedBlocks: List[GBlock])
  extends GridBuilderT


/** Реализация grid builder'а для билда сетки через State Data. */
case class GridBuilderSd(sd: IScSd,
                         override val _addedBlocks: List[GBlock] = Nil) extends GridBuilderT {
  override def browser = sd.common.browser
  override def grid = sd.grid
}


/** Сборка ребилдера плитки карточек.
  * В [[GridBuilder]] передаётся почищенное состояние и все имеющиеся блоки. */
object GridRebuilder {
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



// Методом проб и ошибок выяснено, что при раскрытии панели не надо обновлять состояние.
// А при сокрытии панели сначала надо сетку пересчитать, а только потом принимать решения.
// Этот код надо перепилить, просто потому что непонятно почему он работает именно так.
// Косяки скорее всего появились после появления поддержки resize, запиленой спустя больше полугода после основного кода sc (2016.apr.27-28).

/** В целях нормализации говнокода тут API для  */
trait RebuildAfterPanelChangeT {

  def sd0: MScSd
  def calc: GridOffsetCalc

  /** Пересчет основных данных плитки под экран. */
  def _refreshGridData(): MGridData = {
    // Внести поправки в состояние плитки.
    val mgs2 = calc.GridOffsetter(sd0).execute()
    val gData2 = sd0.grid.copy(
      state = mgs2
    )
    val csz = gData2.getGridContainerSz( sd0.common.screen )
    sd0.grid.copy(
      state = mgs2.withContParams(csz)
    )
  }

  /** Исполнить ребилдер сетки и обновить состояние сетки, вернув его. */
  def _doRebuildGrid(mgd3: MGridData): MGridData = {
    // Обновить размер контейнера.
    for {
      csz      <- mgd3.state.contSz
      gcontent <- GContent.find()
    } {
      gcontent.setContainerSz( csz )
    }

    // Отребилдить сетку
    val gBuilder = GridRebuilder(mgd3, sd0.common.browser)
    val gbState2 = gBuilder.execute()

    // Собрать новые данные состояния FSM
    mgd3.copy(
      builderStateOpt = Some(gbState2)
    )
  }

  /** Вернуть исходный инстанс MGridData. */
  def _mgd0(mgdR: MGridData): MGridData

  /** Запуск действа на исполнение. */
  def execute(): MGridData = {
    val mgdR = _refreshGridData()
    if ( _mgd0(mgdR).state.isDesktopView ) {
      _doRebuildGrid( mgdR )
    } else {
      mgdR
    }
  }
}

/** Логика действий про открытии панели. */
case class RebuildGridOnPanelOpen(sd0: MScSd, calc: GridOffsetCalc) extends RebuildAfterPanelChangeT {
  override def _mgd0(mgdR: MGridData): MGridData = sd0.grid
}

/** Логика действий при закрытии панели. */
case class RebuildGridOnPanelClose(sd0: MScSd, calc: GridOffsetCalc) extends RebuildAfterPanelChangeT {
  override def _mgd0(mgdR: MGridData): MGridData = mgdR
}
