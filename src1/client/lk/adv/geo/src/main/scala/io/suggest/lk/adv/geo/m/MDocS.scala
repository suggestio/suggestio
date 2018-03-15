package io.suggest.lk.adv.geo.m

import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.04.17 22:31
  * Description: Модель состояния компонента краткой документации формы.
  */
object MDocS {

  implicit def univEq: UnivEq[MDocS] = UnivEq.derive

}


case class MDocS(
                  expanded: Boolean = false
                )
{

  def withExpanded( expanded2: Boolean ) = copy( expanded = expanded2 )

}
