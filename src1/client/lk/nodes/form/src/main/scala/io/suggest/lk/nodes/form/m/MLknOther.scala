package io.suggest.lk.nodes.form.m

import diode.FastEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.03.17 16:51
  * Description: Модель прочих важных данных формы.
  */
object MLknOther {

  /** Поддержка быстрого сравнивания. */
  implicit object MLknOtherFastEq extends FastEq[MLknOther] {
    override def eqv(a: MLknOther, b: MLknOther): Boolean = {
      a.onNodeId eq b.onNodeId
    }
  }

}

case class MLknOther(
                     onNodeId: String
                    )
