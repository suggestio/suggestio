package io.suggest.text.util

import TextUtil._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.03.13 17:19
 * Description: Различные тесты для TextUtil.
 */

class TextUtilSpec extends AnyFlatSpec {

  "trgm_token_std()" should "trigrammize ASCII string tokens" in {
    trgmTokenStd("asdfgh") shouldEqual List(" as", "asd", "sdf", "dfg", "fgh", "gh ").reverse
    trgmTokenStd("abc")    shouldEqual List(" ab", "abc", "bc ").reverse
    trgmTokenStd("a")      shouldEqual List(" a ")
    trgmTokenStd("")       shouldEqual Nil
  }

  it should "trigrammize unicode tokens" in {
    trgmTokenStd("путин")  shouldEqual List(" пу", "пут", "ути", "тин", "ин ").reverse
    trgmTokenStd("в")      shouldEqual List(" в ")
  }


  "trgm_token_full()" should "fully trgmize tokens" in {
    trgmTokenFull("putin") shouldEqual List("  p", " pu", "put", "uti", "tin", "in ").reverse
    trgmTokenFull("вася")  shouldEqual List("  в", " ва", "вас", "ася", "ся ").reverse
    trgmTokenFull("с")     shouldEqual List("  с", " с ").reverse
    trgmTokenFull("гg")    shouldEqual List("  г", " гg", "гg ").reverse
    trgmTokenFull("")      shouldEqual Nil
  }


  "trgm_token_min()" should "minimally trgm tokens" in {
    trgmTokenMin("meat")   shouldEqual List("mea", "eat").reverse
    trgmTokenMin("сало")   shouldEqual List("сал", "ало").reverse
    trgmTokenMin("с")      shouldEqual Nil
    trgmTokenMin("")       shouldEqual Nil
  }


  "trgm_token_min_end()" should "minimally trgm tokens" in {
    trgmTokenMinEnd("vodka") shouldEqual List("vod", "odk", "dka", "ka ").reverse
    trgmTokenMinEnd("пиво")  shouldEqual List("пив", "иво", "во ").reverse
    trgmTokenMinEnd("и")     shouldEqual List(" и ")
    trgmTokenMinEnd("")      shouldEqual Nil
  }

  "normalize()" should "normalize stuff" in {
    normalize("АбВ") shouldEqual "абв"
    normalize("asd") shouldEqual "asd"
  }


  "fixMischaractersInWord()" should "fix mischars in russian words" in {
    mischarFixString("Аксеccуары")            shouldEqual "Аксессуары"
    mischarFixString("3ашибись").toLowerCase  shouldEqual "зашибись"
    mischarFixString("Ра6ота")                shouldEqual "Работа"
  }

  it should "fix mischars in english words" in {
    mischarFixString("H0use")                 shouldEqual "HOuse" // ноль вместо О
    mischarFixString("Ноuse")                 shouldEqual "House" // Рус. "но" вместо "ho"
  }

}
