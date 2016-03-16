package io.suggest.lk.adv.geo.tags.fsm

import io.suggest.fsm.StateData
import io.suggest.lk.adv.fsm.IUpdatePriceDataStart
import io.suggest.lk.adv.geo.tags.m.MAgtStateData
import io.suggest.sjs.common.fsm.SjsFsm

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.03.16 12:29
  * Description: stub-trait для разработки аддонов для AdvGeoTags FSM [[AgtFormFsm]].
  */
trait AgtFormFsmStub
  extends SjsFsm
  with StateData
  with IUpdatePriceDataStart
{

  override type SD = MAgtStateData
  override protected var _stateData: SD = MAgtStateData()

}
