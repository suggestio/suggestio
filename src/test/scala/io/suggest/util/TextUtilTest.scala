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
    trgm_token_std("asdfgh") should equal (List(" as", "asd", "sdf", "dfg", "fgh", "gh ").reverse)
    trgm_token_std("abc")    should equal (List(" ab", "abc", "bc ").reverse)
    trgm_token_std("a")      should equal (List(" a "))
    trgm_token_std("")       should equal (List())
  }

  it should "trigrammize unicode tokens" in {
    trgm_token_std("путин")  should equal (List(" пу", "пут", "ути", "тин", "ин ").reverse)
    trgm_token_std("в")      should equal (List(" в "))
  }


  "trgm_token_full()" should "fully trgmize tokens" in {
    trgm_token_full("putin") should equal (List("  p", " pu", "put", "uti", "tin", "in ").reverse)
    trgm_token_full("вася")  should equal (List("  в", " ва", "вас", "ася", "ся ").reverse)
    trgm_token_full("с")     should equal (List("  с", " с ").reverse)
    trgm_token_full("гg")    should equal (List("  г", " гg", "гg ").reverse)
    trgm_token_full("")      should equal (List())
  }


  "trgm_token_min()" should "minimally trgm tokens" in {
    trgm_token_min("meat")   should equal (List("mea", "eat").reverse)
    trgm_token_min("сало")   should equal (List("сал", "ало").reverse)
    trgm_token_min("с")      should equal (List())
    trgm_token_min("")       should equal (List())
  }


  "trgm_token_min_end()" should "minimally trgm tokens" in {
    trgm_token_min_end("vodka") should equal (List("vod", "odk", "dka", "ka ").reverse)
    trgm_token_min_end("пиво")  should equal (List("пив", "иво", "во ").reverse)
    trgm_token_min_end("и")     should equal (List(" и "))
    trgm_token_min_end("")      should equal (List())
  }

  "normalize()" should "normalize stuff" in {
    normalize("АбВ") should equal ("абв")
    normalize("asd") should equal ("asd")
  }

}
