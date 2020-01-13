package models.adv.geo

import io.suggest.n2.node.MNode

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.11.15 15:05
  * Description: Модель аргументов шаблона [[views.html.lk.adv.geo.AdvGeoForAdTpl]].
  */
case class MForAdTplArgs(
                          mad              : MNode,
                          producer         : MNode,
                          formState        : String
                        )
