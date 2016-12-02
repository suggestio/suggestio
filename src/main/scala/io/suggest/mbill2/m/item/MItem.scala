package io.suggest.mbill2.m.item

import com.google.inject.{Inject, Singleton}
import io.suggest.common.m.sql.ITableName
import io.suggest.common.slick.driver.ExPgSlickDriverT
import io.suggest.mbill2.m.common.{InsertManyReturning, InsertOneReturning}
import io.suggest.mbill2.m.dt._
import io.suggest.mbill2.m.geo.shape.{GeoShapeOptSlick, IGeoShapeOpt}
import io.suggest.mbill2.m.gid._
import io.suggest.mbill2.m.item.cols._
import io.suggest.mbill2.m.item.status.{IMItemStatus, ItemStatusSlick, MItemStatus, MItemStatuses}
import io.suggest.mbill2.m.item.typ.{IMItemType, MItemType, MItemTypeSlick, MItemTypes}
import io.suggest.mbill2.m.order._
import io.suggest.mbill2.m.price._
import io.suggest.mbill2.m.tags.{ITagFaceOpt, TagFaceOptSlick}
import io.suggest.model.geo.GeoShape
import io.suggest.model.sc.common.SinkShowLevel
import org.joda.time.{DateTime, Interval}
import slick.lifted.ProvenShape
import slick.profile.SqlAction

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 12:06
 * Description: Модель item'ов одного заказа. Это как бы абстрактная модель,
 */

// TODO По таблице items:
// - Переименовать столбец ad_id в node_id
// - Обновить коммент к столбцу node_id.
// - Удалить столбец sls.

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
  with NodeIdSlick
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
  with MAdItemStatusesSlick
{

  override val TABLE_NAME = "item"

  import driver.api._

  override type Table_t = MItemsTable
  override type El_t    = MItem

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
    with NodeIdColumn
    with ReasonOptColumn
    with RcvrIdOptColumn
    with SlsColumn
    with GeoShapeOptColumn
    with TagFaceOptColumn
    with DateStatusColumn
  {

    override def * : ProvenShape[MItem] = {
      (orderId, iType, status, price, nodeId, dtIntervalOpt, rcvrIdOpt, sls, reasonOpt, geoShapeOpt, tagFaceOpt,
        dateStatus, id.?) <> (
        MItem.tupled, MItem.unapply
      )
    }

    // DSL для быстрой сборки query.filter(...)
    def withNodeId(nodeIds: String*): Rep[Boolean] = {
      nodeId.inSet( nodeIds )
    }
    def withTypes(types: TraversableOnce[MItemType]): Rep[Boolean] = {
      iTypeStr.inSet( MItemTypes.onlyIds(types).toTraversable )
    }
    def withStatus(status1: MItemStatus): Rep[Boolean] = {
      statusStr === status1.strId
    }
    def withStatuses(statuses: TraversableOnce[MItemStatus]): Rep[Boolean] = {
      statusStr.inSet( MItemStatuses.onlyIds(statuses).toTraversable )
    }
    def withRcvrs(rcvrIds: Traversable[String]): Rep[Option[Boolean]] = {
      rcvrIdOpt.inSet( rcvrIds )
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

  /** Concat списка элементов в строку, содержащую sql-список строк для отправки в IN (...). */
  private def _mkSqlInString(es: TraversableOnce[Any]): String = {
    es.mkString("'", "','", "'")
  }

  /**
    * Для списка перечисленных карточек найти вернуть статусы из множества интересующих.
    *
    * @param adIds Интересующие карточки.
    * @param statuses Интересующие статусы item'ов или все возможные.
    * @return Пары adId -> [[io.suggest.mbill2.m.item.MAdItemStatuses]].
    */
  def findStatusesForAds(adIds: Traversable[String], statuses: Traversable[MItemStatus] = MItemStatuses.valuesT) = {
    // TODO Sec Возможность SQL injection, нужно передавать список через args, но slick sql не умеет IN (...) синтаксис.
    // Возможно, стоит попробовать эту пионерскую поделку https://github.com/tarao/slick-jdbc-extension-scala
    val adIdsStr = _mkSqlInString( adIds )
    val statusesStr = _mkSqlInString( statuses.toIterator.map(_.strId) )
    sql"SELECT #$NODE_ID_FN, array_agg(DISTINCT #$STATUS_FN) FROM #$TABLE_NAME WHERE ad_id IN (#$adIdsStr) AND #$STATUS_FN IN (#$statusesStr) GROUP BY ad_id"
      .as[MAdItemStatuses]
  }

  /**
    * Безвозвратно стереть все item'ы для указанного id заказа.
    *
    * @param orderId Номер заказа.
    * @return Кол-во удалённых рядов.
    */
  def deleteByOrderId(orderId: Gid_t): DBIOAction[Int, NoStream, Effect.Write] = {
    query
      .filter(_.orderId === orderId)
      .delete
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
  with INodeId
  with IReasonOpt
  with IRcvrIdOpt
  with ISls
  with IGeoShapeOpt
  with ITagFaceOpt
  with IDateStatus


/** Экземпляр модели (ряда абстрактной таблицы item'ов). */
case class MItem(
  override val orderId        : Gid_t,
  override val iType          : MItemType,
  override val status         : MItemStatus,
  override val price          : MPrice,
  override val nodeId         : String,
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


/** Интерфейс для поля, содержащего [[MItem]]. */
trait IMItem {
  def mitem: MItem
}
