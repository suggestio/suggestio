package io.suggest.sc.sjs.m.mgrid

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.06.15 14:23
 * Description: Модель аккамулятора данных grid builder'а. Изначально она была вплетена в builder.
 * Во время работы builder'а, у него создаётся свой внутренний mutable-контекст на основе этих данных.
 * А затем экспортируется в этом формате.
 */
object MGridBuilderState {

  /** Исходное значение для поля leftPtr. Используется и при сбросе значения поля. */
  def leftPtrDflt = 0

}


trait IGridBuilderState {

  /** Инфа по колонкам. Нужен O(1) доступ по индексу. Длина равна или не более кол-ва колонок. */
  // Массив изменяемый, но стараемся работать с ним безопасно.
  def colsInfo          : Array[MColumnState]

  def leftPtr           : Int
  def cLine             : Int
  def currColumn        : Int
  def isAdd             : Boolean

  /** Заворачивание данных состояния в неизменяемый контейнер состояния. */
  def exportState: MGridBuilderState = {
    MGridBuilderState(
      colsInfo    = colsInfo,
      leftPtr     = leftPtr,
      cLine       = cLine,
      currColumn  = currColumn,
      isAdd       = isAdd
    )
  }

  /** Вычислить максимальную высоту колонки в карте колонок. */
  def maxColHeight: Int = {
    if (colsInfo.length > 0) {
      colsInfo.iterator
        .map { _.heightUsed }
        .max
    } else {
      0
    }
  }

}


/** Дефолтовая реализация модели [[IGridBuilderState]]. */
case class MGridBuilderState(
  override val colsInfo          : Array[MColumnState],
  override val leftPtr           : Int                 = MGridBuilderState.leftPtrDflt,
  override val cLine             : Int                 = 0,
  override val currColumn        : Int                 = 0,
  override val isAdd             : Boolean             = false
)
  extends IGridBuilderState {

  override def exportState = this

}


/** Изменяемый контекст состояния. Используется для обертки при запуске билдера. */
trait MutableState extends IGridBuilderState {
  def _builderState: IGridBuilderState

  var colsInfo    : Array[MColumnState] = _builderState.colsInfo.clone()
  var leftPtr     : Int                 = _builderState.leftPtr
  var cLine       : Int                 = _builderState.cLine
  var currColumn  : Int                 = _builderState.currColumn
  var isAdd       : Boolean             = _builderState.isAdd
}
