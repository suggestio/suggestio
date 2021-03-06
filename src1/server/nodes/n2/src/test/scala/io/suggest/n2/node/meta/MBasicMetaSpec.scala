package io.suggest.n2.node.meta

import java.time.OffsetDateTime

import io.suggest.test.json.PlayJsonTestUtil
import org.scalatest.flatspec.AnyFlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.09.15 12:53
 * Description: Тесты для модели [[MBasicMeta]].
 */
class MBasicMetaSpec extends AnyFlatSpec with PlayJsonTestUtil {

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
        dateCreated   = OffsetDateTime.now.minusMinutes(5),
        dateEdited    = Some( OffsetDateTime.now.plusSeconds(1) ),
        langs         = List("ru")
      )
    }
  }

}
