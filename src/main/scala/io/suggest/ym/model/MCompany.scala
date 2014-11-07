package io.suggest.ym.model

import io.suggest.model.common.{EMName, EMDateCreatedStatic, EMNameMut}
import io.suggest.ym.model.common.{EMCompanyMetaMut, MCompanyMeta, EMCompanyMeta, EMCompanyMetaStatic}
import scala.concurrent.{ExecutionContext, Future}
import org.elasticsearch.client.Client
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model._
import io.suggest.util.SioEsUtil._
import io.suggest.util.MacroLogsImpl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.01.14 17:40
 * Description: Компания - верхний элемент иерархии структуры юр.лиц.
 * Компания может владеть магазинами и торговыми центрами.
 */

object MCompany
  extends EsModelStaticMutAkvEmptyT
  with EsModelStaticT
  with EMCompanyMetaStatic
  with MacroLogsImpl
{

  override type T = MCompany
  override val ES_TYPE_NAME = "company"


  override def generateMappingStaticFields: List[Field] = List(
    FieldSource(enabled = true),
    FieldAll(enabled = false)
  )

  override protected def dummy(id: Option[String], version: Option[Long]) = {
    MCompany(id = id, versionOpt = version, meta = MCompanyMeta(name = ""))
  }

  /**
   * Удалить документ по id.
   * @param id id документа.
   * @return true, если документ найден и удалён. Если не найден, то false
   */
  override def deleteById(id: String, ignoreResources: Boolean = false)
                         (implicit ec:ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Boolean] = {
    val adnsCountFut = MAdnNode.countByCompanyId(id)
    adnsCountFut flatMap {
      case 0L =>
        super.deleteById(id, ignoreResources)

      case adnsCount =>
        Future failed new ForeignKeyException(s"Cannot delete company with $adnsCount marts/shops or other AdnMembers.")
    }
  }

  // compat: 2014.07.01: поля name и dateCreated были перемещены в контейнер-поле meta.
  // TODO Пересохранить все компании и удалить этот код:
  override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (EMName.NAME_ESFN, nameRaw) =>
        acc.meta = acc.meta.copy(name = EsModel.stringParser(nameRaw))
      case (EMDateCreatedStatic.DATE_CREATED_ESFN, dcRaw) =>
        acc.meta = acc.meta.copy(dateCreated = EsModel.dateTimeParser(dcRaw))
    }
  }
}


import MCompany._

/**
 * Экземпляр распарсенного ряда БД.
 * @param id id по базе.
 */
final case class MCompany(
  var meta    : MCompanyMeta,
  id          : Option[String] = None,
  versionOpt  : Option[Long] = None
)
  extends EsModelEmpty
  with EsModelT
  with EMCompanyMetaMut
{
  override type T = MCompany
  override def companion = MCompany

}


trait MCompanySel {
  def companyId: String
  def company(implicit ec:ExecutionContext, client: Client) = getById(companyId)
}


trait MCompanyJmxMBean extends EsModelJMXMBeanI
final class MCompanyJmx(implicit val ec: ExecutionContext, val client: Client, val sn: SioNotifierStaticClientI)
  extends EsModelJMXBase
  with MCompanyJmxMBean
{
  override def companion = MCompany
}

