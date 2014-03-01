package io.suggest.ym.model

import org.joda.time.DateTime
import scala.concurrent.{ExecutionContext, Future}
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.common.xcontent.XContentBuilder
import MCompany.CompanyId_t
import io.suggest.util.SioConstants._
import io.suggest.proto.bixo.crawler.MainProto
import org.elasticsearch.client.Client
import io.suggest.event._
import io.suggest.model._
import io.suggest.model.EsModel._
import io.suggest.util.SioEsUtil._

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

  type MartId_t = MainProto.MartId_t

  val ES_TYPE_NAME = "mart"

  def generateMapping: XContentBuilder = jsonGenerator { implicit b =>
    IndexMapping(
      typ = ES_TYPE_NAME,
      static_fields = Seq(
        FieldSource(enabled = true),
        FieldAll(enabled = false, analyzer = FTS_RU_AN)
      ),
      properties = Seq(
        FieldString(
          id = COMPANY_ID_ESFN,
          include_in_all = false,
          index = FieldIndexingVariants.not_analyzed
        ),
        FieldString(
          id = NAME_ESFN,
          include_in_all = true,
          index = FieldIndexingVariants.no
        ),
        FieldString(
          id = ADDRESS_ESFN,
          include_in_all = true,
          index = FieldIndexingVariants.no
        ),
        FieldString(
          id = SITE_URL_ESFN,
          include_in_all = false,
          index = FieldIndexingVariants.no
        ),
        FieldDate(
          id = DATE_CREATED_ESFN,
          include_in_all = false,
          index = FieldIndexingVariants.no
        )
      )
    )
  }


  def applyKeyValue(acc: MMart): PartialFunction[(String, AnyRef), Unit] = {
    case (COMPANY_ID_ESFN, value)     => acc.companyId = companyIdParser(value)
    case (NAME_ESFN, value)           => acc.name = nameParser(value)
    case (ADDRESS_ESFN, value)        => acc.address = addressParser(value)
    case (SITE_URL_ESFN, value)       => acc.siteUrl = Some(siteUrlParser(value))
    case (DATE_CREATED_ESFN, value)   => acc.dateCreated = dateCreatedParser(value)
  }

  protected def dummy(id: String) = MMart(
    id = Some(id),
    companyId = null,
    name = null,
    address = null,
    siteUrl = None
  )

  def companyIdQuery(companyId: CompanyId_t) = QueryBuilders.fieldQuery(ES_TYPE_NAME, companyId)

  /**
   * Вернуть все ТЦ, находящиеся во владении указанной конторы.
   * @param companyId id конторы.
   * @return Список ТЦ в неопределённом порядке.
   */
  def getByCompanyId(companyId: CompanyId_t)(implicit ec:ExecutionContext, client: Client): Future[Seq[MMart]] = {
    client.prepareSearch(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
      .setQuery(companyIdQuery(companyId))
      .execute()
      .map { searchResp2list }
  }

  def countByCompanyId(companyId: CompanyId_t)(implicit ec:ExecutionContext, client: Client): Future[Long] = {
    client.prepareCount(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
      .setQuery(companyIdQuery(companyId))
      .execute()
      .map { _.getCount }
  }

  /**
   * Удалить ТЦ с указанными id. Центральная фунцкия удаления, остальные должны дергать её из-за
   * сайд-эффектов на кравлер.
   * Перед удалением проверяется, нет ли магазинов в этом ТЦ.
   * @param id Идентификатор.
   * @return Кол-во удалённых рядов. Т.е. 0 или 1.
   */
  override def deleteById(id: String)(implicit ec:ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Boolean] = {
    MShop.countByMartId(id) flatMap {
      case 0L =>
        val fut = super.deleteById(id)
        fut onSuccess {
          case true => sn publish YmMartDeletedEvent(id)
        }
        fut

      case martShopCount =>
        Future failed new ForeignKeyException(s"Cannot delete mart with $martShopCount shops. Delete shops first.")
    }
  }
}


import MMart._

case class MMart(
  var companyId     : CompanyId_t,
  var name          : String,
  var address       : String,
  var siteUrl       : Option[String],
  id                : Option[MMart.MartId_t] = None,
  var dateCreated   : DateTime = null
) extends EsModelT[MMart] with MCompanySel with CompanyShopsSel with MartShopsSel {
  def martId = id.get
  def companion = MMart

  def writeJsonFields(acc: XContentBuilder) {
    acc.field(COMPANY_ID_ESFN, companyId)
      .field(NAME_ESFN, name)
      .field(ADDRESS_ESFN, address)
    if (siteUrl.isDefined)
      acc.field(SITE_URL_ESFN, siteUrl.get)
    if (dateCreated == null)
      dateCreated = DateTime.now()
    acc.field(DATE_CREATED_ESFN, dateCreated)
  }

  /**
   * Сохранить экземпляр в хранилище ES и сгенерить уведомление, если экземпляр обновлён.
   * @return Фьючерс с новым/текущим id
   */
  override def save(implicit ec:ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[String] = {
    val fut = super.save
    if (id.isEmpty) {
      fut onSuccess { case martId =>
        sn publish YmMartAddedEvent(martId)
      }
    }
    fut
  }
}


trait MMartSel {
  def martId: MartId_t
  def mart(implicit ec:ExecutionContext, client: Client) = getById(martId)
}

trait CompanyMartsSel {
  def companyId: CompanyId_t
  def companyMarts(implicit ec:ExecutionContext, client: Client) = getByCompanyId(companyId)
}

