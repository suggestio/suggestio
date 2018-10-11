package io.suggest.mbill2.m.item.cols

import io.suggest.slick.profile.IProfile

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 15:04
 * Description: Аддон для поддержки поля node_id в slick-моделях.
 */
trait NodeIdSlick extends IProfile {

  import profile.api._

  def NODE_ID_FN = "node_id"

  trait NodeIdColumn { that: Table[_] =>
    def nodeId = column[String](NODE_ID_FN)
  }

}
