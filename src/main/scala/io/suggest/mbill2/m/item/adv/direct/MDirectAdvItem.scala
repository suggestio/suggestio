package io.suggest.mbill2.m.item.adv.direct

import com.google.inject.Inject
import io.suggest.common.slick.driver.ExPgSlickDriverT
import io.suggest.mbill2.m.item.adv.cols.{SlsSlick, RcvrIdSlick, ISls, IRcvrId}
import io.suggest.mbill2.m.item.adv.{IAdvItem, MAdvItemsBaseT}
import io.suggest.mbill2.m.item.status.MItemStatus
import io.suggest.mbill2.m.item.typ.MItemType
import io.suggest.mbill2.m.order.MOrders
import io.suggest.mbill2.m.price.MPrice
import io.suggest.model.sc.common.SinkShowLevel
import org.joda.time.DateTime
import slick.lifted.ProvenShape

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 15:25
 * Description: slick-модель для прямого размещения карточек на узлах.
 */
@Inject
class MDirectAdvItems(
  override protected val driver   : ExPgSlickDriverT,
  override protected val mOrders  : MOrders
)
  extends MAdvItemsBaseT
  with RcvrIdSlick
  with SlsSlick
{

  import driver.api._

  override val TABLE_NAME = super.TABLE_NAME + "_direct"

  class MDirectAdvItemsTable(tag: Tag)
    extends Table[MDirectAdvItem](tag, TABLE_NAME)
    with MAdvItemsTableT
    with RcvrIdColumn
    with SlsColumn
  {
    override def * : ProvenShape[MDirectAdvItem] = {
      (orderId, iType, status, price, adId, dateStart, dateEnd, rcvrId, sls, reasonOpt, id.?) <> (
        MDirectAdvItem.tupled, MDirectAdvItem.unapply
      )
    }
  }

  val directAdvItems = TableQuery[MDirectAdvItemsTable]

}



/** Интерфейс экземпляра модели item_adv_direct. */
trait IDirectAdvItem
  extends IAdvItem
  with IRcvrId
  with ISls


/** Класс экземпляра модели item_adv_direct. */
case class MDirectAdvItem(
  override val orderId      : Long,
  override val iType        : MItemType,
  override val status       : MItemStatus,
  override val price        : MPrice,
  override val adId         : String,
  override val dateStart    : DateTime,
  override val dateEnd      : DateTime,
  override val rcvrId       : String,
  override val sls          : Set[SinkShowLevel],
  override val reasonOpt    : Option[String]      = None,
  override val id           : Option[Long]        = None
)
  extends IDirectAdvItem
