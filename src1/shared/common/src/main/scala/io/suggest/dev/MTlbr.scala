package io.suggest.dev

import io.suggest.common.empty.EmptyProduct
import io.suggest.common.html.HtmlConstants
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.06.18 17:38
  * Description: Вспомогательная модель для описания группы 2d-сдвигов: top-left-bottom-right.
  * Модель неявно-пустая.
  *
  * Изначально создана для задания screen safe-area.
  * Safe-area - территория экрана, на которой можно отображать контент, а не только фон.
  * Модель описывает возможные отступы от краёв, на которых контент нельзя рендерить.
  * Появилась для iphone10+, где экран имеет вырез сверху.
  */
object MTlbr {

  def empty = apply()

  implicit def univEq: UnivEq[MTlbr] = UnivEq.derive

  private def _offsetOr0(offOpt: Option[Int]): Int =
    offOpt getOrElse 0

}


/** Контейнер данных геометрии безопасной (пригодной для контента) зоны экрана устройства.
  * Только положительные значения.
  *
  * @param topO Отступ безопасной зоны экрана сверху, если есть.
  * @param bottomO Отступ снизу, если есть.
  * @param leftO Отступ слева, если есть.
  * @param rightO Отступ справа, если есть.
  */
case class MTlbr(
                  topO       : Option[Int]     = None,
                  leftO      : Option[Int]     = None,
                  rightO     : Option[Int]     = None,
                  bottomO    : Option[Int]     = None,
                )
  extends EmptyProduct
{

  import MTlbr._offsetOr0

  def withTopO(topO: Option[Int]) = copy(topO = topO)
  def withLeftO(leftO: Option[Int]) = copy(leftO = leftO)
  def withRightO(rightO: Option[Int]) = copy(rightO = rightO)
  def withBottomO(bottomO: Option[Int]) = copy(bottomO = bottomO)

  // Быстрый доступ к фактическим значенияем сдвигов (без Option).
  def top = _offsetOr0(topO)
  def left = _offsetOr0(leftO)
  def right = _offsetOr0(rightO)
  def bottom= _offsetOr0(bottomO)

  def width: Int = left + right
  def height: Int = top + bottom

  /** Отрендерить в строку вида +3+++ (или +3+4+5+6). */
  override def toString: String = {
    val delim = HtmlConstants.PLUS
    productIterator
      .flatMap {
        case v: Option[_] =>
          delim :: v.asInstanceOf[Option[Int]].toList
        case _ => Nil
      }
      .mkString
  }

}
