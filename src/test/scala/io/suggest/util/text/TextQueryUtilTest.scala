package io.suggest.util.text

import org.scalatest._
import TextQueryUtil._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.06.14 9:11
 * Description: Тесты для TextQueryUtil, портированы из web21 (где раньше располагалась TextQueryUtil).
 */

class TextQueryUtilTest extends FlatSpec with Matchers {

  "splitQueryStr()" should "split user's ASCII string query into fts and engram parts" in {
    splitQueryStr("kpss asd").get         shouldBe ("kpss", "asd")
    splitQueryStr("kpss").get             shouldBe ("", "kpss")
    splitQueryStr("asd ").get             shouldBe ("asd", "")
    splitQueryStr("asdf asdf asdf\t").get shouldBe ("asdf asdf asdf", "")
    splitQueryStr("asdf dhtg 200").get    shouldBe ("asdf dhtg", "200")
  }

  it should "split user's UTF-8 string query into fts and engram parts" in {
    splitQueryStr("мудак").get            shouldBe ("", "мудак")
    splitQueryStr("все мудаки").get       shouldBe ("", "мудаки")  //("все", "мудаки") - видимо из-за фильтра стоп-слов тут "все" срезаются.
    splitQueryStr("мудак чудак ").get     shouldBe ("мудак чудак", "")
  }

}
