package io.suggest.model.n2.node.meta

import io.suggest.test.json.PlayJsonTestUtil
import org.scalatest.FlatSpec
import MBusinessInfoEs.MBUSINESS_INFO_FORMAT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.09.15 15:27
 * Description: Тесты для модели [[MBusinessInfo]].
 */
class MBusinessInfoSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = MBusinessInfo

  "JSON" should "handle minimal/empty model" in {
    jsonTest( MBusinessInfo.empty )
    jsonTest( MBusinessInfo() )
  }

  it should "handle fullfilled model" in {
    jsonTest {
      MBusinessInfo(
        siteUrl         = Some("http://isuggest.ru/index.html"),
        audienceDescr   = Some("Очень vip публика ходит тут.\n\n\r\tВот<>!@#@!#\n."),
        humanTraffic = Some(2000),
        info            = Some("Some info -_-ar3\nsdrgse5g \r\t fsg435 \n")
      )
    }
  }

}
