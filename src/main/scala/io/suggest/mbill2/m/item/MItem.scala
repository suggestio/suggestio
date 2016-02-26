package io.suggest.mbill2.m.item

import com.google.inject.{Inject, Singleton}
import io.suggest.common.m.sql.ITableName
import io.suggest.common.slick.driver.ExPgSlickDriverT
import io.suggest.mbill2.m.common.{InsertManyReturning, InsertOneReturning}
import io.suggest.mbill2.m.dt._
import io.suggest.mbill2.m.geo.shape.{IGeoShapeOpt, GeoShapeOptSlick}
import io.suggest.mbill2.m.gid._
import io.suggest.mbill2.m.item.cols._
import io.suggest.mbill2.m.item.status.{MItemStatuses, ItemStatusSlick, MItemStatus, IMItemStatus}
import io.suggest.mbill2.m.item.typ.{MItemTypeSlick, MItemType, IMItemType}
import io.suggest.mbill2.m.order._
import io.suggest.mbill2.m.price._
import io.suggest.mbill2.m.tags.{TagFaceOptSlick, ITagFaceOpt}
import io.suggest.mbill2.util.PgaNamesMaker
import io.suggest.model.geo.GeoShape
import io.suggest.model.sc.common.SinkShowLevel
import org.joda.time.{DateTime, Interval}
import slick.jdbc.SQLActionBuilder
import slick.lifted.ProvenShape
import slick.profile.SqlAction

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
  with MultiGetById
  with InsertOneReturning with InsertManyReturning
  with DeleteById
  with GeoShapeOptSlick
  with TagFaceOptSlick
  with DateStatusSlick
  with MAdItemIdsSlick
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
    *
    * @param itm2 Обновлённый инстанс item'а.
    * @return Экшен UPDATE.
    */
  def updateStatus(itm2: MItem) = {
    updateStatus1(itm2.status, itm2.id.get)
  }
  def updateStatus1(status: MItemStatus, ids: Gid_t*) = {
    updateStatus2(status, ids)
  }
  def updateStatus2(status: MItemStatus, ids: Traversable[Gid_t]) = {
    if (ids.isEmpty) {
      throw new IllegalArgumentException("ids must be non-empty")
    } else {
      query
        .filter { _.id inSet ids }
        .map { i => (i.status, i.dateStatus) }
        .update((status, DateTime.now()))
    }
  }

  /** Поиск рядов, относящихся как-то текущей дате. */
  def findCurrent(now: DateTime = DateTime.now) = {
    query.filter { mitem =>
      mitem.dateStartOpt >= now &&
      mitem.dateEndOpt < now
    }
  }


  /** Поиск id текущих размещений по ad_id.
    *
    * @param status Статус оных.
    * @return Список ad_id -> Seq[id].
    */
  def findCurrentForStatus(status: MItemStatus, expired: Boolean) = {
    // TODO В slick никак не осилят custom aggregate functions. https://github.com/slick/slick/pull/796
    val dtFn = if (!expired) DATE_START_FN else DATE_END_FN
    // Поэтому тот plain SQL вместо использования lifted API.
    sql"SELECT #$AD_ID_FN, array_agg(#$ID_FN) FROM #$TABLE_NAME WHERE #$STATUS_FN = ${status.strId} AND now() >= #$dtFn GROUP BY #$AD_ID_FN"
      .as[MAdItemIds]
  }


  /** Экшен считывания ожидающего item'а с целью его обновления, который 100% существует.
    * Метод используется внутри транзакций модерации item'ов.
    *
    * @param itemId id запрашиваемого item'а.
    * @return Опциональный экземляр [[MItem]].
    */
  def getByIdStatusAction(itemId: Gid_t, status: MItemStatus): SqlAction[Option[MItem], NoStream, Effect.Read] = {
    query
      // Явно запрещаем получать item без ожидаемого статуса
      .filter { i =>
        (i.id === itemId) && (i.statusStr === status.strId)
      }
      .result
      .headOption
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
