package io.suggest.model.n2.node.meta

import io.suggest.model.PlayJsonTestUtil
import io.suggest.model.n2.node.meta.colors.{MColors, MColorData}
import org.scalatest.FlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.09.15 13:22
 * Description: Тесты для модели метаданных [[MAddress]].
 */
class MMetaSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = MMeta

  "JSON" should "handle defaulted model" in {
    jsonTest( MMeta(MBasicMeta()) )
  }

  it should "handle full-filled model" in {
    jsonTest {
      MMeta(
        basic = MBasicMeta(
          nameOpt = Some("axcasdqwd")
        ),
        person = MPersonMeta(
          nameFirst = Some("KKkkasdkkk")
        ),
        address = MAddress(
          town = Some("Petrozavodsk")
        ),
        business = MBusinessInfo(
          siteUrl = Some("https://suggest.io/")
        ),
        colors = MColors(
          bg = Some( MColorData("AA33EE") )
        )
      )
    }
  }

}
