package io.suggest.model.n2.extra

import io.suggest.model.PlayJsonTestUtil
import io.suggest.model.sc.common.AdShowLevels
import io.suggest.ym.model.common.{AdnSinks, AdnRights}
import org.scalatest.FlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.10.15 22:51
 * Description: Тесты для модели [[MAdnExtra]], которая является подмоделью [[MNodeExtras]].
 */
class MAdnExtraSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = MAdnExtra


  "JSON" should "support minimal model" in {
    jsonTest( MAdnExtra() )
  }

  it should "support full-filled model" in {
    jsonTest {
      MAdnExtra(
        rights = Set( AdnRights.PRODUCER, AdnRights.RECEIVER ),
        isUser = true,
        shownTypeIdOpt = Some("a"),
        testNode = true,
        outSls = {
          val sli = MSlInfo(AdShowLevels.LVL_START_PAGE, 10)
          Map(sli.sl -> sli)
        },
        sinks = Set( AdnSinks.SINK_WIFI, AdnSinks.SINK_GEO ),
        showInScNl = true
      )
    }
  }

}
