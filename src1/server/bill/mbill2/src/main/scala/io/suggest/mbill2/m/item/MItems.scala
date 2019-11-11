package io.suggest.mbill2.m.item

import java.time.OffsetDateTime

import javax.inject.{Inject, Singleton}
import io.suggest.common.m.sql.ITableName
import io.suggest.mbill2.m.common.{InsertManyReturning, InsertOneReturning}
import io.suggest.mbill2.m.dt._
import io.suggest.mbill2.m.geo.shape.GeoShapeOptSlick
import io.suggest.mbill2.m.gid._
import io.suggest.mbill2.m.item.cols._
import io.suggest.mbill2.m.item.status.{ItemStatusSlick, MItemStatus, MItemStatuses}
import io.suggest.mbill2.m.item.typ.{MItemType, MItemTypeSlick}
import io.suggest.mbill2.m.order._
import io.suggest.mbill2.m.price._
import io.suggest.mbill2.m.tags.TagFaceOptSlick
import io.suggest.slick.profile.pg.SioPgSlickProfileT
import org.threeten.extra.Interval
import slick.lifted.ProvenShape
import slick.sql.SqlAction
import io.suggest.enum2.EnumeratumUtil.ValueEnumEntriesOps

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 12:06
 * Description: Модель item'ов одного заказа. Это как бы абстрактная модель,
 */

