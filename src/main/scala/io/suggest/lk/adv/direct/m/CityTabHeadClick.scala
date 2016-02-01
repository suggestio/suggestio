package io.suggest.lk.adv.direct.m

import io.suggest.lk.adv.direct.vm.nbar.cities.CityTabHead
import io.suggest.sjs.common.fsm.IFsmMsg
import org.scalajs.dom.Event

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.12.15 14:10
 * Description: Сигнал в FSM о клике по городу в списке городов.
 */
case class CityTabHeadClick(
  event         : Event,
  cityTabHead   : CityTabHead
)
  extends IFsmMsg
