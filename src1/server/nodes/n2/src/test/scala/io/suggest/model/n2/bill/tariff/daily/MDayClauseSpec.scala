package io.suggest.model.n2.bill.tariff.daily

import io.suggest.bill.{MCurrencies, MPrice}
import io.suggest.test.json.PlayJsonTestUtil
import org.scalatest.flatspec.AnyFlatSpec

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.01.17 12:07
  * Description: Тесты для модели [[MDayClause]].
  */
class MDayClauseSpec extends AnyFlatSpec with PlayJsonTestUtil {

  override type T = MDayClause

  "JSON" should "handle minimal model" in {
    jsonTest {
      MDayClause(
        name   = "days",
        amount = MPrice.realAmountToAmount( 11.44, MCurrencies.RUB ),
      )
    }
  }

  it should "handle full-filled model" in {
    jsonTest {
      MDayClause(
        name    = "weekday",
        amount  = MPrice.realAmountToAmount( 12.22, MCurrencies.RUB ),
        calId   = Some("asdasd-3-423_ewqdwqe3fwfew")
      )
    }
  }

}
