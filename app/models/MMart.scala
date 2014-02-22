package models

import org.joda.time.DateTime
import util.event._
import scala.concurrent.Future
import util.SiowebEsUtil.client
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.common.xcontent.XContentBuilder
import EsModel._
import MCompany.CompanyId_t
import io.suggest.util.SioEsUtil.laFuture2sFuture
import play.api.libs.concurrent.Execution.Implicits._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.01.14 18:31
 * Description: Mart -- это торговое помещение, имеющее адрес. Частные случаи:
 * - Торговый центр. Как правило, это и есть mart.
 * - Рынок.
 * - Собственное помещение единственного мазагина.
 */

object MMart extends EsModelStaticT[MMart] {

  type MartId_t = String

  val ES_TYPE_NAME = "mart"

  def applyMap(m: collection.Map[String, AnyRef], acc: MMart): MMart = {
    m foreach {
      case (COMPANY_ID_ESFN, value)   => acc.company_id = companyIdParser(value)
      case (NAME_ESFN, value)         => acc.name = nameParser(value)
      case (ADDRESS_ESFN, value)      => acc.address = addressParser(value)
      case (SITE_URL_FN, value)       => acc.site_url = Some(siteUrlParser(value))
      case (DATE_CREATED_ESFN, value) => acc.date_created = dateCreatedParser(value)
    }
    acc
  }

  protected def dummy(id: String) = MMart(
    company_id = null,
    name = null,
    address = null,
    site_url = None
  )

  /**
   * Вернуть все ТЦ, находящиеся во владении указанной конторы.
   * @param companyId id конторы.
   * @return Список ТЦ в неопределённом порядке.
   */
  def getByCompanyId(companyId: CompanyId_t): Future[Seq[MMart]] = {
    val companyIdQuery = QueryBuilders.fieldQuery(ES_TYPE_NAME, companyId)
    client.prepareSearch(ES_INDEX_NAME)
      .setQuery(companyIdQuery)
      .execute()
      .map { searchResp2list }
  }

  /**
   * Удалить ТЦ с указанными id. Центральная фунцкия удаления, остальные должны дергать её из-за
   * сайд-эффектов на кравлер.
   * @param id Идентификатор.
   * @return Кол-во удалённых рядов. Т.е. 0 или 1.
   */
  override def deleteById(id: String): Future[Boolean] = {
    val fut = super.deleteById(id)
    fut onSuccess {
      case true => SiowebNotifier publish YmMartDeletedEvent(id)
    }
    fut
  }
}


import MMart._

case class MMart(
  var company_id    : CompanyId_t,
  var name          : String,
  var address       : String,
  var site_url      : Option[String],
  id                : Option[MMart.MartId_t] = None,
  var date_created  : DateTime = null
) extends EsModelT[MMart] with MCompanySel with CompanyShopsSel with MartShopsSel {
  def mart_id = id.get
  def companion = MMart

  def writeJsonFields(acc: XContentBuilder) {
    acc.field(COMPANY_ID_ESFN, company_id)
      .field(NAME_ESFN, name)
      .field(ADDRESS_ESFN, address)
    if (site_url.isDefined)
      acc.field(SITE_URL_FN, site_url)
    if (date_created == null)
      date_created = DateTime.now()
    acc.field(DATE_CREATED_ESFN, date_created)
  }

  /**
   * Сохранить экземпляр в хранилище ES и сгенерить уведомление, если экземпляр обновлён.
   * @return Фьючерс с новым/текущим id
   */
  override def save: Future[String] = {
    val fut = super.save
    if (id.isEmpty) {
      fut onSuccess { case martId =>
        SiowebNotifier publish YmMartAddedEvent(martId)
      }
    }
    fut
  }
}


trait MMartSel {
  def mart_id: MartId_t
  def mart = getById(mart_id)
}

trait CompanyMartsSel {
  def company_id: CompanyId_t
  def companyMarts = getByCompanyId(company_id)
}

