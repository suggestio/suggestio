package util

import org.specs2.mutable._
import util.Stopwords._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.13 12:11
 * Description: Тесты для генератора стоп-слов.
 */

class StopwordsSpec extends Specification {
  ".ALL_STOPS Set" should {

    "detect stopwords for russian" in {
      ALL_STOPS.contains("и")   must beTrue
      ALL_STOPS.contains("или") must beTrue
      ALL_STOPS.contains("астрал") must beFalse
    }

    "detect stopwords for english" in {
      ALL_STOPS.contains("and") must beTrue
      ALL_STOPS.contains("or")  must beTrue
      ALL_STOPS.contains("exec") must beFalse
    }

  }
}
