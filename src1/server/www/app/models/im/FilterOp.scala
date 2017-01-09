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
   */
  protected sealed abstract class Val(val qsValue: String) extends super.Val(qsValue) with ImOp {
    override def opCode = ImOpCodes.Filter

    /** Название фильтра, которое передаётся в convert -filter. */
    def imName: String

    override def addOperation(op: IMOperation): Unit = {
      op.filter(imName)
    }

    override def unwrappedValue: Option[String] = Some(imName)
  }

  type ImFilter = Val

  val Lanczos: ImFilter = new Val("a") {
    override def imName = "Lanczos"
  }


  implicit def value2val(x: Value): ImFilter = x.asInstanceOf[ImFilter]

  def apply(vs: Seq[String]): ImFilter = apply(vs.head)
  def apply(v: String): ImFilter = ImFilters.withName(v)
}
