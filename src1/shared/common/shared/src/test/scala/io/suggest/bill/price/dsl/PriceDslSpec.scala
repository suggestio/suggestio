package io.suggest.bill.price.dsl

import io.suggest.bill.{MCurrencies, MPrice}
import MCurrencies.RUB
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
        price = MPrice(100, RUB)
      )
    )

    assertEquals( dsl.price, MPrice(300, RUB) )
  }


  test("Return correct price after summing") {
    val dsl = Sum(
      BaseTfPrice( MPrice(100, RUB) ) ::
        BaseTfPrice( MPrice(150, RUB) ) ::
        Nil
    )

    assertEquals( dsl.price, MPrice(250, RUB) )
  }


  test("Return correct price after mult+sum+mult") {
    val dsl = Mapper(
      multiplifier = Some(2.0),
      underlying = Sum(
        Seq(
          BaseTfPrice( MPrice(100, RUB) ) * 2.0,
          BaseTfPrice( MPrice(150, RUB ) ) * 2.0
        )
      )
    )
    assertEquals( dsl.price, MPrice(1000, RUB) )
  }



  // --------------------------------
  // splitOnSum()
  // --------------------------------
  test("Correctly split items on Sum with simple sum") {
    val items = Seq(
      BaseTfPrice( MPrice(100, RUB) ),
      BaseTfPrice( MPrice(150, RUB ) ) * 2.0
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
          BaseTfPrice( MPrice(100, RUB) ) * 2.0,
          BaseTfPrice( MPrice(150, RUB ) ) * 2.0
        )
      )
    )
    assertEquals(
      dsl.splitOnSum.toSeq,
      Seq(
        BaseTfPrice( MPrice(100, RUB) ) * 2.0 * 3.0,
        BaseTfPrice( MPrice(150, RUB ) ) * 2.0 * 3.0
      )
    )
  }

}
