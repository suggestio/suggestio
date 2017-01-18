package io.suggest.model.n2.bill.tariff.daily

import io.suggest.bill.MCurrencies
import io.suggest.model.PlayJsonTestUtil
import org.scalatest.FlatSpec

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.01.17 12:02
  * Description: Тесты для модели [[MDailyTf]].
  */
class MDailyTfSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = MDailyTf

  "JSON" should "handle minimal model" in {
    val m = MDailyTf(
      currency = MCurrencies.RUB,
      clauses = MDayClause.clauses2map(
        MDayClause(
          name    = "weekday",
          amount  = 10
        )
      )
    )
    jsonTest(m)
  }

  it should "handle full-filled model" in {
    val m = MDailyTf(
      currency = MCurrencies.EUR,
      clauses = MDayClause.clauses2map(
        MDayClause(
          name    = "weekend",
          amount  = 15,
          calId   = None
        ),
        MDayClause(
          name    = "weekday",
          amount  = 10,
          calId   = Some("64hrethrh6h45h4")
        )
      ),
      comissionPc = Some(0.3)
    )
    jsonTest(m)
  }

}
