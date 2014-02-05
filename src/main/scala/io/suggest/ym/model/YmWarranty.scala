package io.suggest.ym.model

import io.suggest.ym.YmParsers
import org.joda.time.Period

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.01.14 16:25
 * Description: Тут различные режимы предоставления гарантии.
 */

object Warranty extends Serializable {
  import YmParsers._
  def apply(s: String): Warranty = parse(WARRANTY_PARSER, s).get
}

sealed trait Warranty extends Serializable {
  def hasWarranty: Boolean
  def periodOpt: Option[Period]
  def raw: String
}

case object NoWarranty extends Warranty {
  def hasWarranty: Boolean = false
  def periodOpt: Option[Period] = None
  def raw: String = "false"
}

case object WarrantyNoPeriod extends Warranty {
  def hasWarranty: Boolean = true
  def periodOpt: Option[Period] = None
  def raw: String = "true"
}

case class HasWarranty(p: Period) extends Warranty {
  def hasWarranty: Boolean = true
  def periodOpt: Option[Period] = Some(p)
  def raw: String = p.toString
}
