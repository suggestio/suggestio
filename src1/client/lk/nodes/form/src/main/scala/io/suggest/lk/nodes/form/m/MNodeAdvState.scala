package io.suggest.lk.nodes.form.m

import diode.data.Pot
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.03.17 10:33
  * Description: Модель данных состояния размещения карточки на указанном узле.
  */
object MNodeAdvState {

  implicit def univEq: UnivEq[MNodeAdvState] = UnivEq.force

}

case class MNodeAdvState(
                          newIsEnabled  : Boolean,
                          req           : Pot[_]    = Pot.empty
                        )
{

  def withReq(req2: Pot[_]) = copy(req = req2)

}
