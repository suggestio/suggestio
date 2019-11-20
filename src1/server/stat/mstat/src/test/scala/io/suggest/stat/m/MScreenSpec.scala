package io.suggest.stat.m

import io.suggest.common.geom.d2.MOrientations2d
import org.scalatest.flatspec.AnyFlatSpec

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.09.16 11:05
  * Description: Тесты для модели [[MScreen]].
  */
class MScreenSpec extends AnyFlatSpec with PlayJsonTestUtil {

  override type T = MScreen

  "JSON" should "support empty model" in {
    jsonTest( MScreen() )
  }

  it should "support full-filled model" in {
    jsonTest {
      MScreen(
        orientation = Some( MOrientations2d.Vertical ),
        vportPhys   = Some(MViewPort(
          widthPx   = 100,
          heightPx  = 200,
          pxRatio   = Some( 1.3F )
        )),
        vportQuanted = Some(MViewPort(
          widthPx   = 128,
          heightPx  = 256,
          pxRatio   = Some( 1.5F )
        ))
      )
    }
  }

}
