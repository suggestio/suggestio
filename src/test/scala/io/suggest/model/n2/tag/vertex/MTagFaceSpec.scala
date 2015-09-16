package io.suggest.model.n2.tag.vertex

import org.scalatest._, Matchers._
import play.api.libs.json.Json

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.09.15 14:38
 * Description: Тесты для модели [[MTagFace]].
 */
class MTagFaceSpec extends FlatSpec {

  private def mtf1 = MTagFace("лестые грибы")
  private def mtf2 = MTagFace("ягоды")

  "MTagSpec" should "do JSON serialization/deserialization" in {
    val mtf = mtf1
    val jsVal = Json.toJson(mtf)
    val parsed = jsVal.validate[MTagFace]
    assert(parsed.isSuccess, parsed)
    parsed.get  shouldBe  mtf
  }

  it should "do JSON processing with tags map: [TagFacesMap]" in {
    import MTagFace.{facesMapWrites, facesMapReads}
    val _mtf1 = mtf1
    val _mtf2 = mtf2
    val mtfs = Seq(_mtf1, _mtf2)
    val facesMap = MTagFace.faces2map(mtfs)
    val jsVal = Json.toJson(facesMap)
    val mapped = jsVal.validate[TagFacesMap]

    assert(mapped.isSuccess, mapped)
    val mres = mapped.get
    mres.size  shouldBe  mtfs.size

    assert(mres.valuesIterator contains _mtf1,  mres)
    assert(mres.valuesIterator contains _mtf2,  mres)
  }

}
