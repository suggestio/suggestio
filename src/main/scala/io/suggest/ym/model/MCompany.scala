package io.suggest.ym.model

import org.joda.time.DateTime
import scala.concurrent.{ExecutionContext, Future}
import org.elasticsearch.client.Client
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model._
import io.suggest.util.SioEsUtil._
import io.suggest.model.common._
import io.suggest.util.MacroLogsImpl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.01.14 17:40
 * Description: Компания - верхний элемент иерархии структуры юр.лиц.
 * Компания может владеть магазинами и торговыми центрами.
 */

object MCompany
  extends EsModelStaticEmpty
  with EMNameStatic
  with EMDateCreatedStatic
  with MacroLogsImpl
{

  override type T = MCompany

  type CompanyId_t = String

  val ES_TYPE_NAME: String = "company"


  def generateMappingStaticFields: List[Field] = List(
    FieldSource(enabled = true),
    FieldAll(enabled = false)
  )

  protected def dummy(id: String) = MCompany(id = Option(id), name = null)

  /**
   * Удалить документ по id.
   * @param id id документа.
   * @return true, если документ найден и удалён. Если не найден, то false
   */
  override def deleteById(id: String)(implicit ec:ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Boolean] = {
    val adnsCountFut = MAdnNode.countByCompanyId(id)
    adnsCountFut flatMap {
      case 0L =>
        super.deleteById(id)

      case adnsCount =>
        Future failed new ForeignKeyException(s"Cannot delete company with $adnsCount marts/shops or other AdnMembers.")
    }
  }
}


import MCompany._

/**
 * Экземпляр распарсенного ряда БД.
 * @param name Название конторы.
 * @param id id по базе.
 * @param dateCreated Дата создания ряда/компании.
 */
case class MCompany(
  var name          : String,
  id                : Option[MCompany.CompanyId_t] = None,
  var dateCreated   : DateTime = null
)
  extends EsModelEmpty
  with EMName
  with EMDateCreatedMut
{
  override type T = MCompany

  def companyId = id.get
  def companion = MCompany
}


trait MCompanySel {
  def companyId: CompanyId_t
  def company(implicit ec:ExecutionContext, client: Client) = getById(companyId)
}
