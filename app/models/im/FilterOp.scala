package models.im

import org.im4java.core.IMOperation

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.14 10:27
 * Description: Фильтрация картинки.
 */

object ImFilters extends Enumeration {

  /**
   * Класс значения этого перечисления.
   * @param qsValue короткий id фильтра, который рендерится юзеру в ссылке.
   * @param imName название фильтра, которое передаётся в convert -filter.
   */
  protected sealed case class Val(qsValue: String, imName: String) extends super.Val(qsValue) with ImOp {
    override def opCode = ImOpCodes.Filter

    override def addOperation(op: IMOperation): Unit = {
      op.filter(imName)
    }
  }

  type ImFilter = Val

  val Lanczos: ImFilter = Val("a", "Lanczos")

  implicit def value2val(x: Value): ImFilter = x.asInstanceOf[ImFilter]

  def apply(vs: Seq[String]): ImFilter = apply(vs.head)
  def apply(v: String): ImFilter = ImFilters.withName(v)
}
