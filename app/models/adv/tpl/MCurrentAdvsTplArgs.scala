package models.adv.tpl

import models._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.11.15 14:49
 * Description: Аргументы для рендера шаблонов с инфой по текущим размещениям карточки.
 */
trait ICurrentAdvsTplArgs {
  def advs      : Seq[MAdvI]
  def adv2adn   : Map[Int, MNode]
}


case class MCurrentAdvsTplArgs(
  override val advs      : Seq[MAdvI],
  override val adv2adn   : Map[Int, MNode]
)
  extends ICurrentAdvsTplArgs
