package io.suggest.mbill2.m.item

import io.suggest.common.slick.driver.IPgDriver
import io.suggest.mbill2.m.item.cols.INodeId
import io.suggest.mbill2.m.item.status.{MItemStatuses, MItemStatus}
import slick.jdbc.GetResult

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.02.16 17:14
  * Description: Поддержка модели списков item'ов, связанных с рекламной карточкой.
  */
trait MAdItemStatusesSlick extends IPgDriver {

  import driver.api._

  /** Десериализатор Pg RowSet'а c [[MAdItemIds]]. */
  implicit val adItemStatusesGr = GetResult { r =>
    MAdItemStatuses(
      nodeId      = r.nextString(),
      statusesStr = r.<<[Seq[String]]
    )
  }

}


/** Интерфейс модели. */
trait IAdItemStatuses extends INodeId {
  /** Ключи item'ов, связанных с указанной рекламной карточкой. */
  def statusesStr: Seq[String]

  def statuses: Set[MItemStatus] = {
    statusesStr.flatMap(MItemStatuses.maybeWithName).toSet
  }
}


/** Дефолтовая реализация модели [[IAdItemIds]].
  *
  * @param nodeId id рекламной карточки.
  * @param statusesStr Множество строк статусов item'ов карточки.
  */
case class MAdItemStatuses(
  override val nodeId      : String,
  override val statusesStr : Seq[String]
)
  extends IAdItemStatuses
{
  override lazy val statuses = super.statuses
}
