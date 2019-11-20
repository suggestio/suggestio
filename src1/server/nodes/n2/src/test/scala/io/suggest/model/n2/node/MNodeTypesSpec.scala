package io.suggest.model.n2.node

import io.suggest.test.json.PlayJsonTestUtil
import org.scalatest.flatspec.AnyFlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.09.15 10:02
 * Description: Тесты для модели MNodeTypes
 */
class MNodeTypesSpec extends AnyFlatSpec with PlayJsonTestUtil {

  override type T = MNodeType

  "JSON" should "handle all values" in {
    for (v <- MNodeTypes.values) {
      jsonTest(v)
    }
  }


  import MNodeTypes._

  "hasParent()" should "!Person > Person" in {
    assert(!(Person hasParent Person), Person)
  }
  it should "Media.Image > Media" in {
    assert(Media.Image hasParent Media, Media.Image)
  }


  "eqOrHasParent()" should "Person >= Person" in {
    assert(Person eqOrHasParent Person, Person)
  }
  it should "Image >= Media" in {
    assert(Media.Image eqOrHasParent Media, Media.Image)
  }
  it should "Media >= Media" in {
    assert(Media eqOrHasParent Media, Media)
  }

}
