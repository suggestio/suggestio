package io.suggest.model.n2.node.meta

import io.suggest.model.PlayJsonTestUtil
import org.joda.time.DateTime
import org.scalatest.FlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.09.15 12:53
 * Description: Тесты для модели [[MBasicMeta]].
 */
class MBasicMetaSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = MBasicMeta

  "JSON" should "handle minimal model" in {
    jsonTest(MBasicMeta())
  }

  it should "handle fullfilled model" in {
    jsonTest {
      MBasicMeta(
        nameOpt       = Some("some name"),
        nameShortOpt  = Some("ЯПредлагаю"),
        techName    = Some("nomad.jpg"),
        hiddenDescr   = Some("Все свои, всё схвачено."),
        dateCreated   = DateTime.now.minusMinutes(5),
        dateEdited    = Some( DateTime.now.plusSeconds(1) ),
        langs         = List("ru")
      )
    }
  }

}
