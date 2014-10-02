package io.suggest.ym.model.common

import io.suggest.ym.model.common.EMReceivers.Receivers_t
import org.scalatest._
import SinkShowLevels._
import Matchers._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.10.14 10:58
 * Description: Тесты для суб-модели карты ресиверов.
 */
class EMReceiversSpec extends WordSpec {

  "AdReceiverInfo.mergeMaps()" must {

    import AdReceiverInfo._

    val allGeoSsl = Set(GEO_PRODUCER_SL, GEO_CATS_SL, GEO_START_PAGE_SL)

    "merge simple maps with one rcvrId" in {
      val rcvr = "HMjoIElnTZS7CQYe9c_--A"
      val rcvrInfo0 = AdReceiverInfo(rcvr, Set(GEO_PRODUCER_SL))
      val map0: Receivers_t = Map(rcvrInfo0.receiverId -> rcvrInfo0)
      mergeRcvrMaps(map0, map0)       shouldBe  Map(rcvr -> rcvrInfo0)
      mergeRcvrMaps(map0, Map.empty)  shouldBe  Map(rcvr -> rcvrInfo0)
      mergeRcvrMaps(Map.empty, map0)  shouldBe  Map(rcvr -> rcvrInfo0)
      mergeRcvrMaps(Map.empty, map0)  shouldBe  Map(rcvr -> rcvrInfo0)

      val rcvrInfo1 = AdReceiverInfo(rcvr, allGeoSsl)
      val map1: Receivers_t = Map(rcvrInfo1.receiverId -> rcvrInfo1)
      mergeRcvrMaps(map1, Map.empty)  shouldBe  Map(rcvr -> rcvrInfo1)
      mergeRcvrMaps(Map.empty, map1)  shouldBe  Map(rcvr -> rcvrInfo1)
      mergeRcvrMaps(map1, map1)       shouldBe  Map(rcvr -> rcvrInfo1)

      // Тестим обе карты
      mergeRcvrMaps(map0, map1)       shouldBe  Map(rcvr -> rcvrInfo1)
      mergeRcvrMaps(map1, map0)       shouldBe  Map(rcvr -> rcvrInfo1)
    }

  }

}
