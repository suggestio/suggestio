package io.suggest.lk.nodes.form.m

import diode.data.Pot

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.03.17 11:05
  * Description: Состояние обновления галочки isEnabled.
  */
case class MNodeEnabledUpdateState(
                                  newIsEnabled      : Boolean,
                                  request           : Pot[_]
                                ) {

  def withRequest(req: Pot[_]) = copy(request = req)

}
