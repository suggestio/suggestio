package models.mlk.nodes

import io.suggest.model.n2.node.MNode

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 17:19
  * Description: Модель аргументов для шаблона [[views.html.lk.nodes.NodesTpl]].
  */
case class MLkNodesTplArgs(
                            formState   : String,
                            mnode       : MNode
                          )
