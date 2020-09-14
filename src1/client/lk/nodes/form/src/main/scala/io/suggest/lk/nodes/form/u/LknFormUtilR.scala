package io.suggest.lk.nodes.form.u

import io.suggest.common.html.HtmlConstants
import io.suggest.scalaz.NodePath_t

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.03.17 14:49
  * Description: Утиль для react-формы узлов.
  */
object LknFormUtilR {

  def nodePath2treeId(nodePath: NodePath_t): String =
    nodePath.mkString( "", HtmlConstants.`.`, HtmlConstants.DIEZ )

}
