package io.suggest.ym.parsers

import scala.util.parsing.combinator.JavaTokenParsers

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.03.14 9:35
 * Description:
 */
object ParserUtil {

  /** Функция перегона строки в целое. Используется в date/time-парсерах. */
  def str2IntF(s: String) = s.toInt

  /** Функция перегона строки в число с плавающей точкой. */
  def str2FloatF(s: String) = {
    s.replace(',', '.').toFloat
  }

}


import ParserUtil._

/** Общие парсеры. Из-за особенностей архитектуры scala-парсеров, приходится делать это трейтом. */
trait CommonParsers extends JavaTokenParsers {

  /** Неявный конвертер результата работы парсера в Option[T]. */
  implicit def parseResult2Option[T](pr: ParseResult[T]): Option[T] = {
    if (pr.successful)
      Some(pr.get)
    else
      None
  }

  /** Парсер float-чисел, вводимых юзерами. Не исключаем, что юзеры могут вводить float-числа, начиная их с запятой. */
  val floatP: Parser[Float] = """-?(\d+([.,]+\d*)?|\d*[.,]+\d+)""".r ^^ str2FloatF

}


/** Парсер процентных значений. */
object PercentParser extends JavaTokenParsers with CommonParsers {

  val pcSignParser: Parser[String] = "%"

  // Закомменчено - пока не используется.
  /*val percentParser = {
    floatP <~ pcSignParser
  }*/

}
