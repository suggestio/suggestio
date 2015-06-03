package io.suggest.sc.sjs.util.grid.builder

import io.suggest.sc.sjs.m.mgrid._
import io.suggest.sjs.common.util.ISjsLogger

import scala.annotation.tailrec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.06.15 13:52
 * Description: Билдер сетки из первого поколения системы.
 * Алгоритм напоминает оффлайновый Burke либо online fit (OF, online Burke).  TODO: уточнить.
 * Код билдера основан на cbca_grid.build() и её helper'ах.
 *
 * @see [[http://prolll.com/pr/backpack/ Demo некоторых алгоритмов 2D-упаковки на js]]
 */
trait V1Builder extends ISjsLogger {

  /** true - Элементы добавляются в существующую сетку, false -- новая сетка с нуля. */
  def _isAdd: Boolean

  /** Доступ к состоянию выдачи. */
  def _mgs: MGridState //= MGrid.state

  /** Исходный список обрабатываемых блоков.  */
  def _addedBlocks: List[MBlockInfo]

  val _gparams: MGridParams = MGrid.params


  // TODO Вынести этот велосипед внутри функции в отдельный класс, собирающий сетку.
  // Setting left & top
  def leftPtrBase = 0
  private var leftPtr = leftPtrBase
  //var topPtr = 0

  // Аккамулятор блоков для обработки. По мере обработки блоков, использованные блоки выбрасываются из него.
  private var blocksAcc: List[MBlockInfo] = {
    if (!_isAdd && _addedBlocks.isEmpty) {
      _mgs.blocks.toList
    } else {
      _addedBlocks
    }
  }

  // Определяем ширину окна
  //val wndWidth = MAgent.availableScreen.width

  // Ставим указатели строки и колонки
  private var cLine = 0
  private var currColumn = 0

  /** Карта высот колонок сетки. */
  val colsInfo: Array[MColumnState] = {
    val stateColsInfo = _mgs.colsInfo
    if (_isAdd && stateColsInfo.length > 0) {
      // Добавление блоков в уже рассчитанную сетку.
      stateColsInfo
    } else {
      // Новая сетка.
      _mgs.newColsInfo()
    }
  }

  /** Кол-во колонок. */
  val colsCount = _mgs.columnsCount

  /** Детектирование текущей максимальной ширины в сетке в текущей строке. */
  def _getMaxBlockWidth(): Int = {
    var mw = 1
    @tailrec def __detect(i: Int): Int = {
      if (i < colsCount && colsInfo(i).heightUsed == cLine ) {
        mw += 1
        __detect(i + 1)
      } else {
        mw - 1
      }
    }
    __detect(currColumn)
  }

  // Кешируем тут разные динамические константы перед запуском цикла.
  private val cpadding = _gparams.cellPadding
  val paddedCellSize = _gparams.paddedCellSize

  // Посчитать размер в ячейках на основе пиксельного размера.
  protected def getWCellSize(sz: Int) = (sz + cpadding) / paddedCellSize

  /** Рекурсивная функция тела _extractBlock(), которое обходит список в поисках результата. */
  protected final def _extractBlock2(bMaxW: Int, rest: List[MBlockInfo]): Option[(MBlockInfo, List[MBlockInfo])] = {
    if (bMaxW <= 0 || rest.isEmpty) {
      None
    } else {
      val b = rest.head
      val tl = rest.tail
      val wCellWidth = getWCellSize(b.width)
      if (wCellWidth <= bMaxW) {
        Some((b, tl))
      } else {
        _extractBlock2(bMaxW, tl).map {
          case (bFound, tl2) =>
            (bFound, b :: tl2)
        }
      }
    }
  }

  /** Найти и извлечь блок подходящей ширины, обновив аккамулятор блоков. */
  protected def _extractBlock(bMaxW: Int, rest: List[MBlockInfo] = blocksAcc): Option[MBlockInfo] = {
    _extractBlock2(bMaxW, rest) map {
      case (b, rest2) =>
        blocksAcc = rest2
        b
    }
  }

  /**
   * Билдер вычислил координаты очередного блока и хочет реакции от вышестоящей системы
   * по перемещению блока в нужные координаты.
   * @param leftPx Координата X.
   * @param topPx Координата Y.
   * @param b Блок.
   */
  protected def moveBlock(leftPx: Int, topPx: Int, b: MBlockInfo): Unit

  /**
   * step() не может подобрать подходящий блок в текущей строке и хочет просто шагнуть в следующую ячейку,
   * оставив пустоту за собой.
   * Этот метод вносит одношаговые изменения в состояние.
   */
  protected def beforeStepToNextCell(): Unit = {
    currColumn += 1
    leftPtr += paddedCellSize
  }

  /** step() переходит на следующую строку. Нужно внести изменения в состояние. */
  protected def beforeStepNextLine(): Unit = {
    currColumn = 0
    cLine += 1
    leftPtr = leftPtrBase
  }

  // В оригинале был for-цикл с ограничением на 1000 итераций.
  @tailrec final def step(i: Int): Unit = {
    if (i >= 1000) {
      // return -- слишком много итераций.
      warn("Too many iterations: " + i)
    } else if (currColumn >= colsCount) {
      // Конец текущей строки -- перейти на следующую строку.
      beforeStepNextLine()
      step(i + 1)

      // В оригинале была ещё ветка: if this.is_only_spacers() == true ; break
    } else if ( colsInfo(currColumn).heightUsed == cLine ) {
      // Высота текущей колонки равна cLine.
      // есть место хотя бы для одного блока с минимальной шириной, выясним блок с какой шириной может влезть.
      val bMaxW = _getMaxBlockWidth()
      val bOpt = _extractBlock(bMaxW)
      if (bOpt.nonEmpty) {
        val b = bOpt.get

        val wCellWidth  = getWCellSize(b.width)
        val wCellHeight = getWCellSize(b.height)
        ( currColumn until (currColumn + wCellWidth) )
          .iterator
          .filter { _ < colsCount }
          .foreach { ci =>
            colsInfo(currColumn).heightUsed += wCellHeight
            currColumn += 1
          }

        moveBlock(
          leftPx  = leftPtr,
          topPx   = cLine * paddedCellSize + _gparams.topOffset,
          b       = b
        )

        leftPtr += b.width + cpadding
        step(i + 1)

      } else if (blocksAcc.nonEmpty) {
        // Требуется переход в след.ячейку, оставив пустоту в этой ячейке.
        colsInfo(currColumn).heightUsed += 1
        beforeStepToNextCell()
        step(i + 1)
      }
      // Если нет следующего блока - обход закончен.

    } else {
      // Требуется переход на следующую ячейку.
      beforeStepToNextCell()
      step(i + 1)
    }
  }

  /** Запуск цикла перестроения сетки */
  def execute(): Unit = {
    step(0)
  }

  override def toString: String = {
    "v1b(" + _isAdd + ",[" + blocksAcc.length + "])"
  }

}
