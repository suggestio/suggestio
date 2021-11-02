package io.suggest.mbill2.m.order

import java.time.OffsetDateTime

import javax.inject.Inject
import io.suggest.common.m.sql.ITableName
import io.suggest.mbill2.m.common.InsertOneReturning
import io.suggest.mbill2.m.contract.{ContractIdSlickFk, ContractIdSlickIdx, FindByContractId, MContracts}
import io.suggest.mbill2.m.dt.{DateCreatedSlick, DateStatusSlick}
import io.suggest.mbill2.m.gid._
import io.suggest.mbill2.util.PgaNamesMaker
import io.suggest.slick.profile.pg.SioPgSlickProfileT
import play.api.inject.Injector
import slick.lifted.ProvenShape

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.12.15 17:02
 * Description: Модель заказов, т.е. ордеров, т.е. групп item'ов.
 */

final class MOrders @Inject() (
                                injector: Injector,
                                override protected val profile      : SioPgSlickProfileT,
                              )
  extends GidSlick
  with DateCreatedSlick
  with ContractIdSlickFk with ContractIdSlickIdx
  with ITableName
  with GetById
  with MultiGetById
  with InsertOneReturning
  with DeleteById
  with DateStatusSlick
  with FindByContractId
{

  import profile.api._

  override protected lazy val mContracts = injector.instanceOf[MContracts]

  override type Table_t = MOrdersTable
  override type El_t    = MOrder

  override def TABLE_NAME   = "order"

  override def CONTRACT_ID_INX = PgaNamesMaker.fkInx(TABLE_NAME, CONTRACT_ID_FN)

  def STATUS_FN             = "status"
  def STATUS_INX            = s"${TABLE_NAME}_${STATUS_FN}_idx"

  /** Slick-описание таблицы заказов. */
  class MOrdersTable(tag: Tag)
    extends Table[MOrder](tag, TABLE_NAME)
    with GidColumn
    with DateCreated
    with ContractIdFk with ContractIdIdx
    with DateStatusColumn
  {

    def statusStr     = column[String](STATUS_FN)
    def status        = statusStr <> (MOrderStatuses.withValue, MOrderStatus.unapplyStrId)
    def statusStrInx  = index(STATUS_INX, statusStr)

    override def * : ProvenShape[MOrder] = {
      (status, contractId, dateCreated, dateStatus, id.?) <> (
        (MOrder.apply _).tupled, MOrder.unapply
      )
    }

  }

  /** Экземпляр статической части модели, пригодный для запуска и проведения запросов. */
  override lazy val query = TableQuery[MOrdersTable]

  /** Апдейт значения экземпляра модели новым id. */
  override protected def _withId(el: MOrder, id: Gid_t): MOrder = {
    el.copy(id = Some(id))
  }


  def saveStatus(morder2: MOrder, now: OffsetDateTime = OffsetDateTime.now()): DBIOAction[Int, NoStream, Effect.Write] =
    saveStatus1( morder2.id.get, morder2.status, now )

  /**
    * Обновить статус ордера вместе с датой статуса.
    *
    * @param id id обновляемого ордера.
    * @param status новый статус ордера.
    * @return Экшен update, возвращающий кол-во обновлённых рядов.
    */
  def saveStatus1(id: Gid_t, status: MOrderStatus, now: OffsetDateTime = OffsetDateTime.now()): DBIOAction[Int, NoStream, Effect.Write] = {
    query
      .filter { _.id === id }
      .map { o => (o.status, o.dateStatus) }
      .update { (status, now) }
  }

  /**
    * Часто читать весь ордер не нужно, а надо только contract_id узнать по id ордера.
    * @param orderId id ордера.
    * @return Опциональный id контракта.
    */
  def getContractId(orderId: Gid_t): DBIOAction[Option[Gid_t], NoStream, Effect.Read] = {
    query
      .filter(_.id === orderId)
      .map(_.contractId)
      .result
      .headOption
  }


  def countByIdStatusContract(ids: Iterable[Gid_t],
                              statuses: Iterable[MOrderStatus] = Nil,
                              contractIds: Iterable[Gid_t] = Nil
                             ): DBIOAction[Int, NoStream, Effect.Read] = {
    var q = if (ids.isEmpty)
      query
    else
      query.filter( _.id inSet ids )
    if (statuses.isEmpty)
      q = q.filter(_.statusStr inSet statuses.map(_.value))
    if (contractIds.nonEmpty)
      q = q.filter(_.contractId inSet contractIds)
    q.size.result
  }

}
