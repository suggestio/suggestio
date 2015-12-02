package io.suggest.mbill2.m.order

import com.google.inject.{Inject, Singleton}
import io.suggest.common.m.sql.ITableName
import io.suggest.common.slick.driver.ExPgSlickDriverT
import io.suggest.mbill2.m.contract.{ContractIdSlickIdx, ContractIdSlickFk, MContracts}
import io.suggest.mbill2.m.dt.DateCreatedSlick
import io.suggest.mbill2.m.gid.GidSlick
import io.suggest.mbill2.m.price._
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
  extends PriceSlick
  with GidSlick
  with DateCreatedSlick
  with ContractIdSlickFk with ContractIdSlickIdx
  with AmountSlick
  with CurrencyCodeSlick
  with ITableName
{

  import driver.api._

  override val TABLE_NAME   = "order"

  override def CONTRACT_ID_INX = PgaNamesMaker.fkInx(TABLE_NAME, CONTRACT_ID_FN)

  def STATUS_FN             = "status"
  def DATE_STATUS_FN        = "date_status"

  def STATUS_INX            = s"${TABLE_NAME}_${STATUS_FN}_idx"

  /** Slick-описание таблицы заказов. */
  class MOrdersTable(tag: Tag)
    extends Table[MOrder](tag, TABLE_NAME)
    with PriceColumn with CurrencyColumn
    with GidColumn
    with DateCreatedColumn
    with ContractIdFk with ContractIdIdx
    with AmountColumn
    with CurrencyCodeColumn
  {

    def statusStr     = column[String](STATUS_FN)
    def dateStatus    = column[DateTime](DATE_STATUS_FN)

    def statusStrInx  = index(STATUS_INX, statusStr)

    def status = statusStr <> (MOrderStatuses.withNameT, MOrderStatuses.unapply)

    override def * : ProvenShape[MOrder] = {
      (status, contractId, price, dateCreated, dateStatus, id.?) <> (
        MOrder.tupled, MOrder.unapply
      )
    }

  }

  /** Экземпляр статической части модели, пригодный для запуска и проведения запросов. */
  val orders = TableQuery[MOrdersTable]

}


case class MOrder(
  status        : MOrderStatus,
  contractId    : Long,
  price         : MPrice,
  dateCreated   : DateTime      = DateTime.now,
  dateStatus    : DateTime      = DateTime.now,
  id            : Option[Long]  = None
)
