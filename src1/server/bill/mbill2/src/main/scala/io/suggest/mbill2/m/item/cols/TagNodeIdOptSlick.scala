package io.suggest.mbill2.m.item.cols

import io.suggest.slick.profile.IProfile

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.12.17 11:05
  * Description: Трейт поддержки поля с id тега, в котором происходит тег-размещение карточки.
  */
trait TagNodeIdOptSlick extends IProfile {

  import profile.api._

  def TAG_NODE_ID_FN = "tag_node_id"

  trait TagNodeIdOptColumn { that: Table[_] =>
    def tagNodeIdOpt = column[Option[String]]( TAG_NODE_ID_FN )
  }

}
