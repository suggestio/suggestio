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
import io.suggest.util.{MacroLogsImpl, JacksonWrapper}

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


  def generateMappingStaticFields: List[Field] = List(
    FieldSource(enabled = true),
    FieldAll(enabled = false, analyzer = FTS_RU_AN)
  )

  def generateMappingProps: List[DocField] = List(
    FieldString(COMPANY_ID_ESFN, include_in_all = false, index = FieldIndexingVariants.not_analyzed),
    FieldString(NAME_ESFN, include_in_all = true, index = FieldIndexingVariants.no),
    FieldString(TOWN_ESFN, include_in_all = true, index = FieldIndexingVariants.no),
    FieldString(ADDRESS_ESFN, include_in_all = true, index = FieldIndexingVariants.no),
    FieldString(SITE_URL_ESFN, include_in_all = false, index = FieldIndexingVariants.no),
    FieldDate(DATE_CREATED_ESFN, include_in_all = false, index = FieldIndexingVariants.no),
    FieldString(PHONE_ESFN, include_in_all = false, index = FieldIndexingVariants.no),
    FieldString(PERSON_ID_ESFN, include_in_all = false, index = FieldIndexingVariants.not_analyzed),
    FieldString(LOGO_IMG_ID, include_in_all = false, index = FieldIndexingVariants.no)
  )


  def applyKeyValue(acc: MMart): PartialFunction[(String, AnyRef), Unit] = {
    case (COMPANY_ID_ESFN, value)     => acc.companyId = companyIdParser(value)
    case (NAME_ESFN, value)           => acc.name = nameParser(value)
    case (ADDRESS_ESFN, value)        => acc.address = addressParser(value)
    case (SITE_URL_ESFN, value)       => acc.siteUrl = Option(siteUrlParser(value))
    case (DATE_CREATED_ESFN, value)   => acc.dateCreated = dateCreatedParser(value)
    case (TOWN_ESFN, value)           => acc.town = stringParser(value)
    case (PHONE_ESFN, value)          => acc.phone = Option(stringParser(value))
    case (PERSON_ID_ESFN, value)      => acc.personIds = JacksonWrapper.convert[List[String]](value)
    case (LOGO_IMG_ID, value)         => acc.logoImgId = Option(stringParser(value))
  }

  protected def dummy(id: String) = MMart(
    id = Option(id),
    companyId = null,
    name = null,
    address = null,
    town = null,
    siteUrl = None,
    personIds = null,
    phone = None
  )

  def companyIdQuery(companyId: CompanyId_t) = QueryBuilders.termQuery(ES_TYPE_NAME, companyId)

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
   * Найти ТЦ, относящиеся к указанному юзеру.
   * @param personId id юзера.
   * @return Список найденных результатов.
   */
  def findByPersonId(personId: String)(implicit ec: ExecutionContext, client: Client): Future[Seq[MMart]] = {
    val personIdQuery = QueryBuilders.termQuery(PERSON_ID_ESFN, personId)
    client.prepareSearch(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
      .setQuery(personIdQuery)
      .execute()
      .map { searchResp2list }
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
  var town          : String,
  var address       : String,
  var siteUrl       : Option[String],
  var phone         : Option[String],
  var personIds     : List[String],
  var logoImgId     : Option[String] = None,
  id                : Option[MMart.MartId_t] = None,
  var dateCreated   : DateTime = null
) extends EsModelT[MMart] with MCompanySel with CompanyShopsSel with MartShopsSel {
  def martId = id.get
  def companion = MMart

  def mainPersonId = personIds.lastOption

  def writeJsonFields(acc: XContentBuilder) {
    if (companyId == null || name == null || town == null || address == null) {
      throw new IllegalStateException("companyId or name is null")
    }
    acc.field(COMPANY_ID_ESFN, companyId)
      .field(NAME_ESFN, name)
      .field(TOWN_ESFN, town)
      .field(ADDRESS_ESFN, address)
    if (siteUrl.isDefined)
      acc.field(SITE_URL_ESFN, siteUrl.get)
    if (phone.isDefined)
      acc.field(PHONE_ESFN, phone.get)
    if (dateCreated == null)
      dateCreated = DateTime.now()
    if (logoImgId.isDefined)
      acc.field(LOGO_IMG_ID, logoImgId.get)
    if (!personIds.isEmpty)
      acc.array(PERSON_ID_ESFN, personIds : _*)
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


trait MMartOptSel {
  def martId: Option[MartId_t]
  def mart(implicit ec:ExecutionContext, client: Client) = {
    martId match {
      case Some(_martId)  => getById(_martId)
      case None           => Future successful None
    }
  }
}

trait CompanyMartsSel {
  def companyId: CompanyId_t
  def companyMarts(implicit ec:ExecutionContext, client: Client) = getByCompanyId(companyId)
}

