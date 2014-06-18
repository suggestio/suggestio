package util

import org.specs2.mutable._
import SiowebEsUtil._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.13 14:09
 * Description: Тесты для генератора запросов к ElasticSearch и всей его утили.
 */

class SiowebEsUtilSpec extends Specification {

  "splitQueryStr()" should {

    "Move ES highlight tags to the end of the word" in {
      moveHlTags("все <em>а</em>спекты")    must beEqualTo("все <em>а</em>спекты")       // Короткие префиксы не подсвечиваются
      moveHlTags("все <em>асп</em>екты ")   must beEqualTo("все <em>аспекты</em> ")      // Длинные -- исправляются
      moveHlTags("все <em>асп</em>екты <em>рекл</em>амы") must beEqualTo("все <em>аспекты</em> <em>рекламы</em>")   // Тестируем несколько совпадений в одной строке
    }

  }

}
