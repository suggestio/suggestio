package io.suggest.stat.m

import io.suggest.common.geom.d2.MOrientations2d
import org.scalatest.flatspec.AnyFlatSpec

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.09.16 10:42
  * Description: Тесты для модели [[MStat]].
  */
class MStatSpec extends AnyFlatSpec with PlayJsonTestUtil {

  override type T = MStat

  "JSON" should "support minimal model" in {
    jsonTest {
      MStat(
        common  = MCommonStat(
          components = Seq(MComponents.Sc)
        ),
        actions = Nil
      )
    }
  }

  it should "support model fill all fields filled" in {
    jsonTest {
      MStat(
        common = MCommonStat(
          components = MComponents.Sc :: Nil
        ),
        actions = Seq(
          MAction(
            actions = Seq( MActionTypes.Person ),
            nodeId  = Seq( "asdasdasdasdasdasdasd" ),
            nodeName = Nil
          )
        ),
        ua = MUa(
          ua = Some("da user agenta / Mozilla 5.0 compatible")
        ),
        screen = MStatScreen(
          orientation = Some( MOrientations2d.Vertical )
        ),
        location = MStatLocation(
          geo = MGeoLocData(
            town = Some("Berdyansk")
          )
        ),
        diag = MDiag(
          message = Some("test test test")
        )
      )
    }
  }

}
