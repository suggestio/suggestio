package io.suggest.mbill2.m.item

import io.suggest.mbill2.m.item.cols.INodeId
import io.suggest.mbill2.m.item.status.{MItemStatus, MItemStatuses}
import io.suggest.slick.profile.pg.IPgProfile
import slick.jdbc.GetResult

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.02.16 17:14
  * Description: Поддержка модели списков item'ов, связанных с рекламной карточкой.
  */
trait MAdItemStatusesSlick extends IPgProfile {

  import profile.api._

  /** Десериализатор Pg RowSet'а c [[MAdItemIds]]. */
  implicit val adItemStatusesGr = GetResult { r =>
    MAdItemStatuses(
      nodeId      = r.nextString(),
      statusesStr = r.<<[Seq[String]]
    )
  }

}


/** Дефолтовая реализация модели [[IAdItemIds]].
  *
  * @param nodeId id рекламной карточки.
  * @param statusesStr Множество строк статусов item'ов карточки.
  *                    Ключи item'ов, связанных с указанной рекламной карточкой.
  */
case class MAdItemStatuses(
                            override val nodeId   : String,
                            statusesStr           : Seq[String]
                          )
  extends INodeId
{

  lazy val statuses: Set[MItemStatus] = {
    statusesStr.flatMap(MItemStatuses.withValueOpt).toSet
  }

}
