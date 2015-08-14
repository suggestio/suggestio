package io.suggest.sc.sjs.m.mgrid

import io.suggest.adv.ext.model.im.ISize2di
import io.suggest.sc.sjs.vm.grid.GBlock
import io.suggest.sc.tile.TileConstants

import scala.collection.mutable.ListBuffer

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 14:13
 * Description: Переменные состояния сетки выдачи.
 */
trait IGridState {

  /** Максимальная ширина одной ячейки. */
  def maxCellWidth      : Int

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
  def isDesktopView = columnsCount > 2

  /** При рассчете left/right offset'ов калькулятором учитывается мнение выдачи. */
  def canNonZeroOffset: Boolean = {
    // TODO Нужно понять толком, какой смысл несет выражение в скобках...
    //cbca_grid.columns > 2 || ( cbca_grid.left_offset != 0 || cbca_grid.right_offset != 0 )
    isDesktopView || leftOffset != 0 || rightOffset != 0
  }

  def withContParams(cw: IColsWidth with ICwCm): IGridState

}


/** Дефолтовая реализация модели [[IGridState]]. */
case class MGridState(
  /** Максимальная ширина одной ячейки. */
  maxCellWidth      : Int     = TileConstants.CELL_WIDTH_140_CSSPX,

  /** Левый сдвиг в кол-ве ячеек. */
  leftOffset        : Int     = 0,

  /** Правый сдвиг в кол-ве ячеек. */
  rightOffset       : Int     = 0,

  /** Кол-во колонок на экране. */
  columnsCount      : Int     = 2,

  /** true, когда больше карточек у сервера нет для текущей выдачи. */
  fullyLoaded       : Boolean = false,

  /** Кол-во карточек для следующей пакетной загрузки. */
  adsPerLoad        : Int     = 30,

  /** Кол-во загруженных карточек. */
  blocksLoaded      : Int     = 0,

  /** Запрошена подгрузка ещё карточек? */
  isLoadingMore     : Boolean = false,

  /** Размер контейнера, если рассчитан. */
  contSz            : Option[ICwCm] = None

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
      maxCellWidth = cw.maxCellWidth,
      columnsCount = cw.columnsCnt,
      contSz       = Some(cw)
    )
  }


}


object MGridState {

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

