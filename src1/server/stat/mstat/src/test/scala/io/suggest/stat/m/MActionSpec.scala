package io.suggest.stat.m

import org.scalatest.FlatSpec

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.09.16 11:00
  * Description: Тесты для модели [[MAction]].
  */
class MActionSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = MAction

  "JSON" should "support minimal model" in {
    jsonTest {
      MAction(
        actions   = Seq( MActionTypes.Person ),
        nodeId    = Seq( "asdasdq123123123" ),
        nodeName  = Nil
      )
    }
  }

  it should "support full-filled model" in {
    jsonTest {
      MAction(
        actions   = Seq( MActionTypes.Person, MActionTypes.ScIndexCovering ),
        nodeId    = Seq( "asdasdq123123123", "23sergsergserg" ),
        nodeName  = Seq( "Asd asd asd", "varvaevreverv" ),
        count     = Seq(1, 5, 7),
        textNi    = Seq("test 1 2 3", "a b c d e f", "q w erty")
      )
    }
  }

}
