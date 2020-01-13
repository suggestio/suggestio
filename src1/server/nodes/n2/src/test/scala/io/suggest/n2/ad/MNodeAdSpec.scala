package io.suggest.n2.ad

import io.suggest.ad.blk.BlockMeta
import io.suggest.ad.blk.ent.{EntFont, MEntity, TextEnt}
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.n2.ad.rd.RichDescr
import io.suggest.test.json.PlayJsonTestUtil
import org.scalatest.flatspec.AnyFlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.10.15 18:24
 * Description: Тесты для модели [[MNodeAd]].
 */
class MNodeAdSpec extends AnyFlatSpec with PlayJsonTestUtil {

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
            text = Some(TextEnt("asdasd", EntFont("FFFFFF"))),
            coords = Some( MCoords2di(11, -10) )
          )
          val e3 = MEntity(
            id = 3,
            text = Some(TextEnt("yyh rthgrt", EntFont("AAFF44"))),
            coords = Some( MCoords2di(222, 333) )
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
