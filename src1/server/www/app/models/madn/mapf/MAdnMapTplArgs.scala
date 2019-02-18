package models.madn.mapf

import io.suggest.model.n2.node.MNode

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.11.16 16:20
  * Description: Модель аргументов для шаблона [[views.html.lk.adn.mapf.AdnMapTpl]].
  */
case class MAdnMapTplArgs(
                           mnode      : MNode,
                           formB64    : String
                         )
