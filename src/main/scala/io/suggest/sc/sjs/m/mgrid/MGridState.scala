package io.suggest.sc.sjs.m.mgrid

import io.suggest.adv.ext.model.im.ISize2di
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

  /** Инфа по колонкам. Нужен O(1) доступ по индексу. Длина равна или не более кол-ва колонок. */
  def colsInfo          : Array[MColumnState]

  def withNewBlocks(newBlocks: TraversableOnce[IBlockInfo]): IGridState
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
  // TODO mutable-состояние унаследовано от предыдущей архитектуры. Надо бы исправить этот дефект.

  /** Максимальная ширина одной ячейки. */
  var maxCellWidth      : Int     = TileConstants.CELL_WIDTH_140_CSSPX,

  /** Левый сдвиг в кол-ве ячеек. */
  var leftOffset        : Int     = 0,

  /** Правый сдвиг в кол-ве ячеек. */
  var rightOffset       : Int     = 0,

  /** Кол-во колонок на экране. */
  var columnsCount      : Int     = 2,

  /** true, когда больше карточек у сервера нет для текущей выдачи. */
  var fullyLoaded       : Boolean = false,

  /** Кол-во карточек для следующей пакетной загрузки. */
  var adsPerLoad        : Int     = 30,

  /** Кол-во загруженных карточек. */
  var blocksLoaded      : Int     = 0,

  /** Запрошена подгрузка ещё карточек? */
  var isLoadingMore     : Boolean = false,

  /** Размер контейнера, если рассчитан. */
  var contSz            : Option[ICwCm] = None,

  /** Инфа по колонкам. Нужен O(1) доступ по индексу. Длина равна или не более кол-ва колонок. */
  var colsInfo          : Array[MColumnState] = Array.empty,

  /** Инфа по текущим блокам. */
  // TODO После спиливания архитектуры v1 можно/нужно заменить IBlockInfo на GBlock. Внутри всё равно теперь GBlock.
  var blocks            : ListBuffer[IBlockInfo] = ListBuffer.empty
) extends IGridState {

  /** Контроллер требует закинуть новые блоки в эту модель состояния, указывая точное кол-во блоков.
    * @param newBlocks Последовательность новых блоков.
    * @param newBlocksCount Длина коллекции newBlocks.
    */
  @deprecated("FSM-MVM: Use for stateful FSM use immutable withNewBlocks() instead.")
  def appendNewBlocksMut(newBlocks: TraversableOnce[IBlockInfo], newBlocksCount: Int): Unit = {
    blocks.appendAll(newBlocks)
    blocksLoaded += newBlocksCount
  }
  override def withNewBlocks(newBlocks: TraversableOnce[IBlockInfo]): MGridState = {
    // TODO mutable-коллекция здесь
    blocks.appendAll(newBlocks)
    copy(
      blocksLoaded = blocks.size    // O(1)
    )
  }

  /** Контроллер приказывает сбросить состояние плитки, касающееся загруженных и отображаемых карточек. */
  @deprecated("FSM-MVM: Use immutable nothingLoaded() instead.")
  def nothingLoadedMut(): MGridState = {
    blocks = ListBuffer.empty
    blocksLoaded = 0
    isLoadingMore = false
    fullyLoaded = false
    this
  }
  override def nothingLoaded(): MGridState = {
    copy(
      blocks        = ListBuffer.empty,
      blocksLoaded  = 0,
      isLoadingMore = false,
      fullyLoaded   = false
    )
  }

  /** Загрузить кое-какие изменения в состояния. */
  @deprecated("FSM-MVM: Use for stateful FSM use immutable withContParams() instead.")
  def updateWithMut(cw: IColsWidth with ICwCm): Unit = {
    maxCellWidth = cw.maxCellWidth
    columnsCount = cw.columnsCnt
    contSz       = Some(cw)
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

