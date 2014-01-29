package io.suggest.ym.parsers

import org.scalatest._
import io.suggest.ym.{MassUnits, YmStringsAnalyzer}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.01.14 16:22
 * Description:
 */
class MassUnitParserTest extends FlatSpec with Matchers {

  import MassUnitParser._

  private def getF[T](p: Parser[T], analyzer:YmStringsAnalyzer): String => T = {
    {pn: String =>
      val pn1 = analyzer.normalizeText(pn)
      parseAll(p, pn1).get
    }
  }


  "WEIGHT_NORM_UNITS_PARSER" should "understand standard units of mass measurments" in {
    import MassUnits._
    val an = new YmStringsAnalyzer
    val f = getF(MASS_NORM_UNITS_PARSER, an)
    f("КГ.")              shouldEqual kg
    f("килограммов")      shouldEqual kg
    f("г")                shouldEqual gramm
    f("гр.")              shouldEqual gramm
    f("grams")            shouldEqual gramm
    f("ц.")               shouldEqual centner
    f("centner")          shouldEqual centner
    f("ct.")              shouldEqual carat
    f("кар.")             shouldEqual carat
    f("караты")           shouldEqual carat
    f("lbs")              shouldEqual lbs
    f("pounds")           shouldEqual lbs
    f("фунтов")           shouldEqual lbs
  }

}
