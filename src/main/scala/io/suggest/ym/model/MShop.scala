package io.suggest.ym.model

import org.joda.time.DateTime
import io.suggest.model._
import EsModel._
import scala.concurrent.{ExecutionContext, Future}
import MMart.MartId_t, MCompany.CompanyId_t
import org.elasticsearch.index.query.{FilterBuilders, QueryBuilders}
import org.elasticsearch.common.xcontent.XContentBuilder
import io.suggest.util.SioEsUtil._
import io.suggest.util.SioConstants._
import io.suggest.proto.bixo.crawler.MainProto
import org.elasticsearch.client.Client
import io.suggest.event._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.01.14 19:23
 * Description: Таблица с магазинами, зарегистрированными в системе.
 * Модель денормализованна по полю mart_id: оно может быть пустым, и это значит, что магазин
 * не привязан к конкретному ТЦ. В дальнейшем, при необходимости можно будет
 */

object MShop extends EsModelStaticT[MShop] {

  type ShopId_t = MainProto.ShopId_t

  val ES_TYPE_NAME = "shop"

  def generateMapping: XContentBuilder = jsonGenerator { implicit b =>
    IndexMapping(
      typ = ES_TYPE_NAME,
      staticFields = Seq(
        FieldSource(enabled = true),
        FieldAll(enabled = true, analyzer = FTS_RU_AN)
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
          include_in_all = true,
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
    martId = null,
    companyId = null,
    name = null
  )


  def applyKeyValue(acc: MShop): PartialFunction[(String, AnyRef), Unit] = {
    case (COMPANY_ID_ESFN, value)     => acc.companyId   = companyIdParser(value)
    case (MART_ID_ESFN, value)        => acc.martId      = Some(martIdParser(value))
    case (NAME_ESFN, value)           => acc.name        = nameParser(value)
    case (DATE_CREATED_ESFN, value)   => acc.dateCreated = dateCreatedParser(value)
    case (DESCRIPTION_ESFN, value)    => acc.description = Some(descriptionParser(value))
    case (MART_FLOOR_ESFN, value)     => acc.martFloor   = Some(martFloorParser(value))
    case (MART_SECTION_ESFN, value)   => acc.martSection = Some(martSectionParser(value))
  }

  /**
   * Найти все магазины, относящиеся к указанному ТЦ.
   * @param martId id торгового центра.
   * @param sortField Название поля, по которому надо сортировать результаты.
   * @param isReversed Если true, то сортировать будем в обратном порядке.
   *                   Игнорируется, если sortField не задано.
   * @return Список MShop в неопределённом порядке.
   */
  def findByMartId(martId: MartId_t, sortField: Option[String] = None, isReversed:Boolean = false)(implicit ec:ExecutionContext, client: Client): Future[Seq[MShop]] = {
    val req = client.prepareSearch(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
      .setQuery(martIdQuery(martId))
    if (sortField.isDefined)
      req.addSort(sortField.get, isReversed2sortOrder(isReversed))
    req.execute()
      .map { searchResp2list }
  }

  def martIdQuery(martId: MartId_t) = QueryBuilders.termQuery(MART_ID_ESFN, martId)

  def countByMartId(martId: MartId_t)(implicit ec:ExecutionContext, client: Client): Future[Long] = {
    client.prepareCount(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
      .setQuery(martIdQuery(martId))
      .execute()
      .map { _.getCount }
  }


  /**
   * Выдать все магазины, находящиеся во владении указанной компании.
   * @param companyId id компании.
   * @return Список MShop в неопределённом порядке.
   */
  def getByCompanyId(companyId: CompanyId_t)(implicit ec:ExecutionContext, client: Client): Future[Seq[MShop]] = {
    client.prepareSearch(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
      .setQuery(companyIdQuery(companyId))
      .execute()
      .map { searchResp2list }
  }

  def companyIdQuery(companyId: CompanyId_t) = QueryBuilders.termQuery(COMPANY_ID_ESFN, companyId)

  def countByCompanyId(companyId: CompanyId_t)(implicit ec:ExecutionContext, client: Client): Future[Long] = {
    client.prepareCount(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
      .setQuery(companyIdQuery(companyId))
      .execute()
      .map { _.getCount }
  }

  /**
   * Найти все магазины, принадлежащие конторе и расположенные в указанном ТЦ.
   * @param companyId id компании-владельца магазина.
   * @param martId id торгового центра.
   * @return Список MShop в неопределённом порядке.
   */
  def getByCompanyAndMart(companyId: CompanyId_t, martId:MartId_t)(implicit ec:ExecutionContext, client: Client): Future[Seq[MShop]] = {
    val martIdFilter = FilterBuilders.termFilter(MART_ID_ESFN, martId)
    val query = QueryBuilders.filteredQuery(companyIdQuery(companyId), martIdFilter)
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
  def getMartIdFor(id: String)(implicit ec:ExecutionContext, client: Client): Future[Option[MartId_t]] = {
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
  override def deleteById(id: ShopId_t)(implicit ec:ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Boolean] = {
    getMartIdFor(id) flatMap {
      case Some(martId) =>
        // TODO Безопасно ли вызывать super-методы из анонимных классов? Оно вроде работает, но всё же...
        val deleteFut = super.deleteById(id)
        deleteFut onSuccess {
          case true => sn publish YmShopDeletedEvent(martId=martId, shopId=id)
        }
        deleteFut onSuccess {
          // TODO Вынести это в SioNotifer?
          case _ => MShopPriceList.deleteByShop(id)
        }
        deleteFut

      case None => ???  // TODO хз что тут нужно делать, но это невероятная ситуация.
    }
  }
}


import MShop._

case class MShop(
  var companyId   : CompanyId_t,
  var martId      : Option[MartId_t] = None,
  var name        : String,
  var description : Option[String] = None,
  var martFloor   : Option[Int] = None,
  var martSection : Option[Int] = None,
  var id          : Option[MShop.ShopId_t] = None,
  var dateCreated : DateTime = null
) extends EsModelT[MShop] with MCompanySel with MMartOptSel with CompanyMartsSel with ShopPriceListSel with MShopOffersSel {

  def companion = MShop
  def shopId = id.get

  override def writeJsonFields(acc: XContentBuilder) {
    acc.field(COMPANY_ID_ESFN, companyId)
      .field(NAME_ESFN, name)
    if (martId.isDefined)
      acc.field(MART_ID_ESFN, martId.get)
    if (description.isDefined)
      acc.field(DESCRIPTION_ESFN, description.get)
    if (martFloor.isDefined)
      acc.field(MART_FLOOR_ESFN, martFloor.get)
    if (martSection.isDefined)
      acc.field(MART_SECTION_ESFN, martSection.get)
    if (dateCreated == null)
      dateCreated = DateTime.now()
    acc.field(DATE_CREATED_ESFN, dateCreated)
  }

  override def save(implicit ec:ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[String] = {
    val fut = super.save
    // Если создан новый магазин, то надо уведомлять о создании нового магазина.
    if (id.isEmpty && martId.isDefined) {
      fut onSuccess { case newId =>
        sn publish YmShopAddedEvent(martId=martId.get, shopId=newId)
      }
    }
    fut
  }

  /** Обновить переменные текущего класса с помощью другого класса.
    * @param newMshop Другой экземпляр MShop, содержащий все необходимые данные.
    */
  def loadFrom(newMshop: MShop) {
    if (!(newMshop eq this)) {
      this.companyId = newMshop.companyId
      this.martId = newMshop.martId
      this.name = newMshop.name
      this.description = newMshop.description
      this.martFloor = newMshop.martFloor
      this.martSection = newMshop.martSection
    }
  }

}


trait CompanyShopsSel {
  def companyId: CompanyId_t
  def companyShops(implicit ec:ExecutionContext, client: Client) = getByCompanyId(companyId)
}

trait MartShopsSel {
  def martId: MartId_t
  def martShops(implicit ec:ExecutionContext, client: Client) = findByMartId(martId)
}

trait MShopSel {
  def shopId: ShopId_t
  def shop(implicit ec:ExecutionContext, client: Client) = getById(shopId)
}

