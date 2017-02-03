package util.ble

import com.google.inject.Inject
import io.suggest.mbill2.m.item.MItems
import io.suggest.mbill2.m.item.typ.MItemTypes
import io.suggest.util.logs.MacroLogsImpl
import models.mproj.ICommonDi
import util.billing.Bill2Util

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.11.16 18:31
  * Description: Биллинг для BLE-маячков.
  * Биллинг подразумевает, что маячки могут быть активны и неактивны.
  */
class BeaconsBilling @Inject() (
  bill2Util                           : Bill2Util,
  mItems                              : MItems,
  val mCommonDi                       : ICommonDi
)
  extends MacroLogsImpl
{

  import mCommonDi.slick.profile.api._


  /** Поиск всех маячков и прочих узлов для указанного ресивера. */
  def findActiveBeaconIdsOfRcvr(nodeId: String): DBIOAction[Seq[String], Streaming[String], Effect.Read] = {
    mItems.query
      .filter { i =>
        i.withRcvrs( nodeId :: Nil ) &&
        i.withTypes( MItemTypes.BleBeaconActive :: Nil )
      }
      .map(_.nodeId)
      .distinct
      .result
  }

}
