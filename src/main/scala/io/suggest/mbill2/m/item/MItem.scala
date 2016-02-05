package io.suggest.mbill2.m.item

import com.google.inject.{Inject, Singleton}
import io.suggest.common.m.sql.ITableName
import io.suggest.common.slick.driver.ExPgSlickDriverT
import io.suggest.mbill2.m.common.InsertOneReturning
import io.suggest.mbill2.m.dt._
import io.suggest.mbill2.m.geo.shape.{IGeoShapeOpt, GeoShapeOptSlick}
import io.suggest.mbill2.m.gid._
import io.suggest.mbill2.m.item.cols._
import io.suggest.mbill2.m.item.status.{ItemStatusSlick, MItemStatus, IMItemStatus}
import io.suggest.mbill2.m.item.typ.{MItemTypeSlick, MItemType, IMItemType}
import io.suggest.mbill2.m.order._
import io.suggest.mbill2.m.price._
import io.suggest.mbill2.m.tags.{TagFaceOptSlick, ITagFaceOpt}
import io.suggest.mbill2.util.PgaNamesMaker
import io.suggest.model.geo.GeoShape
import io.suggest.model.sc.common.SinkShowLevel
import org.joda.time.{DateTime, Interval}
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
  with FindByOrderId
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
  with GeoShapeOptSlick
  with TagFaceOptSlick
  with DateStatusSlick
{

  override val TABLE_NAME = "item"

  import driver.api._

  override type Table_t = MItemsTable
  override type El_t    = MItem

  def PROD_ID_FN        = "prod_id"
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
    with GeoShapeOptColumn
    with TagFaceOptColumn
    with DateStatusColumn
  {

    def prodId          = column[String](PROD_ID_FN)
    def prodIdInx       = index(PROD_ID_INX, prodId)

    override def * : ProvenShape[MItem] = {
      (orderId, iType, status, price, adId, prodId, dtIntervalOpt, rcvrIdOpt, sls, reasonOpt, geoShapeOpt, tagFaceOpt,
        dateStatus, id.?) <> (
        MItem.tupled, MItem.unapply
      )
    }

  }

  override val query = TableQuery[MItemsTable]

  /** Апдейт значения экземпляра модели новым id. */
  override protected def _withId(el: MItem, id: Gid_t): MItem = {
    el.copy(id = Some(id))
  }


  /**
    * Выставить статус для указанного item'а.
    * @param itm2 Обновлённый инстанс item'а.
    * @return Экшен UPDATE.
    */
  def saveStatus(itm2: MItem) = {
    saveStatus1(itm2.id.get, itm2.status)
  }
  def saveStatus1(id: Gid_t, status: MItemStatus) = {
    query
      .filter { _.id === id }
      .map { i => (i.status, i.dateStatus) }
      .update( (status, DateTime.now()) )
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
  with IGeoShapeOpt
  with ITagFaceOpt
  with IDateStatus
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
  override val geoShape       : Option[GeoShape]    = None,
  override val tagFaceOpt     : Option[String]      = None,
  override val dateStatus     : DateTime            = DateTime.now(),
  override val id             : Option[Gid_t]       = None
)
  extends IItem
{

  /** @return Инстанс [[MItem]] с новым статусом и датой обновления оного. */
  def withStatus(status1: MItemStatus): MItem = {
    copy(
      status      = iType.orderClosedStatus,
      dateStatus  = DateTime.now()
    )
  }

}
