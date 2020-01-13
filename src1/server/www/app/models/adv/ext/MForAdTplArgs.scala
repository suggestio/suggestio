package models.adv.ext

import io.suggest.n2.node.MNode
import models.adv._
import play.api.data.Form

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.10.15 11:27
 * Description: Модель контейнера аргументов шаблона adv.ext.forAdTpl.
 */
case class MForAdTplArgs(
                          mad        : MNode,
                          producer   : MNode,
                          targets    : Seq[MExtTarget],
                          advForm    : Form[List[MExtTargetInfo]],
                          oneTgForm  : MExtTarget => OneExtTgForm
                        )