/** DI-контейнер для slick-модели абстрактных item'ов. */
@Singleton
class MItems @Inject() (
                         override protected val profile  : SioPgSlickProfileT,
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
  with DateStartSlick
  with DateEndSlick
  with NodeIdSlick
  with ReasonOptSlick
  with RcvrIdOptSlick
  with GetById
  with MultiGetById
  with InsertOneReturning with InsertManyReturning
  with DeleteById
  with GeoShapeOptSlick
  with TagFaceOptSlick
  with DateStatusSlick
  with MAdItemIdsSlick
  with MAdItemStatusesSlick
  with TagNodeIdOptSlick
{

  import profile.api._

  /** Алиас типа для абстрактной query. */
  type Query0 = Query[MItems#MItemsTable, MItem, Seq]


  override val TABLE_NAME = "item"

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
    with DateStartOpt
    with DateEndOpt
    with NodeIdColumn
    with ReasonOptColumn
    with RcvrIdOptColumn
    with GeoShapeOptColumn
    with TagFaceOptColumn
    with DateStatusColumn
    with TagNodeIdOptColumn
  {

    override def * : ProvenShape[MItem] = {
      (orderId, iType, status, price, nodeId, dateStartOpt, dateEndOpt, rcvrIdOpt, reasonOpt, geoShapeOpt,
        tagFaceOpt, tagNodeIdOpt, dateStatus, id.?) <> (
        (MItem.apply _).tupled, MItem.unapply
      )
    }

    // DSL для быстрой сборки query.filter(...)
    def withIds(ids: Gid_t*): Rep[Boolean] =
      withIds1( ids )
    def withIds1(ids: Iterable[Gid_t]): Rep[Boolean] =
      id inSet ids
    def withNodeId(nodeIds: String*): Rep[Boolean] =
      withNodeIds(nodeIds)
    def withNodeIds(nodeIds: Iterable[String]): Rep[Boolean] =
      nodeId.inSet( nodeIds )
    def withTypes1(types: MItemType*): Rep[Boolean] =
      withTypes(types)
    def withTypes(types: IterableOnce[MItemType]): Rep[Boolean] =
      iTypeStr.inSet( types.iterator.onlyIds.to(Iterable) )
    def withStatus(status1: MItemStatus): Rep[Boolean] =
      statusStr === status1.value
    def withStatuses(statuses: IterableOnce[MItemStatus]): Rep[Boolean] =
      statusStr.inSet( statuses.iterator.onlyIds.to(Iterable) )
    def withRcvrIds(rcvrIds: Iterable[String]): Rep[Option[Boolean]] =
      rcvrIdOpt.inSet( rcvrIds )
    def withOrderId(orderIds: Gid_t*): Rep[Boolean] =
      withOrderIds(orderIds)
    def withOrderIds(orderIds: Iterable[Gid_t]): Rep[Boolean] =
      orderId inSet orderIds

  }

  object MItemsTable {

    /** Код отсюда доступен для query напрямую без дополнительных import'ов
      * TODO Не везде срабатывает - требуются импорты.
      */
    implicit class MItemsTableSeqQueryExtOps[T <: Query0](q: T) {

      def itemsCurrentFor(now: OffsetDateTime = OffsetDateTime.now()) = {
        q.filter { mitem =>
          mitem.dateStartOpt <= now &&
          mitem.dateEndOpt >= now
        }
      }

      def withId(ids: Gid_t*) =
        withIds(ids)
      def withIds(ids: Iterable[Gid_t]) =
        q.filter { _.id inSet ids }

      def withNodeId(nodeIds: String*) =
        withNodeIds(nodeIds)
      def withNodeIds(nodeIds: Iterable[String])  =
        q.filter { _.nodeId inSet nodeIds }

      def withType(itypes: MItemType*) =
        withTypes(itypes)
      def withTypes(itypes: IterableOnce[MItemType]) =
        q.filter { _.withTypes(itypes) }

      def withStatus(statuses: MItemStatus*) =
        withStatuses(statuses)
      def withStatuses(statuses: IterableOnce[MItemStatus]) =
        q.filter { _.withStatuses(statuses) }

      def withOrderId(orderIds: Gid_t*) =
        withOrderIds( orderIds )
      def withOrderIds(orderIds: Iterable[Gid_t]) =
        q.filter { _.withOrderIds(orderIds) }

      def withRcvrId(rcvrIds: String*) =
        withRcvrIds(rcvrIds)
      def withRcvrIds(rcvrIds: Iterable[String]) =
        q.filter { _.withRcvrIds(rcvrIds) }

    }

  }


  override val query = TableQuery[MItemsTable]

  import MItemsTable._

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
  def updateStatus2(status: MItemStatus, ids: Iterable[Gid_t]) = {
    if (ids.isEmpty) {
      throw new IllegalArgumentException("ids must be non-empty")
    } else {
      query
        .withIds( ids )
        .map { i => (i.status, i.dateStatus) }
        .update((status, OffsetDateTime.now))
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
      .withId(itemId)
      .withStatus(status)
      .result
      .headOption
  }

  /** Concat списка элементов в строку, содержащую sql-список строк для отправки в IN (...). */
  private def _mkSqlInString(es: Iterator[Any]): String =
    es.mkString("'", "','", "'")

  /**
    * Для списка перечисленных карточек найти вернуть статусы из множества интересующих.
    *
    * @param adIds Интересующие карточки.
    * @param statuses Интересующие статусы item'ов или все возможные.
    * @return Пары adId -> [[io.suggest.mbill2.m.item.MAdItemStatuses]].
    */
  def findStatusesForAds(adIds: Iterable[String], statuses: Iterable[MItemStatus]) = {
    // TODO Sec Возможность SQL injection, нужно передавать список через args, но slick sql не умеет IN (...) синтаксис.
    // Возможно, стоит попробовать эту пионерскую поделку https://github.com/tarao/slick-jdbc-extension-scala
    val adIdsStr = _mkSqlInString( adIds.iterator )
    val statusesStr = _mkSqlInString( statuses.onlyIds )
    sql"SELECT #$NODE_ID_FN, array_agg(DISTINCT #$STATUS_FN) FROM #$TABLE_NAME WHERE #$NODE_ID_FN IN (#$adIdsStr) AND #$STATUS_FN IN (#$statusesStr) GROUP BY #$NODE_ID_FN"
      .as[MAdItemStatuses]
  }

  def countByIdStatus(itemIds: Iterable[Gid_t], statuses: Iterable[MItemStatus] = Nil): DBIOAction[Int, NoStream, Effect.Read] = {
    var q0: Query0 = query
    if (itemIds.nonEmpty)
      q0 = q0.withIds( itemIds )
    if (statuses.nonEmpty)
      q0 = q0.withStatuses( statuses )
    q0.size
      .result
  }

  def getOrderIds(itemIds: Iterable[Gid_t]): DBIOAction[Seq[Gid_t], Streaming[Gid_t], Effect.Read] = {
    query
      .withIds(itemIds)
      .map(_.orderId)
      .distinct
      .result
  }


  /**
    * Безвозвратно стереть все item'ы для указанного id заказа.
    *
    * @param orderId Номер заказа.
    * @return Кол-во удалённых рядов.
    */
  def deleteByOrderId(orderId: Gid_t): DBIOAction[Int, NoStream, Effect.Write] = {
    query
      .withOrderId(orderId)
      .delete
  }

  /** Найти item'ы с таким же гео-шейпом, как у указанного item'а.
    *
    * @param query0 Какой-то исходный запрос item'ов.
    * @param itemId id item'а, содержащего необходимый шейп.
    *
    * @return Query.
    */
  def withSameGeoShapeAs(itemId: Gid_t, query0: Query0 = query): Query0 = {
    query0
      .filter { i =>
        val itemShapeQ = query
          .filter(_.id === itemId)
          .map(_.geoShapeStrOpt)
          .filter(_.isDefined)
        i.geoShapeStrOpt in itemShapeQ
      }
  }


  /** Сборка query для поиска текущих item'ов карточки. */
  def findCurrentForNode(nodeId: String, itypes: Iterable[MItemType], query0: Query0 = query): Query0 = {
    var q2 = query0
      .withNodeId( nodeId )
      .withStatuses( MItemStatuses.advBusy )
    // Если допустимые типы item'ов перечислены, то добавить соотв.SQL:
    if (itypes.nonEmpty)
      q2 = q2.withTypes( itypes )

    q2
  }


  /** Подсчёт item'ов в указанном ордере. */
  def countByOrderId(orderId: Gid_t): DBIOAction[Int, NoStream, Effect.Read] = {
    query
      .withOrderId( orderId )
      .size
      .result
  }

}


/** jvm-only утиль для [[MItem]]-модели. */
object MItemJvm {

  object Implicits {

    implicit class MItemJvmOpsExt(val o: MItem) extends AnyVal {

      def dtIntervalOpt: Option[Interval] = {
        for (start <- o.dateStartOpt; end <- o.dateEndOpt) yield {
          Interval.of(start.toInstant, end.toInstant)
        }
      }

    }

  }

}


/** Интерфейс для поля, содержащего [[MItem]]. */
trait IMItem {
  def mitem: MItem
}
