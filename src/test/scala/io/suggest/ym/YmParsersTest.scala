package io.suggest.ym

import org.scalatest._
import YmParsers._
import org.joda.time._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.01.14 22:46
 * Description: Тесты для YmParsers.
 */
class YmParsersTest extends FlatSpec with Matchers {

  "DIMENSIONS_PARSER" should "parse different dimensions" in {
    val f = {s: String => parse(DIMENSIONS_PARSER, s).get }
    f("207/23/54")          should be (Dimensions(207.0F, 23.0F, 54.0F))
    f("2000/100/200")       should be (Dimensions(2000, 100, 200))
    f("20000/1500/600")     should be (Dimensions(20000, 1500, 600))
    f("\t\t200/150/150 ")   should be (Dimensions(200, 150, 150))
    f(" 200 / 150 / 150 ")  should be (Dimensions(200, 150, 150))
  }


  "ISO_TIMEPERIOD_PARSER" should "parse ISO 8601 periods" in {
    val f = {s: String => parse(ISO_TIMEPERIOD_PARSER, s).get }
    f("P1Y2M10DT2H30M")  should be (new Period().withYears(1).withMonths(2).withDays(10).withHours(2).withMinutes(30))
  }

}
