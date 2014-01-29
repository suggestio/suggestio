package io.suggest.util

import org.scalatest._
import TextUtil._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.03.13 17:19
 * Description: Различные тесты для TextUtil.
 */

class TextUtilTest extends FlatSpec with Matchers {

  "trgm_token_std()" should "trigrammize ASCII string tokens" in {
    trgmTokenStd("asdfgh") should equal (List(" as", "asd", "sdf", "dfg", "fgh", "gh ").reverse)
    trgmTokenStd("abc")    should equal (List(" ab", "abc", "bc ").reverse)
    trgmTokenStd("a")      should equal (List(" a "))
    trgmTokenStd("")       should equal (List())
  }

  it should "trigrammize unicode tokens" in {
    trgmTokenStd("путин")  should equal (List(" пу", "пут", "ути", "тин", "ин ").reverse)
    trgmTokenStd("в")      should equal (List(" в "))
  }


  "trgm_token_full()" should "fully trgmize tokens" in {
    trgmTokenFull("putin") should equal (List("  p", " pu", "put", "uti", "tin", "in ").reverse)
    trgmTokenFull("вася")  should equal (List("  в", " ва", "вас", "ася", "ся ").reverse)
    trgmTokenFull("с")     should equal (List("  с", " с ").reverse)
    trgmTokenFull("гg")    should equal (List("  г", " гg", "гg ").reverse)
    trgmTokenFull("")      should equal (List())
  }


  "trgm_token_min()" should "minimally trgm tokens" in {
    trgmTokenMin("meat")   should equal (List("mea", "eat").reverse)
    trgmTokenMin("сало")   should equal (List("сал", "ало").reverse)
    trgmTokenMin("с")      should equal (List())
    trgmTokenMin("")       should equal (List())
  }


  "trgm_token_min_end()" should "minimally trgm tokens" in {
    trgmTokenMinEnd("vodka") should equal (List("vod", "odk", "dka", "ka ").reverse)
    trgmTokenMinEnd("пиво")  should equal (List("пив", "иво", "во ").reverse)
    trgmTokenMinEnd("и")     should equal (List(" и "))
    trgmTokenMinEnd("")      should equal (List())
  }

  "normalize()" should "normalize stuff" in {
    normalize("АбВ") should equal ("абв")
    normalize("asd") should equal ("asd")
  }

}
