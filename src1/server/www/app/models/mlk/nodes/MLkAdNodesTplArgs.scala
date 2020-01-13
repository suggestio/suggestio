package models.mlk.nodes

import io.suggest.n2.node.MNode

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.03.17 18:52
  * Description: Модель аргументов для шаблона [[views.html.lk.nodes.AdNodesTpl]].
  */
case class MLkAdNodesTplArgs(
                              formState   : String,
                              mad         : MNode,
                              producer    : MNode
                            )
