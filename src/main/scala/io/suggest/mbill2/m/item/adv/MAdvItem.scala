package io.suggest.mbill2.m.item.adv

import com.google.inject.{Singleton, Inject}
import io.suggest.common.slick.driver.ExPgSlickDriverT
import io.suggest.mbill2.m.dt.{DateEndSlick, DateStartSlick, IDateEnd, IDateStart}
import io.suggest.mbill2.m.item.adv.cols.{IReasonOpt, ReasonOptSlick, IAdId, AdIdSlick}
import io.suggest.mbill2.m.item.{MItemsBaseT, IItem}
import io.suggest.mbill2.m.item.status.MItemStatus
import io.suggest.mbill2.m.item.typ.MItemType
import io.suggest.mbill2.m.order.MOrders
import io.suggest.mbill2.m.price.MPrice
import org.joda.time.DateTime
import slick.lifted.ProvenShape

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 14:53
 * Description: Модель для работы с абстрактной таблицы заказов размещений карточек.
 */

/** Заготовка для item_adv моделей с целью дедубликации одинаковой логики. */
trait MAdvItemsBaseT
  extends MItemsBaseT
  with DateStartSlick
  with DateEndSlick
  with AdIdSlick
  with ReasonOptSlick
{

  import driver.api._

  override def TABLE_NAME = super.TABLE_NAME + "_adv"

  /** Заготовка slick-таблицы для item_adv-моделей. */
  trait MAdvItemsTableT
    extends MItemsTableT
    with DateStartColumn
    with DateEndColumn
    with AdIdColumn
    with ReasonOptColumn
  { that: Table[_] =>
  }

}


/** Реализация DI-контейнера slick-модели таблицы items_adv. */
@Singleton
class MAdvItems @Inject()(
  override protected val driver   : ExPgSlickDriverT,
  override protected val mOrders  : MOrders
)
  extends MAdvItemsBaseT
{

  import driver.api._

  override val TABLE_NAME = super.TABLE_NAME

  /** Реализация описания slick-таблицы item_adv. */
  class MAdvItemsTable(tag: Tag)
    extends Table[MAdvItem](tag, TABLE_NAME)
    with MAdvItemsTableT
  {
    override def * : ProvenShape[MAdvItem] = {
      (orderId, iType, status, price, adId, dateStart, dateEnd, reasonOpt, id.?) <> (
        MAdvItem.tupled, MAdvItem.unapply
      )
    }
  }

  val itemsAdv = TableQuery[MAdvItemsTable]

}


/** Интерфейс экземпляров этой модели. */
trait IAdvItem
  extends IItem
  with IDateStart
  with IDateEnd
  with IAdId
  with IReasonOpt


/** Экземпляр модели заказов размещений. */
case class MAdvItem(
  override val orderId      : Long,
  override val iType        : MItemType,
  override val status       : MItemStatus,
  override val price        : MPrice,
  override val adId         : String,
  override val dateStart    : DateTime,
  override val dateEnd      : DateTime,
  override val reasonOpt    : Option[String]    = None,
  override val id           : Option[Long]      = None
)
  extends IAdvItem
