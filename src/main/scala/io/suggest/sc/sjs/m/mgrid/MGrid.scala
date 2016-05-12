package io.suggest.sc.sjs.m.mgrid

import io.suggest.common.geom.d2.ISize2di

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.05.15 17:45
 * Description: Модель состояния cbca_grid.
 */

/** Интерфейс для контейнера данных сетки. */
trait IGridData extends MGridUtil {

  /** Параметры сетки, выставляются всей пачкой. Изменяются редко. */
  def params: MGridParams

  /** Текущее состояние сетки. Состоит из переменных и обновляется контроллерами. */
  def state: MGridState

  /** Опциональное состояние билдера, если есть. */
  def builderStateOpt: Option[MGridBuilderState]

  /** Извлечь состояние билдера или вернуть новое, если готовое состояние отсутствует. */
  def builderState: MGridBuilderState = {
    builderStateOpt.getOrElse {
      MGridBuilderState( state.newColsInfo() )
    }
  }

  override protected def grid = this
}


/** Дефолтовая реализация контейнера состояний компонентов плитки. */
case class MGridData(
  override val params           : MGridParams                 = MGridParams(),
  override val state            : MGridState                  = MGridState(),
  override val builderStateOpt  : Option[MGridBuilderState]   = None
)
  extends IGridData


/** Утиль для анализа данных в grid-моделях. */
trait MGridUtil {

  protected def grid: IGridData

  /**
    * Рассчет новых параметров контейнера.
    * Это pure-function, она не меняет состояние системы, а только считает.
    *
    * @param screen Данные по текущему экрану.
    * @return Экземпляр с результатами рассчетов.
    */
  def getGridContainerSz(screen: ISize2di): MGetContStateResult = {
    val colCount = grid.params.countColumns(screen)

    var cw = grid.params.margin(colCount)
    var cm = 0
    // Есть несколько вариантов: экран узковат по ширине; экран большой.
    // Если экран узковат, то нельзя вычитать из него боковые панели.

    val colCount2 = if (MGridState.isDesktopView(colCount)) {
      // Колонок много, делим область экрана между плиткой и боковыми панелями.
      var margin = 0

      val leftOff = grid.state.leftOffset
      if (leftOff > 0) {
        margin = grid.params.margin( leftOff )
        cw -= margin
        cm = margin
      }

      val rightOff = grid.state.rightOffset
      if (rightOff > 0) {
        margin = grid.params.margin( rightOff )
        cw -= margin
        cm = -margin
      }

      colCount - leftOff - rightOff

    } else {
      // Колонок на экране острый дефицит. Grid будет отображена по всей ширине независимо от панели.
      MGridState.MIN_CELL_COLUMNS
    }

    // Собрать и вернуть результат рассчетов.
    MGetContStateResult(
      cw = cw,
      cm = cm,
      columnsCnt = colCount2
    )
  }

}
