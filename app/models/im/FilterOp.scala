package models.im

import org.im4java.core.IMOperation

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.14 10:27
 * Description: Фильтрация картинки.
 */

object FilterOp {
  def apply(vs: Seq[String]): FilterOp = {
    apply(vs.head)
  }

  def apply(v: String): FilterOp = {
    val filter: ImFilter = ImFilters.withName(v)
    FilterOp(filter)
  }
}

/**
 * Экземпляр операция фильтра.
 * @param filter Фильтр, который будет отрендерен в строку вызова convert.
 */
case class FilterOp(filter: ImFilter) extends ImOp {
  override def opCode = ImOpCodes.Filter

  override def qsValue = filter.strId

  override def addOperation(op: IMOperation): Unit = {
    op.filter(filter.imName)
  }
}


/** Допустимые фильтры. */
object ImFilters extends Enumeration {

  /**
   * Класс значения этого перечисления.
   * @param strId короткий id фильтра, который рендерится юзеру в ссылке.
   * @param imName название фильтра, которое передаётся в convert -filter.
   */
  protected sealed case class Val(strId: String, imName: String) extends super.Val(strId)

  type ImFilter = Val

  val Lanczos: ImFilter = Val("a", "Lanczos")

  implicit def value2val(x: Value): ImFilter = x.asInstanceOf[ImFilter]
}
