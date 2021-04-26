package io.suggest.bill.price.dsl

import io.suggest.bill.{MCurrencies, MPrice}
import MCurrencies.RUB
import minitest._
import scalaz.{EphemeralStream, Equal, Tree}

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
    val dsl = Tree.Node(
      PriceDsl(
        term = PriceDslTerms.Mapper,
        multiplifier = Some( 3.0 ),
      ),
      EphemeralStream(
        Tree.Leaf {
          PriceDsl(
            term = PriceDslTerms.BasePrice,
            price = Some( MPrice(100, RUB ) ),
          )
        }
      )
    )

    assertEquals( dsl.price, MPrice(300, RUB) )
  }


  test("Return correct price after summing") {
    val dsl = Tree.Node(
      PriceDsl(
        term = PriceDslTerms.Sum,
      ),
      EphemeralStream(
        Tree.Leaf(
          PriceDsl(
            term = PriceDslTerms.BasePrice,
            price = Some( MPrice(100, RUB) ),
          )
        ),
        Tree.Leaf(
          PriceDsl(
            term = PriceDslTerms.BasePrice,
            price = Some( MPrice(150, RUB) ),
          )
        ),
      )
    )

    assertEquals( dsl.price, MPrice(250, RUB) )
  }


  test("Return correct price after mult+sum+mult") {
    val dsl = Tree.Node(
      PriceDsl(
        term = PriceDslTerms.Mapper,
        multiplifier = Some( 2.0 ),
      ),
      EphemeralStream(
        Tree.Node(
          PriceDsl(
            term = PriceDslTerms.Sum,
          ),
          EphemeralStream(
            Tree.Leaf(
              PriceDsl(
                term = PriceDslTerms.BasePrice,
                price = Some( MPrice(100, RUB) ),
              )
            ) * 2.0,
            Tree.Leaf(
              PriceDsl(
                term = PriceDslTerms.BasePrice,
                price = Some( MPrice(150, RUB) ),
              )
            ) * 2.0,
          ),
        )
      )
    )

    assertEquals( dsl.price, MPrice(1000, RUB) )
  }



  // --------------------------------
  // splitOnSum()
  // --------------------------------
  test("Correctly split items on Sum with simple sum") {
    val items = EphemeralStream(
      Tree.Leaf(
        PriceDsl(
          term = PriceDslTerms.BasePrice,
          price = Some( MPrice(100, RUB) ),
        )
      ),
      Tree.Leaf(
        PriceDsl(
          term = PriceDslTerms.BasePrice,
          price = Some( MPrice(150, RUB) ),
        )
      ) * 2.0
    )

    val dsl = Tree.Node(
      PriceDsl(
        term = PriceDslTerms.Sum,
      ),
      items,
    )

    assert(
      Equal[EphemeralStream[Tree[PriceDsl]]].equal(
        dsl.splitOnSum,
        items
      )
    )
  }


  /** Тест наиболее близок к реальным TfDaily-тарифам.
    * Есть внешнее домножение, есть внутреннее, есть суммирование.
    */
  test("Split items on Sum in non-simple cases") {
    val multedBy = 3.0

    val items = EphemeralStream(
      Tree.Leaf(
        PriceDsl(
          term = PriceDslTerms.BasePrice,
          price = Some( MPrice(100, RUB) ),
        )
      ) * 2.0,
      Tree.Leaf(
        PriceDsl(
          term = PriceDslTerms.BasePrice,
          price = Some( MPrice(150, RUB) ),
        )
      ) * 2.0,
    )

    val dsl = Tree.Node(
      PriceDsl(
        term = PriceDslTerms.Mapper,
        multiplifier = Some( multedBy ),
      ),
      EphemeralStream(
        Tree.Node(
          PriceDsl(
            term = PriceDslTerms.Sum,
          ),
          items,
        )
      )
    )

    assert(
      Equal[EphemeralStream[Tree[PriceDsl]]].equal(
        dsl.splitOnSum,
        items.map(_ * multedBy)
      )
    )
  }

}
