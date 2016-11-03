package models.madn.mapf

import io.suggest.model.n2.node.MNode

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.11.16 16:20
  * Description: Модель аргументов для шаблона [[views.html.lk.adn.mapf.adnMapTpl]].
  */

trait IAdnMapTplArgs {
  def mnode: MNode
}


/** Дефолтовая реализация модели [[IAdnMapTplArgs]]. */
case class MAdnMapTplArgs(
  override val mnode      : MNode
)
  extends IAdnMapTplArgs
