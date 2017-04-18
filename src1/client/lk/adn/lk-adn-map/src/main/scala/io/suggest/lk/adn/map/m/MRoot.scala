package io.suggest.lk.adn.map.m

import diode.FastEq
import io.suggest.adv.free.MAdv4Free
import io.suggest.maps.m.MMapS

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.04.17 21:32
  * Description: Корневая модель состояния компонента fsm-mvm.
  */
object MRoot {

  /** Поддержка FastEq. */
  implicit object MRootFastEq extends FastEq[MRoot] {
    override def eqv(a: MRoot, b: MRoot): Boolean = {
      (a.map eq b.map) &&
        (a.lam eq b.lam) &&
        (a.adv4free eq b.adv4free)
    }
  }

}


/** Класс корневой модели состояния формы.
  *
  * @param map Состояние географической карты.
  * @param lam Состояния размещения.
  */
case class MRoot(
                  map           : MMapS,
                  lam           : MLamS,
                  adv4free      : Option[MAdv4Free]
                ) {

  def withMap(map2: MMapS) = copy(map = map2)
  def withLam(lam2: MLamS) = copy(lam = lam2)
  def withAdv4Free(a4fOpt: Option[MAdv4Free]) = copy(adv4free = a4fOpt)

}
