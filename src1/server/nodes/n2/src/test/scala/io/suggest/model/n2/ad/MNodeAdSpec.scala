package io.suggest.model.n2.ad

import io.suggest.model.n2.ad.blk.BlockMeta
import io.suggest.model.n2.ad.ent.MEntity
import io.suggest.model.n2.ad.ent.text.{EntFont, TextEnt}
import io.suggest.model.n2.ad.rd.RichDescr
import io.suggest.test.json.PlayJsonTestUtil
import org.scalatest.FlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.10.15 18:24
 * Description: Тесты для модели [[MNodeAd]].
 */
class MNodeAdSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = MNodeAd

  "JSON" should "handle empty model" in {
    jsonTest( MNodeAd() )
  }

  it should "handle full-filled model" in {
    jsonTest {
      MNodeAd(
        entities = {
          val e1 = MEntity(
            id = 1,
            text = Some(TextEnt("asdasd", EntFont("FFFFFF")))
          )
          val e3 = MEntity(
            id = 3,
            text = Some(TextEnt("yyh rthgrt", EntFont("AAFF44")))
          )
          MNodeAd.toEntMap(e1, e3)
        },
        richDescr = Some(RichDescr(
          bgColor = "ffAA44",
          text    = """<b class="text_L">hello world!</b> 213"""
        )),
        blockMeta = Some(BlockMeta.DEFAULT)
      )
    }
  }

}
