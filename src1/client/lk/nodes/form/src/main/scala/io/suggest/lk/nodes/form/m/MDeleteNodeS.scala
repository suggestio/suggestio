package io.suggest.lk.nodes.form.m

import diode.data.Pot

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.03.17 18:03
  * Description: Модель состояния компонента удаления узла.
  */
case class MDeleteNodeS(
                         request: Pot[_] = Pot.empty
                       ) {

  def withRequest(req2: Pot[_]) = copy(request = req2)

}
