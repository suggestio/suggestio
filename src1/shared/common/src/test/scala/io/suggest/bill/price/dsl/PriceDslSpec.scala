package io.suggest.bill.price.dsl

import io.suggest.bill.{MCurrencies, MPrice}
import MCurrencies.{RUB, USD}
import minitest._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.04.17 13:01
  * Description: Тесты для системы price-dsl.
  */
object PriceDslSpec extends SimpleTestSuite {

  // --------------------------
  // Тесты для price()
  // --------------------------

  test("Return correct price after multiplication") {
    val dsl = Mapper(
      multiplifier = Some(3.0),
      underlying = BaseTfPrice(
        price = MPrice(1.0, RUB)
      )
    )

    assertEquals( dsl.price, MPrice(3.0, RUB) )
  }


  test("Return correct price after summing") {
    val dsl = Sum(
      BaseTfPrice( MPrice(1.0, RUB) ) ::
        BaseTfPrice( MPrice(1.5, RUB) ) ::
        Nil
    )

    assertEquals( dsl.price, MPrice(2.5, RUB) )
  }


  test("Return correct price after mult+sum+mult") {
    val dsl = Mapper(
      multiplifier = Some(2.0),
      underlying = Sum(
        Seq(
          BaseTfPrice( MPrice(1.0, RUB) ) * 2.0,
          BaseTfPrice( MPrice(1.5, RUB ) ) * 2.0
        )
      )
    )
    assertEquals( dsl.price, MPrice(10.0, RUB) )
  }



  // --------------------------------
  // splitOnSum()
  // --------------------------------
  test("Correctly split items on Sum with simple sum") {
    val items = Seq(
      BaseTfPrice( MPrice(1.0, RUB) ),
      BaseTfPrice( MPrice(1.5, RUB ) ) * 2.0
    )
    val dsl = Sum( items )
    assertEquals(
      dsl.splitOnSum.toSeq,
      items
    )
  }

  /** Тест наиболее близок к реальным TfDaily-тарифам.
    * Есть внешнее домножение, есть внутреннее, есть суммирование.
    */
  test("Split items on Sum in non-simple cases") {
    val dsl = Mapper(
      multiplifier = Some(3.0),
      underlying = Sum(
        Seq(
          BaseTfPrice( MPrice(1.0, RUB) ) * 2.0,
          BaseTfPrice( MPrice(1.5, RUB ) ) * 2.0
        )
      )
    )
    assertEquals(
      dsl.splitOnSum.toSeq,
      Seq(
        BaseTfPrice( MPrice(1.0, RUB) ) * 2.0 * 3.0,
        BaseTfPrice( MPrice(1.5, RUB ) ) * 2.0 * 3.0
      )
    )
  }


  // --------------------------------
  // mapPrices()
  // --------------------------------
  test("mapPrices() simply, with price normalization") {
    val dsl = BaseTfPrice( MPrice(1.111111112, RUB) ) * 2.0
    val dsl2 = dsl.mapAllPrices( _.normalizeAmountByExponent )
    assertEquals( dsl2.price.amount, 2.22 )
    assertEquals( dsl2.price.currency, RUB )
  }

}
