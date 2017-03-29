package io.suggest.lk.nodes.form.m

import diode.FastEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.03.17 12:45
  * Description: Модель состояния менюшки узла.
  * Если инстанс модели существует, значит менюшка отображается.
  */

object MNodeMenuS {

  implicit object MNodeMenuSFastEq extends FastEq[MNodeMenuS] {
    override def eqv(a: MNodeMenuS, b: MNodeMenuS): Boolean = {
      //a.hideTimerOpt eq b.hideTimerOpt
      true
    }
  }

}

case class MNodeMenuS(
                       //hideTimerOpt   : Option[Long]    = None
                     ) {

  //def withHideTimer(timerOpt: Option[Long]) = copy( hideTimerOpt = timerOpt )

}

