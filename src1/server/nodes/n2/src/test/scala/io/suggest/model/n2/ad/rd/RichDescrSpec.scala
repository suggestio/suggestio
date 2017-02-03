package io.suggest.model.n2.ad.rd

import io.suggest.test.json.PlayJsonTestUtil
import org.scalatest.FlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.10.15 21:31
 * Description: Тесты для json-модели [[RichDescr]].
 */
class RichDescrSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = RichDescr

  "JSON" should "handle model" in {
    jsonTest {
      RichDescr(
        bgColor = "596943",
        text    = """<p>xata - ok<br/></p>"""
      )
    }
  }

}
