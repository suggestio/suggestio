package models.im

import enumeratum.values.{StringEnum, StringEnumEntry}
import org.im4java.core.IMOperation

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.14 10:27
 * Description: Модель фильтров картинки.
 */

object ImFilters extends StringEnum[ImFilter] {

  object Lanczos extends ImFilter("a") {
    override def imName = "Lanczos"
  }

  override def values = findValues

}


/**
  * Класс значения этого перечисления.
  * @param value короткий id фильтра, который рендерится юзеру в ссылке.
  */
sealed abstract class ImFilter(override val value: String) extends StringEnumEntry with ImOp {

  /** Название фильтра, которое передаётся в convert -filter. */
  def imName: String

  override final def qsValue = value
  override def opCode = ImOpCodes.Filter
  override def addOperation(op: IMOperation): Unit = {
    op.filter(imName)
  }
  override def unwrappedValue: Option[String] = Some(imName)

}

