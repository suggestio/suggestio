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

  val floatRE = """-?(\d+([.,]+\d*)?|\d*[.,]+\d+)""".r

  /** Генератор регэкспов для форматированных float-чисел. */
  def floatGroupedRE(groupingSepRE: String, fracSepRE: String): String = {
    s"""(?U)-?(\d(\d|$groupingSepRE)*($fracSepRE+\d*)?|\d*$fracSepRE+\d+)"""
  }

}

// TODO Нужно собрать парсеры для различных float-форматов согласно http://docs.oracle.com/cd/E19455-01/806-0169/overview-9/index.html

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

  val floatStrP: Parser[String] = ParserUtil.floatRE

  /** Парсер float-чисел, вводимых юзерами. Не исключаем, что юзеры могут вводить float-числа, начиная их с запятой. */
  val floatP: Parser[Float] = floatStrP ^^ str2FloatF

}


/** Парсер процентных значений. */
object PercentParser extends JavaTokenParsers with CommonParsers {

  val pcSignParser: Parser[String] = "%"

  // Закомменчено - пока не используется.
  /*val percentParser = {
    floatP <~ pcSignParser
  }*/

}
