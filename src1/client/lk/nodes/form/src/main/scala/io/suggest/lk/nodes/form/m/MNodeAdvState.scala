package io.suggest.lk.nodes.form.m

import diode.data.Pot

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.03.17 10:33
  * Description: Модель данных состояния размещения карточки на указанном узле.
  */
case class MNodeAdvState(
                          newIsEnabled  : Boolean,
                          req           : Pot[_]    = Pot.empty
                        )
{

  def withReq(req2: Pot[_]) = copy(req = req2)

}
