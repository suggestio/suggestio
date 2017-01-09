package io.suggest.lk.adv.direct.fsm.states

import io.suggest.lk.adv.direct.fsm.FsmStubT
import io.suggest.lk.adv.direct.m.MStateData
import io.suggest.lk.adv.direct.vm.Form
import io.suggest.lk.adv.fsm.UpdateAdvFormPrice

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.01.16 11:38
 * Description: Поддержка аддона для запроса и получения отрендеренной цены
 * и кусков формы с сервера.
 *
 * Т.к. форма в целом стабильна. то состояние не переключается, а только меняются данные в состоянии.
 */
trait GetData extends FsmStubT with UpdateAdvFormPrice {

  /** Объект-компаньон для доступа к форме. */
  override protected def _formCompanion = Form

  /** Сохранить timestamp отправки запроса в состояние FSM. */
  override protected def _upSaveXhrTstamp(tstampOpt: Option[Long], sd0: SD): SD = {
    sd0.copy(
      getPriceTs = tstampOpt
    )
  }

  /** Проверить timestamp запроса по данным состояния FSM. */
  override protected def _upXhrGetTstamp(sd0: MStateData): Option[Long] = {
    sd0.getPriceTs
  }

}
