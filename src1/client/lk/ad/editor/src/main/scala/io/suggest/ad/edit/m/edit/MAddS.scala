package io.suggest.ad.edit.m.edit

import diode.FastEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.09.17 14:59
  * Description: Состояние формочки добавления нового элемента в карточку.
  */
object MAddS {

  implicit object MAddSFastEq extends FastEq[MAddS] {
    override def eqv(a: MAddS, b: MAddS): Boolean = {
      true
    }
  }

  def default = MAddS()

}


/** Класс модели состояния добавления нового элемента. */
case class MAddS(
                ) {

  //def withIsFormShown(isFormShown: Boolean) = copy(isFormShown = isFormShown)

}
