package io.suggest.mbill2.m.item

import com.google.inject.{Inject, Singleton}
import io.suggest.common.m.sql.ITableName
import io.suggest.common.slick.driver.ExPgSlickDriverT
import io.suggest.mbill2.m.common.InsertOneReturning
import io.suggest.mbill2.m.dt._
import io.suggest.mbill2.m.gid._
import io.suggest.mbill2.m.item.cols._
import io.suggest.mbill2.m.item.status.{ItemStatusSlick, MItemStatus, IMItemStatus}
import io.suggest.mbill2.m.item.typ.{MItemTypeSlick, MItemType, IMItemType}
import io.suggest.mbill2.m.order.{OrderIdInxSlick, IOrderId, MOrders, OrderIdFkSlick}
import io.suggest.mbill2.m.price._
import io.suggest.mbill2.util.PgaNamesMaker
import io.suggest.model.sc.common.SinkShowLevel
import org.joda.time.Interval
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
  override val mOrders            : MOrders
)
  extends GidSlick
  with PriceSlick
  with CurrencyCodeSlick
  with AmountSlick
  with ITableName
  with OrderIdFkSlick with OrderIdInxSlick
  with ItemStatusSlick
  with MItemTypeSlick
  with IntervalSlick
  with AdIdSlick
  with ReasonOptSlick
  with RcvrIdOptSlick
  with SlsOptSlick
  with GetById
  with InsertOneReturning
  with DeleteById
{

  override val TABLE_NAME = "item"

  import driver.api._

  override type Table_t = MItemsTable
  override type El_t    = MItem

  def PROD_ID_FN        = "prodId"
  def PROD_ID_INX       = PgaNamesMaker.inx(TABLE_NAME, PROD_ID_FN)

  /** Реализация абстрактной slick-таблицы item'ов. */
  class MItemsTable(tag: Tag)
    extends Table[MItem](tag, TABLE_NAME)
    with GidColumn
    with PriceColumn
    with CurrencyCodeColumn
    with CurrencyColumn
    with AmountColumn
    with OrderIdFk with OrderIdInx
    with ItemStatusColumn
    with ItemTypeColumn
    with IntervalColumn
    with AdIdColumn
    with ReasonOptColumn
    with RcvrIdOptColumn
    with SlsColumn
  {

    def prodId          = column[String](PROD_ID_FN)
    def prodIdInx       = index(PROD_ID_INX, prodId)

    override def * : ProvenShape[MItem] = {
      (orderId, iType, status, price, adId, prodId, dtIntervalOpt, rcvrIdOpt, sls, reasonOpt, id.?) <> (
        MItem.tupled, MItem.unapply
      )
    }

  }

  override val query = TableQuery[MItemsTable]

  /** Апдейт значения экземпляра модели новым id. */
  override protected def _withId(el: MItem, id: Gid_t): MItem = {
    el.copy(id = Some(id))
  }

}


/** Интерфейс экземпляра модели. */
trait IItem
  extends IGid
  with IOrderId
  with IMPrice
  with IMItemType
  with IMItemStatus
  with IDtIntervalOpt
  with IAdId
  with IReasonOpt
  with IRcvrIdOpt
  with ISls
{
  def prodId          : String
}

/** Экземпляр модели (ряда абстрактной таблицы item'ов). */
case class MItem(
  override val orderId        : Gid_t,
  override val iType          : MItemType,
  override val status         : MItemStatus,
  override val price          : MPrice,
  override val adId           : String,
  override val prodId         : String,
  override val dtIntervalOpt  : Option[Interval],
  override val rcvrIdOpt      : Option[String],
  override val sls            : Set[SinkShowLevel]  = Set.empty,
  override val reasonOpt      : Option[String]      = None,
  override val id             : Option[Gid_t]       = None
)
  extends IItem

