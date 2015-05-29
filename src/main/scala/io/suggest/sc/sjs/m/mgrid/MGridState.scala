package io.suggest.sc.sjs.m.mgrid

import io.suggest.sc.sjs.m.magent.MAgent
import io.suggest.sc.tile.TileConstants

import scala.collection.mutable.ListBuffer

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 14:13
 * Description: Переменные состояния сетки выдачи.
 */
class MGridState {

  /** Максимальная ширина одной ячейки. */
  var maxCellWidth      : Int     = TileConstants.CELL_WIDTH_140_CSSPX

  /** Левый сдвиг в кол-ве ячеек. */
  var leftOffset        : Int     = 0

  /** Правый сдвиг в кол-ве ячеек. */
  var rightOffset       : Int     = 0

  /** Кол-во колонок на экране. */
  var columnsCount      : Int     = 2

  /** true, когда больше карточек у сервера нет для текущей выдачи. */
  var fullyLoaded       : Boolean = false

  /** Кол-во карточек для следующей пакетной загрузки. */
  var adsPerLoad        : Int     = 30

  /** Кол-во загруженных карточек. */
  var adsLoaded         : Int     = 0

  /** Запрошена подгрузка ещё карточек? */
  var isLoadingMore     : Boolean = false

  /** Размер контейнера, если рассчитан. */
  var contSz            : Option[ICwCm] = None

  /** Инфа по колонкам. Нужен O(1) доступ по индексу. Длина равна или не более кол-ва колонок. */
  var colsInfo          : Array[MColumnState] = Array.empty

  /** Инфа по текущим блокам. */
  var blocks            : ListBuffer[MBlockInfo] = ListBuffer.empty

  /** При рассчете left/right offset'ов калькулятором учитывается мнение выдачи. */
  def canNonZeroOffset: Boolean = {
    // TODO Нужно понять толком, какой смысл несет выражение в скобках...
    //cbca_grid.columns > 2 || ( cbca_grid.left_offset != 0 || cbca_grid.right_offset != 0 )
    columnsCount > 2 || leftOffset != 0 || rightOffset != 0
  }

  /** Загрузить кое-какие изменения в состояния. */
  def updateWith(cw: IColsWidth with ICwCm): Unit = {
    maxCellWidth = cw.maxCellWidth
    columnsCount   = cw.columnsCnt
    contSz       = Some(cw)
  }

  /**
   * Сгенерить новое состояние колонок без сайд-эффектов.
   * @return
   */
  def newColsInfo(): Array[MColumnState] = {
    Array.fill(columnsCount)( MColumnState() )
  }

}


object MGridState {

  /** Предложить кол-во загружаемых за раз карточек с сервера. */
  def getAdsPerLoad(ww: Int = MAgent.availableScreen.width): Int = {
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

