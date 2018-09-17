package io.suggest.mbill2.m.item

import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.slick.profile.pg.IPgProfile
import slick.jdbc.GetResult

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.02.16 17:14
  * Description: Поддержка модели списков item'ов, связанных с рекламной карточкой.
  */
trait MAdItemIdsSlick extends IPgProfile {

  import profile.api._

  /** Десериализатор Pg RowSet'а c [[MAdItemIds]]. */
  implicit val adItemIdsGr = GetResult { r =>
    MAdItemIds(
      nodeId  = r.nextString(),
      itemIds = r.<<[Seq[Gid_t]]
    )
  }

}


/**
  * @param nodeId id рекламной карточки.
  * @param itemIds Ключи item'ов.
  */
case class MAdItemIds(
                       nodeId   : String,
                       itemIds  : Seq[Gid_t]
                     )
