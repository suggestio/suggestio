package io.suggest.mbill2.m.item

import com.google.inject.{Inject, Singleton}
import io.suggest.common.m.sql.ITableName
import io.suggest.common.slick.driver.ExPgSlickDriverT
import io.suggest.mbill2.m.dt.{DateStartSlick, DateEndSlick, IDateEnd, IDateStart}
import io.suggest.mbill2.m.gid.{GidSlick, IGid}
import io.suggest.mbill2.m.item.cols._
import io.suggest.mbill2.m.item.status.{ItemStatusSlick, MItemStatus, IMItemStatus}
import io.suggest.mbill2.m.item.typ.{MItemTypeSlick, MItemType, IMItemType}
import io.suggest.mbill2.m.order.{OrderIdInxSlick, IOrderId, MOrders, OrderIdFkSlick}
import io.suggest.mbill2.m.price._
import io.suggest.mbill2.util.PgaNamesMaker
import io.suggest.model.sc.common.SinkShowLevel
import org.joda.time.DateTime
import slick.lifted.ProvenShape

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 12:06
 * Description: Модель item'ов одного заказа. Это как бы абстрактная модель,
 */

/** DI-контейнер для slick-модели абстрактных item'ов. */
@Singleton
class MItems @Inject() (
  override protected val driver   : ExPgSlickDriverT,
  override protected val mOrders  : MOrders
)
  extends GidSlick
  with PriceSlick
  with CurrencyCodeSlick
  with AmountSlick
  with ITableName
  with OrderIdFkSlick with OrderIdInxSlick
  with ItemStatusSlick
  with MItemTypeSlick
  with DateStartSlick
  with DateEndSlick
  with AdIdSlick
  with ReasonOptSlick
  with RcvrIdOptSlick
  with SlsOptSlick
{

  override val TABLE_NAME = "item"

  import driver.api._

  override def ORDER_ID_INX = PgaNamesMaker.fkInx(TABLE_NAME, ORDER_ID_FN)

  /** Реализация абстрактной slick-таблицы item'ов. */
  class MItemsTable(tag: Tag)
    extends Table[MItem](tag, TABLE_NAME)
    with GidColumn
    with PriceColumn
    with CurrencyCodeColumn
    with CurrencyColumn
    with AmountColumn
    with OrderIdColumn with OrderIdInx
    with OrderIdFk
    with ItemStatusColumn
    with ItemTypeColumn
    with DateStartColumn
    with DateEndColumn
    with AdIdColumn
    with ReasonOptColumn
    with RcvrIdOptColumn
    with SlsColumn
  {

    override def * : ProvenShape[MItem] = {
      (orderId, iType, status, price, adId, dateStart, dateEnd, rcvrIdOpt, sls, reasonOpt, id.?) <> (
        MItem.tupled, MItem.unapply
      )
    }

  }

  val items = TableQuery[MItemsTable]

}


/** Интерфейс экземпляра модели. */
trait IItem
  extends IGid
  with IOrderId
  with IMPrice
  with IMItemType
  with IMItemStatus
  with IDateStart
  with IDateEnd
  with IAdId
  with IReasonOpt
  with IRcvrIdOpt
  with ISls


/** Экземпляр модели (ряда абстрактной таблицы item'ов). */
case class MItem(
  override val orderId      : Long,
  override val iType        : MItemType,
  override val status       : MItemStatus,
  override val price        : MPrice,
  override val adId         : String,
  override val dateStart    : DateTime            = DateTime.now,
  override val dateEnd      : DateTime,
  override val rcvrIdOpt    : Option[String],
  override val sls          : Set[SinkShowLevel]  = Set.empty,
  override val reasonOpt    : Option[String]      = None,
  override val id           : Option[Long]        = None
)
  extends IItem
