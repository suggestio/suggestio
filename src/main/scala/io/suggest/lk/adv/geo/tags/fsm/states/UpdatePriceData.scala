package io.suggest.lk.adv.geo.tags.fsm.states

import io.suggest.lk.adv.fsm.UpdateAdvFormPrice
import io.suggest.lk.adv.geo.tags.fsm.AgtFormFsmStub
import io.suggest.lk.adv.geo.tags.vm.AgtForm

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.03.16 17:38
  * Description: Аддон для добавления поддержки обновления стоимости размещения и прочих
  * данных с сервера s.io.
  */
trait UpdatePriceData
  extends AgtFormFsmStub
  with UpdateAdvFormPrice
{

  /** Объект-компаньон для доступа к форме. */
  override protected def _formCompanion = AgtForm

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
