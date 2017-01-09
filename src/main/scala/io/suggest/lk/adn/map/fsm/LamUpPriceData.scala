package io.suggest.lk.adn.map.fsm

import io.suggest.lk.adn.map.vm.LamForm
import io.suggest.lk.adv.fsm.UpdateAdvFormPrice

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.11.16 21:43
  * Description: Реализация обновления прайса для lk-adn-map формы.
  */

trait LamUpPriceData
  extends LamFsmStub
  with UpdateAdvFormPrice
{

  /** Объект-компаньон для доступа к форме. */
  override protected def _formCompanion = LamForm

  /** Сохранить timestamp отправки запроса в состояние FSM. */
  override protected def _upSaveXhrTstamp(tstampOpt: Option[Long], sd0: SD): SD = {
    sd0.copy(
      getPriceTs = tstampOpt
    )
  }

  /** Проверить timestamp запроса по данным состояния FSM. */
  override protected def _upXhrGetTstamp(sd0: SD): Option[Long] = {
    sd0.getPriceTs
  }

}
