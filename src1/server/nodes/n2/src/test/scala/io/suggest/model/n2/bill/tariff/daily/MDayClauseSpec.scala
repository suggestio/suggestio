package io.suggest.model.n2.bill.tariff.daily

import io.suggest.test.json.PlayJsonTestUtil
import org.scalatest.FlatSpec

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.01.17 12:07
  * Description: Тесты для модели [[MDayClause]].
  */
class MDayClauseSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = MDayClause

  "JSON" should "handle minimal model" in {
    jsonTest {
      MDayClause(
        name   = "days",
        amount = 11.44
      )
    }
  }

  it should "handle full-filled model" in {
    jsonTest {
      MDayClause(
        name    = "weekday",
        amount  = 12.22,
        calId   = Some("asdasd-3-423_ewqdwqe3fwfew")
      )
    }
  }

}
