package io.suggest.n2.node.meta

import io.suggest.color.{MColorData, MColors}
import io.suggest.test.json.PlayJsonTestUtil
import org.scalatest.flatspec.AnyFlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.09.15 13:22
 * Description: Тесты для модели метаданных [[MAddress]].
 */
class MMetaSpec extends AnyFlatSpec with PlayJsonTestUtil {

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
