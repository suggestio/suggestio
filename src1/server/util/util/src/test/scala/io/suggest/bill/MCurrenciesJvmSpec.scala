package io.suggest.bill

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._
import play.api.libs.json.{Format, Json}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.01.17 12:16
  * Description: Тесты для серверной модели [[MCurrencies]].
  */
class MCurrenciesJvmSpec extends AnyFlatSpec {

  "JSON Format" should "work for all supported currencies" in {
    for (v <- MCurrencies.values) {
      val jsonVal = Json.toJson(v)

      val jsRes = implicitly[Format[MCurrency]].reads( jsonVal )
      assert(jsRes.isSuccess, jsRes)
      jsRes.get shouldBe v
    }
  }

}
