package io.suggest.ym.model

import io.suggest.model.common.{EMName, EMDateCreatedStatic}
import io.suggest.model.es._
import io.suggest.ym.model.common.{EMCompanyMetaMut, MCompanyMeta, EMCompanyMetaStatic}
import scala.concurrent.ExecutionContext
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

  // compat: 2014.07.01: поля name и dateCreated были перемещены в контейнер-поле meta.
  // TODO Пересохранить все компании и удалить этот код:
  override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (EMName.NAME_ESFN, nameRaw) =>
        acc.meta = acc.meta.copy(name = EsModelUtil.stringParser(nameRaw))
      case (EMDateCreatedStatic.DATE_CREATED_ESFN, dcRaw) =>
        acc.meta = acc.meta.copy(dateCreated = EsModelUtil.dateTimeParser(dcRaw))
    }
  }
}


/**
 * Экземпляр распарсенного ряда БД.
 * @param id id по базе.
 */
final case class MCompany(
  var meta    : MCompanyMeta,
  id          : Option[String] = None,
  versionOpt  : Option[Long] = None
)
  extends EsModelPlayJsonEmpty
  with EsModelT
  with EMCompanyMetaMut
{
  override type T = MCompany
  override def companion = MCompany

}


trait MCompanyJmxMBean extends EsModelJMXMBeanI
final class MCompanyJmx(implicit val ec: ExecutionContext, val client: Client, val sn: SioNotifierStaticClientI)
  extends EsModelJMXBase
  with MCompanyJmxMBean
{
  override def companion = MCompany
}

