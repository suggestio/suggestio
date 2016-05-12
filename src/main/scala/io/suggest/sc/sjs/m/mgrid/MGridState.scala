package io.suggest.sc.sjs.m.mgrid

import io.suggest.common.geom.d2.ISize2di
import io.suggest.sc.sjs.vm.grid.GBlock

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 14:13
 * Description: Переменные состояния сетки выдачи.
 */
object MGridState {

  def MIN_CELL_COLUMNS = 2

  def isDesktopView(columnsCount: Int): Boolean = {
    columnsCount > MIN_CELL_COLUMNS
  }

  /** Предложить кол-во загружаемых за раз карточек с сервера. */
  def getAdsPerLoad(screen: ISize2di): Int = {
    val ww = screen.width
    if (ww <= 660)
      5
    else if (ww <= 800)
      10
    else if (ww <= 980)
      20
    else
      30
  }

}


trait IGridState {

  /** Левый сдвиг в кол-ве ячеек. */
  def leftOffset        : Int

  /** Правый сдвиг в кол-ве ячеек. */
  def rightOffset       : Int

  /** Кол-во колонок на экране. */
  def columnsCount      : Int

  /** true, когда больше карточек у сервера нет для текущей выдачи. */
  def fullyLoaded       : Boolean

  /** Кол-во карточек для следующей пакетной загрузки. */
  def adsPerLoad        : Int

  /** Кол-во загруженных карточек. */
  def blocksLoaded      : Int

  /** Запрошена подгрузка ещё карточек? */
  def isLoadingMore     : Boolean

  /** Размер контейнера, если рассчитан. */
  def contSz            : Option[ICwCm]

  def withNewBlocks(newBlocks: TraversableOnce[GBlock]): IGridState
  def nothingLoaded(): IGridState

  /**
   * Сгенерить новое состояние колонок без сайд-эффектов.
   * @return
   */
  def newColsInfo(): Array[MColumnState] = {
    Array.fill(columnsCount)( MColumnState() )
  }

  /**
   * Когда колонок мало, то значит экран узкий, и надо отображать панели поверх выдачи, не двигая выдачу.
   * @return false, если выдача узкая под мобильник.
   *         true, если при раскрытии боковой панели для выдачи ещё останется место.
   */
  def isDesktopView: Boolean = {
    MGridState.isDesktopView(columnsCount)
  }

  def withContParams(cw: IColsWidth with ICwCm): IGridState

}


/** Дефолтовая реализация модели [[IGridState]]. */
case class MGridState(
  /** Левый сдвиг в кол-ве ячеек. */
  override val leftOffset        : Int     = 0,

  /** Правый сдвиг в кол-ве ячеек. */
  override val rightOffset       : Int     = 0,

  /** Кол-во колонок на экране. */
  override val columnsCount      : Int     = 2,

  /** true, когда больше карточек у сервера нет для текущей выдачи. */
  override val fullyLoaded       : Boolean = false,

  /** Кол-во карточек для следующей пакетной загрузки. */
  override val adsPerLoad        : Int     = 30,

  /** Кол-во загруженных карточек. */
  override val blocksLoaded      : Int     = 0,

  /** Запрошена подгрузка ещё карточек? */
  override val isLoadingMore     : Boolean = false,

  /** Размер контейнера, если рассчитан. */
  override val contSz            : Option[ICwCm] = None

) extends IGridState {

  override def withNewBlocks(newBlocks: TraversableOnce[GBlock]): MGridState = {
    copy(
      blocksLoaded = blocksLoaded + newBlocks.size
    )
  }

  override def nothingLoaded(): MGridState = {
    copy(
      blocksLoaded  = 0,
      isLoadingMore = false,
      fullyLoaded   = false
    )
  }

  /** Загрузить кое-какие изменения в состояния. */
  override def withContParams(cw: IColsWidth with ICwCm): MGridState = {
    copy(
      columnsCount = cw.columnsCnt,
      contSz       = Some(cw)
    )
  }


}
