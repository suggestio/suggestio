package models

import anorm._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.06.14 14:59
 * Description: Каталог платежных реквизитов.
 */
object MBillPayReqsRu extends SqlModelStatic[MBillPayReqsRu] {
  import SqlParser._

  override val TABLE_NAME = "bill_pay_reqs_ru"
  override val rowParser: RowParser[MBillPayReqsRu] = {
    //get[Pk[Int]]("id")
    ???
  }
}

case class MBillPayReqsRu(
  rName: String,
  rInn : Long,
  rKpp : Long,
  rOkato: Option[Long],
  rOkmto: Option[Long],
  bankName: String,
  bankBik: Long,
  bankBkk: String,
  accountNumber: String,
  commentPrefix: Option[String] = None,
  commentSuffix: Option[String] = None,
  id: Option[Int] = None
)

