package models

import org.joda.time.DateTime
import util.event._
import EsModel._
import util.SiowebEsUtil.client
import io.suggest.util.SioEsUtil.laFuture2sFuture
import scala.concurrent.Future
import MMart.MartId_t, MCompany.CompanyId_t
import org.elasticsearch.index.query.{FilterBuilders, QueryBuilders}
import org.elasticsearch.common.xcontent.XContentBuilder
import play.api.libs.concurrent.Execution.Implicits._
import io.suggest.util.SioEsUtil._
import io.suggest.util.SioConstants._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.01.14 19:23
 * Description: Таблица с магазинами, зарегистрированными в системе.
 */

object MShop extends EsModelStaticT[MShop] {

  type ShopId_t = String

  val ES_TYPE_NAME = "shop"

  override def generateMapping: XContentBuilder = jsonGenerator { implicit b =>
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
          id = MART_ID_ESFN,
          include_in_all = false,
          index = FieldIndexingVariants.not_analyzed
        ),
        FieldString(
          id = NAME_ESFN,
          include_in_all = true,
          index = FieldIndexingVariants.no
        ),
        FieldDate(
          id = DATE_CREATED_ESFN,
          include_in_all = false,
          index = FieldIndexingVariants.no
        ),
        FieldString(
          id = DESCRIPTION_ESFN,
          include_in_all = false,
          index = FieldIndexingVariants.no
        ),
        FieldNumber(
          id = MART_FLOOR_ESFN,
          fieldType = DocFieldTypes.integer,
          include_in_all = true,
          index = FieldIndexingVariants.no
        ),
        FieldNumber(
          id = MART_SECTION_ESFN,
          fieldType = DocFieldTypes.integer,
          include_in_all = true,
          index = FieldIndexingVariants.no
        )
      )
    )
  }

  protected def dummy(id: String) = MShop(
    id = Some(id),
    mart_id = null,
    company_id = null,
    name = null
  )

  def applyMap(m: collection.Map[String, AnyRef], acc: MShop): MShop = {
    m.foreach {
      case (COMPANY_ID_ESFN, value)   => acc.company_id   = companyIdParser(value)
      case (MART_ID_ESFN, value)      => acc.mart_id      = martIdParser(value)
      case (NAME_ESFN, value)         => acc.name         = nameParser(value)
      case (DATE_CREATED_ESFN, value) => acc.date_created = dateCreatedParser(value)
      case (DESCRIPTION_ESFN, value)  => acc.description  = Some(descriptionParser(value))
      case (MART_FLOOR_ESFN, value)   => acc.mart_floor   = Some(martFloorParser(value))
      case (MART_SECTION_ESFN, value) => acc.mart_section = Some(martSectionParser(value))
    }
    acc
  }

  /**
   * Найти все магазины, относящиеся к указанному ТЦ.
   * @param martId id торгового центра.
   * @return Список MShop в неопределённом порядке.
   */
  def getByMartId(martId: MartId_t): Future[Seq[MShop]] = {
    val martIdQuery = QueryBuilders.fieldQuery(MART_ID_ESFN, martId)
    client.prepareSearch(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
      .setQuery(martIdQuery)
      .execute()
      .map { searchResp2list }
  }

  /**
   * Выдать все магазины, находящиеся во владении указанной компании.
   * @param companyId id компании.
   * @return Список MShop в неопределённом порядке.
   */
  def getByCompanyId(companyId: CompanyId_t): Future[Seq[MShop]] = {
    val companyIdQuery = QueryBuilders.fieldQuery(COMPANY_ID_ESFN, companyId)
    client.prepareSearch(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
      .setQuery(companyIdQuery)
      .execute()
      .map { searchResp2list }
  }

  /**
   * Найти все магазины, принадлежащие конторе и расположенные в указанном ТЦ.
   * @param companyId id компании-владельца магазина.
   * @param martId id торгового центра.
   * @return Список MShop в неопределённом порядке.
   */
  def getByCompanyAndMart(companyId: CompanyId_t, martId:MartId_t): Future[Seq[MShop]] = {
    val companyIdQuery = QueryBuilders.fieldQuery(COMPANY_ID_ESFN, companyId)
    val martIdFilter = FilterBuilders.termFilter(MART_ID_ESFN, martId)
    val query = QueryBuilders.filteredQuery(companyIdQuery, martIdFilter)
    client.prepareSearch(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
      .setQuery(query)
      .execute()
      .map { searchResp2list }
  }

  /**
   * Прочитать значение martId для указанного магазина.
   * @param id id магазина.
   * @return id тц если такой магазин существует.
   */
  def getMartIdFor(id: String): Future[Option[MartId_t]] = {
    client.prepareGet(ES_INDEX_NAME, ES_TYPE_NAME, id)
      .setFields(MART_ID_ESFN)
      .execute()
      .map { getResp =>
        if (getResp.isExists) {
          Option(getResp.getField(MART_ID_ESFN))
            .map { field => martIdParser(field.getValue) }
        } else {
          None
        }
      }
  }

  /**
   * Удалить один магазин по id и породить системное сообщение об этом.
   * @param id Ключ ряда.
   * @return Кол-во удалённых рядов. Т.е. 0 или 1.
   */
  override def deleteById(id: ShopId_t): Future[Boolean] = {
    getMartIdFor(id) flatMap {
      case Some(martId) =>
        // TODO Безопасно ли вызывать super-методы из анонимных классов? Оно вроде работает, но всё же...
        val deleteFut = super.deleteById(id)
        deleteFut onSuccess {
          case true => SiowebNotifier publish YmShopDeletedEvent(martId=martId, shopId=id)
        }
        deleteFut

      case None => ???  // TODO хз что тут нужно делать, но это невероятная ситуация.
    }
  }
}


import MShop._

case class MShop(
  var company_id  : CompanyId_t,
  var mart_id     : MartId_t,
  var name        : String,
  var description : Option[String] = None,
  var mart_floor  : Option[Int] = None,
  var mart_section: Option[Int] = None,
  var id          : Option[MShop.ShopId_t] = None,
  var date_created : DateTime = null
) extends EsModelT[MShop] with MCompanySel with MMartSel with CompanyMartsSel with ShopPriceListSel with MShopOffersSel {

  def companion = MShop
  def shop_id = id.get

  override def writeJsonFields(acc: XContentBuilder) {
    acc.field(COMPANY_ID_ESFN, company_id)
      .field(MART_ID_ESFN, mart_id)
      .field(NAME_ESFN, name)
    if (description.isDefined)
      acc.field(DESCRIPTION_ESFN, description.get)
    if (mart_floor.isDefined)
      acc.field(MART_FLOOR_ESFN, mart_floor.get)
    if (mart_section.isDefined)
      acc.field(MART_SECTION_ESFN, mart_section.get)
    if (date_created == null)
      date_created = DateTime.now()
    acc.field(DATE_CREATED_ESFN, date_created)
  }

  override def save: Future[String] = {
    val fut = super.save
    // Если создан новый магазин, то надо уведомлять о создании нового магазина.
    if (id.isEmpty) {
      fut onSuccess { case newId =>
        SiowebNotifier publish YmShopAddedEvent(martId=mart_id, shopId=newId)
      }
    }
    fut
  }

  /** Обновить переменные текущего класса с помощью другого класса.
    * @param newMshop Другой экземпляр MShop, содержащий все необходимые данные.
    */
  def loadFrom(newMshop: MShop) {
    if (!(newMshop eq this)) {
      this.company_id = newMshop.company_id
      this.mart_id = newMshop.mart_id
      this.name = newMshop.name
      this.description = newMshop.description
      this.mart_floor = newMshop.mart_floor
      this.mart_section = newMshop.mart_section
    }
  }

}


trait CompanyShopsSel {
  def company_id: CompanyId_t
  def companyShops = getByCompanyId(company_id)
}

trait MartShopsSel {
  def mart_id: MartId_t
  def martShops = getByMartId(mart_id)
}

trait MShopSel {
  def shop_id: ShopId_t
  def shop = getById(shop_id)
}

