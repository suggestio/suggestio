package io.suggest.ym.parsers

import scala.util.parsing.combinator.JavaTokenParsers
import io.suggest.ym.{TextNormalizerAn, MassUnits}
import MassUnits.MassUnit

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.01.14 16:14
 * Description: Утиль для парсинга единиц измерения массы. Обычно, они встречаются в аттрибуте unit тега param.
 */
object MassUnitParser extends JavaTokenParsers {

  /** Парсер единиц измерения массы, которые указываются в теге param аттрибуте unit. */
  val MASS_NORM_UNITS_PARSER: Parser[MassUnit] = {
    import MassUnits._
    val kgP       = ("кг" | "килограм+".r | "kg" | "kilogram+".r) ^^^ kg
    val gP        = ("г(р(амм?)?)?".r | "g(ram+)".r) ^^^ gramm
    val mgP       = ("мг" | "мил+иг(р(ам+)?)?".r | "mg" | "mil+igram+".r) ^^^ mg
    val tonP      = ("т(он+)?".r | "ton+".r) ^^^ ton
    // TODO Выпилить центнер? Это не стандартизированная историческая единица, она зависит от страны употребления.
    val centnerP  = ("ц(ей?нтнер|т)?".r | "quintal" | "[zc]entner".r | "кв[iи]нтал[ъь]?".r) ^^^ centner
    val caratP    = ("кар(ат)?".r | "ct" | "carat") ^^^ carat
    val lbP       = ("lbs?".r | "фунт" | "pound") ^^^ lbs
    kgP | gP | mgP | tonP | centnerP | caratP | lbP
  }
}

import MassUnitParser._

trait MassUnitParserAn extends TextNormalizerAn {
  /** Распарсить единицы измерения массы. */
  def parseMassUnit(muStr: String) = {
    val muStrNorm = normalizeText(muStr)
    parse(MASS_NORM_UNITS_PARSER, muStrNorm)
  }
}
