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

    "split user's ASCII string query into fts and engram parts" in {
      splitQueryStr("kpss asd").get         must beEqualTo(("kpss", "asd"))
      splitQueryStr("kpss").get             must beEqualTo(("", "kpss"))
      splitQueryStr("asd ").get             must beEqualTo(("asd", ""))
      splitQueryStr("asdf asdf asdf\t").get must beEqualTo(("asdf asdf asdf", ""))
      splitQueryStr("asdf dhtg 200").get    must beEqualTo(("asdf dhtg", "200"))
    }

    "split user's UTF-8 string query into fts and engram parts" in {
      splitQueryStr("мудак").get            must beEqualTo(("", "мудак"))
      splitQueryStr("все мудаки").get       must beEqualTo(("все", "мудаки"))
      splitQueryStr("мудак чудак ").get     must beEqualTo(("мудак чудак", ""))
    }

    "Move ES highlight tags to the end of the word" in {
      moveHlTags("все <em>а</em>спекты")    must beEqualTo("все <em>а</em>спекты")       // Короткие префиксы не подсвечиваются
      moveHlTags("все <em>асп</em>екты ")   must beEqualTo("все <em>аспекты</em> ")      // Длинные -- исправляются
      moveHlTags("все <em>асп</em>екты <em>рекл</em>амы") must beEqualTo("все <em>аспекты</em> <em>рекламы</em>")   // Тестируем несколько совпадений в одной строке
    }

  }

}
