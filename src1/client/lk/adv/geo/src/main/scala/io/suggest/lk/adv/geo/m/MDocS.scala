package io.suggest.lk.adv.geo.m

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.04.17 22:31
  * Description: Модель состояния компонента краткой документации формы.
  */

case class MDocS(
                  expanded: Boolean = false
                )
{

  def withExpanded( expanded2: Boolean ) = copy( expanded = expanded2 )

}
