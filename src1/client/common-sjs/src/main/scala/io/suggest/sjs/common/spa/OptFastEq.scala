package io.suggest.sjs.common.spa

import diode.FastEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.12.16 12:36
  * Description: Поддержка FastEq для опциональных значений, генерируемых динамически.
  */
object OptFastEq {

  implicit object optFastEqImpl extends FastEq[Option[AnyRef]] {
    override def eqv(a: Option[AnyRef], b: Option[AnyRef]): Boolean = {
      (a eq b) || ((a,b) match {
        case (Some(x), Some(y))   => x eq y
        case (None, None)         => true
        case _                    => false
      })
    }
  }

}


