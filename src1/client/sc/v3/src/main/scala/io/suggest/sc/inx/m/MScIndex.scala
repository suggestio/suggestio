package io.suggest.sc.inx.m

import diode.data.Pot
import io.suggest.sc.index.MSc3IndexResp

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.07.17 10:31
  * Description: Модель, описывающая индекс выдачи и его состояния.
  */
case class MScIndex(
                     state : MScIndexState,
                     resp  : Pot[MSc3IndexResp] = Pot.empty
                   ) {

  def withState(state: MScIndexState)     = copy(state = state)
  def withResp(resp: Pot[MSc3IndexResp])  = copy(resp = resp)

}
