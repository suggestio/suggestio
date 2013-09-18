package io.suggest.model

import org.scalatest._
import matchers.ShouldMatchers
import MDVIActive._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.09.13 19:19
 * Description:
 */
class MDVIActiveTest extends FlatSpec with ShouldMatchers {

  "serializeToDkeylessTuple()" should "be deserializable" in {
    val dkey = "aversimage.ru"
    val dvi1 = MDVIActive(dkey, "asdasdasd1", 0)
    deserializeFromTupleDkey(dkey, serializeToDkeylessTuple(dvi1)) should equal (dvi1)
  }

}
