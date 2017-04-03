package io.suggest.sjs.common.vm.spa

import diode.data.Pot

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.04.17 11:56
  * Description: Интерфейс для моделей, содержащих Pot-поле.
  */
trait IMPot[+T] extends IMPots {

  def _pot: Pot[T]

  override def _pots: TraversableOnce[Pot[T]] = {
    _pot :: Nil
  }

}


trait IMPots {

  def _pots: TraversableOnce[Pot[_]]

}