package io.suggest.mbill2.m.order

import com.google.inject.{Inject, Singleton}
import io.suggest.common.m.sql.ITableName
import io.suggest.common.slick.driver.ExPgSlickDriverT
import io.suggest.mbill2.m.contract.MContracts
import io.suggest.mbill2.m.price.{MPrice, PriceSlick, Amount_t}
import org.joda.time.DateTime
import slick.lifted.ProvenShape

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.12.15 17:02
 * Description: Модель заказов, т.е. ордеров, т.е. групп item'ов.
 */
object MOrder extends ITableName {

  override val TABLE_NAME   = "order"

  val ID_FN                 = "id"
  val STATUS_FN             = "status"
  val AMOUNT_FN             = "amount"
  val CURRENCY_CODE_FN      = "currency_code"
  val CONTRACT_ID_FN        = "contract_id"
  val DATE_CREATED_FN       = "date_created"
  val DATE_STATUS_FN        = "date_status"

  def CONTRACT_ID_FK        = s"${TABLE_NAME}_${CONTRACT_ID_FN}_fkey"
  def CONTRACT_ID_INX       = s"fki_$CONTRACT_ID_FK"

  def STATUS_INX            = s"${TABLE_NAME}_${STATUS_FN}_idx"

}


@Singleton
class MOrders @Inject() (
  override protected val driver   : ExPgSlickDriverT,
  protected val mContracts        : MContracts
)
  extends PriceSlick
{

  import MOrder._
  import driver.api._

  /** Slick-описание таблицы заказов. */
  class MTable(tag: Tag)
    extends Table[MOrder](tag, TABLE_NAME)
    with CurrencyTable[MOrder]
    with PriceTable[MOrder]
  {

    def id            = column[Long](ID_FN, O.PrimaryKey, O.AutoInc)
    def statusStr     = column[String](STATUS_FN)
    def amount        = column[Amount_t](AMOUNT_FN)
    def currencyCode  = column[String](CURRENCY_CODE_FN)
    def contractId    = column[Long](CONTRACT_ID_FN)
    def dateCreated   = column[DateTime](DATE_CREATED_FN)
    def dateStatus    = column[DateTime](DATE_STATUS_FN)

    def contract      = foreignKey(CONTRACT_ID_FK, contractId, mContracts.contracts)(_.id)
    def contractIdInx = index(CONTRACT_ID_INX, contractId)

    def statusStrInx  = index(STATUS_INX, statusStr)

    def status = statusStr <> (MOrderStatuses.withNameT, MOrderStatuses.unapply)

    override def * : ProvenShape[MOrder] = {
      (status, contractId, price, dateCreated, dateStatus, id.?) <> (
        (MOrder.apply _).tupled, MOrder.unapply
      )
    }

  }


  val orders = TableQuery[MTable]

}


case class MOrder(
  status        : MOrderStatus,
  contractId    : Long,
  price         : MPrice,
  dateCreated   : DateTime = DateTime.now,
  dateStatus    : DateTime = DateTime.now,
  id            : Option[Long] = None
)
