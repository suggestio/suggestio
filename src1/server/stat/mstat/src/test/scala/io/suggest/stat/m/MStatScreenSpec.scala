package io.suggest.stat.m

import io.suggest.common.geom.d2.MOrientations2d
import org.scalatest.flatspec.AnyFlatSpec

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.09.16 11:05
  * Description: Тесты для модели [[MStatScreen]].
  */
class MStatScreenSpec extends AnyFlatSpec with PlayJsonTestUtil {

  override type T = MStatScreen

  "JSON" should "support empty model" in {
    jsonTest( MStatScreen() )
  }

  it should "support full-filled model" in {
    jsonTest {
      MStatScreen(
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
