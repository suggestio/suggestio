package io.suggest.ym

import scala.util.parsing.combinator._
import org.joda.time.Period
import org.joda.time.format.ISOPeriodFormat

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.01.14 23:54
 * Description: Набор парсеров, используемых для восприятия значений в форматах, используемых в YML.
 */
object YmParsers extends JavaTokenParsers {

  /** Парсер измерений (размерности) товара в формате "длина/ширина/высота". */
  val DIMENSIONS_PARSER = {
    val sepParser: Parser[String] = "/"
    val dn = decimalNumber ^^ { _.toFloat }
    dn ~ (sepParser ~> dn) ~ (sepParser ~> dn) ^^ {
      case ls ~ ws ~ hs  =>  Dimensions(ls, ws, hs)
    }
  }

  /** Парсер периода времени в формате iso8601. Используется, например, для описания гарантии на товар. */
  val ISO_TIMEPERIOD_PARSER: Parser[Period] = {
    val isoRe = "(?i)[P]([0-9.]+[YMWDHMST]){1,}".r
    isoRe ^^ {
      iso8601 => Period.parse(iso8601.toUpperCase, ISOPeriodFormat.standard)
    }
  }


  /** Парсер обычных булевых значений вида true/false из строк. */
  val PLAIN_BOOL_PARSER: Parser[Boolean] = {
    val boolRe = "(?i)(true|false)".r
    boolRe ^^ { _.toBoolean }
  }


  /** Для парсинга гарантии применяется комбинация из boolean-парсера и парсера периода времени. */
  val WARRANTY_PARSER: Parser[Warranty] = {
    val bp = PLAIN_BOOL_PARSER ^^ { Warranty(_) }
    val pp = ISO_TIMEPERIOD_PARSER ^^ {
      period => Warranty(hasWarranty=true, warrantyPeriod=Some(period))
    }
    bp | pp
  }

}
