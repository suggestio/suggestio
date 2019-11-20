package io.suggest.ad.blk.ent

import io.suggest.font.{MFontSizes, MFonts}
import io.suggest.test.json.PlayJsonTestUtil
import io.suggest.text.MTextAligns
import org.scalatest.flatspec.AnyFlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.10.15 14:59
 * Description: Тесты для модели [[EntFont]].
 */
class EntFontSpec extends AnyFlatSpec with PlayJsonTestUtil {

  override type T = EntFont

  "play-JSON" should "handle minimal model" in {
    jsonTest( EntFont() )
  }

  it should "handle full-filled model" in {
    jsonTest {
      EntFont(
        color   = "55ff33",
        size    = Some( MFontSizes.F14 ),
        align   = Some( MTextAligns.Center ),
        family  = Some( MFonts.NewspaperSans )
      )
    }
  }

}
