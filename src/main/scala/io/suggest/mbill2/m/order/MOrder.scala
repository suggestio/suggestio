package io.suggest.mbill2.m.order

import com.google.inject.{Inject, Singleton}
import io.suggest.common.m.sql.ITableName
import io.suggest.common.slick.driver.ExPgSlickDriverT
import io.suggest.mbill2.m.common.InsertOneReturning
import io.suggest.mbill2.m.contract.{ContractIdSlickIdx, ContractIdSlickFk, MContracts}
import io.suggest.mbill2.m.dt.{DateStatusSlick, DateCreatedSlick}
import io.suggest.mbill2.m.gid._
import io.suggest.mbill2.util.PgaNamesMaker
import org.joda.time.DateTime
import slick.lifted.ProvenShape

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.12.15 17:02
 * Description: Модель заказов, т.е. ордеров, т.е. групп item'ов.
 */

@Singleton
class MOrders @Inject() (
  override protected val driver       : ExPgSlickDriverT,
  override protected val mContracts   : MContracts
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
{

  import driver.api._

  override type Table_t = MOrdersTable
  override type El_t    = MOrder

  override val TABLE_NAME   = "order"

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
    def status        = statusStr <> (MOrderStatuses.withNameT, MOrderStatuses.unapply)
    def statusStrInx  = index(STATUS_INX, statusStr)

    override def * : ProvenShape[MOrder] = {
      (status, contractId, dateCreated, dateStatus, id.?) <> (
        MOrder.tupled, MOrder.unapply
      )
    }

  }

  /** Экземпляр статической части модели, пригодный для запуска и проведения запросов. */
  val query = TableQuery[MOrdersTable]

  /** Апдейт значения экземпляра модели новым id. */
  override protected def _withId(el: MOrder, id: Gid_t): MOrder = {
    el.copy(id = Some(id))
  }


  def saveStatus(morder2: MOrder) = {
    saveStatus1(morder2.id.get, morder2.status)
  }
  /**
    * Обновить статус ордера вместе с датой статуса.
    *
    * @param id id обновляемого ордера.
    * @param status новый статус ордера.
    * @return Экшен update, возвращающий кол-во обновлённых рядов.
    */
  def saveStatus1(id: Gid_t, status: MOrderStatus) = {
    query
      .filter { _.id === id }
      .map { o => (o.status, o.dateStatus) }
      .update { (status, DateTime.now()) }
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

}


case class MOrder(
  status        : MOrderStatus,
  contractId    : Gid_t,
  dateCreated   : DateTime      = DateTime.now,
  dateStatus    : DateTime      = DateTime.now,
  id            : Option[Gid_t] = None
)
  extends IGid
{

  def withStatus(status1: MOrderStatus): MOrder = {
    copy(
      status      = status1,
      dateStatus  = DateTime.now()
    )
  }

}
