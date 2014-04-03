package io.suggest.ym.model

import org.joda.time.DateTime
import io.suggest.model._
import EsModel._
import scala.concurrent.{ExecutionContext, Future}
import MMart.MartId_t, MCompany.CompanyId_t
import org.elasticsearch.index.query.{QueryBuilder, FilterBuilders, QueryBuilders}
import org.elasticsearch.common.xcontent.{XContentFactory, XContentBuilder}
import io.suggest.util.SioEsUtil._
import io.suggest.util.SioConstants._
import io.suggest.proto.bixo.crawler.MainProto
import org.elasticsearch.client.Client
import io.suggest.event._
import io.suggest.util.JacksonWrapper
import org.elasticsearch.common.unit.Fuzziness
import io.suggest.util.MyConfig.CONFIG

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.01.14 19:23
 * Description: Таблица с магазинами, зарегистрированными в системе.
 *
 * Модель денормализованна по полю mart_id: оно может быть пустым, и это значит, что магазин
 * не привязан к конкретному ТЦ. В дальнейшем, при необходимости можно будет
 *
 * Модель денормализована по полю personId: оно содержит список пользователей, которые могут управлять магазином.
 * Новые пользователи добавляются в начало списка. Последний пользователь списка списка имеет повышенный статус.
 * Например, можно считать его админом, и тогда он как бы может управлять оставшимся списком пользователей и
 * делать прочие административные действия.
 */

object MShop extends EsModelStaticT[MShop] {

  type ShopId_t = MainProto.ShopId_t

  /** При апдейте маппига надо игнорить конфликты из-за settings */  // TODO Снести после первого публичного запуска.
  override protected def mappingIgnoreConflicts: Boolean = true

  val ES_TYPE_NAME = "shop"

  // Ключи настроек, никогда не индексируются и не анализируются.
  val SETTING_SUP_IS_ENABLED      = SETTINGS_ESFN + ".sup.isEnabled"
  val SETTING_SUP_DISABLE_REASON  = SETTINGS_ESFN + ".sup.disableReason"
  val SETTING_SUP_WITH_LEVELS     = SETTINGS_ESFN + ".sup.withLevels"
  val SETTING_SUP_LSHOP_SHOWN_ADS_MAX   = SETTINGS_ESFN + ".sup.levels.shop.ads.shown.max"


  def generateMappingStaticFields: List[Field] = List(
    FieldSource(enabled = true),
    FieldAll(enabled = true, index_analyzer = EDGE_NGRAM_AN_1)
  )

  def generateMappingProps: List[DocField] = List(
    FieldString(COMPANY_ID_ESFN, include_in_all = false, index = FieldIndexingVariants.not_analyzed),
    FieldString(MART_ID_ESFN, include_in_all = false, index = FieldIndexingVariants.not_analyzed),
    FieldString(NAME_ESFN, include_in_all = true, index = FieldIndexingVariants.no),
    FieldString(PERSON_ID_ESFN, include_in_all = false, index = FieldIndexingVariants.not_analyzed),
    FieldDate(DATE_CREATED_ESFN, include_in_all = false, index = FieldIndexingVariants.no),
    FieldString(DESCRIPTION_ESFN, include_in_all = true, index = FieldIndexingVariants.no),
    FieldNumber(MART_FLOOR_ESFN, fieldType = DocFieldTypes.integer, include_in_all = true, index = FieldIndexingVariants.no),
    FieldNumber(MART_SECTION_ESFN, fieldType = DocFieldTypes.integer, include_in_all = true, index = FieldIndexingVariants.no),
    FieldString(LOGO_IMG_ID, include_in_all = false, index = FieldIndexingVariants.no),
    // settings: нужно их заполнять по мере появления настроек в MShopSettings.
    FieldBoolean(SETTING_SUP_IS_ENABLED, include_in_all = false, index = FieldIndexingVariants.not_analyzed),
    FieldString(SETTING_SUP_DISABLE_REASON, include_in_all = false, index = FieldIndexingVariants.no),
    FieldString(SETTING_SUP_WITH_LEVELS, include_in_all = false, index = FieldIndexingVariants.not_analyzed),
    FieldNumber(SETTING_SUP_LSHOP_SHOWN_ADS_MAX, fieldType = DocFieldTypes.integer, include_in_all = false, index = FieldIndexingVariants.no)
  )


  protected def dummy(id: String) = MShop(
    id = Option(id),
    martId = null,
    companyId = null,
    name = null,
    personIds = Nil
  )


  def applyKeyValue(acc: MShop): PartialFunction[(String, AnyRef), Unit] = {
    case (COMPANY_ID_ESFN, value)     => acc.companyId   = companyIdParser(value)
    case (MART_ID_ESFN, value)        => acc.martId      = Option(martIdParser(value))
    case (NAME_ESFN, value)           => acc.name        = nameParser(value)
    case (DATE_CREATED_ESFN, value)   => acc.dateCreated = dateCreatedParser(value)
    case (DESCRIPTION_ESFN, value)    => acc.description = Option(descriptionParser(value))
    case (MART_FLOOR_ESFN, value)     => acc.martFloor   = Option(martFloorParser(value))
    case (MART_SECTION_ESFN, value)   => acc.martSection = Option(martSectionParser(value))
    case (PERSON_ID_ESFN, value)      => acc.personIds   = JacksonWrapper.convert[List[String]](value)
    case (LOGO_IMG_ID, value)         => acc.logoImgId   = Option(stringParser(value))
    // Парсеры конкретных сеттингов.
    case (SETTING_SUP_IS_ENABLED, v)  =>
      acc.settings.supIsEnabled = booleanParser(v)
    case (SETTING_SUP_DISABLE_REASON, v) =>
      acc.settings.supDisableReason = Option(stringParser(v))
    case (SETTING_SUP_WITH_LEVELS, v: java.lang.Iterable[_]) =>
      acc.settings.supWithLevels = AdShowLevels.deserializeLevelsFrom(v)
    case (SETTING_SUP_LSHOP_SHOWN_ADS_MAX, v) =>
      acc.settings.supLShopMaxAdsShown = intParser(v)
  }

  /**
   * Поиск по указанному запросу.
   * @param searchQuery Поисковый запрос.
   * @return Список результатов в порядке релевантности.
   */
  def searchAll(searchQuery: String, martId: Option[String] = None)(implicit ec: ExecutionContext, client: Client): Future[Seq[MShop]] = {
    var textQuery: QueryBuilder = QueryBuilders.fuzzyQuery("_all", searchQuery)
      .fuzziness(Fuzziness.AUTO)
      .prefixLength(2)
      .maxExpansions(20)
    if (martId.isDefined) {
      val martIdFilter = FilterBuilders.termFilter(MART_ID_ESFN, martId.get)
      textQuery = QueryBuilders.filteredQuery(textQuery, martIdFilter)
    }
    client.prepareSearch(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
      .setQuery(textQuery)
      .execute()
      .map { searchResp2list }
  }

  /**
   * Найти все магазины, относящиеся к указанному ТЦ.
   * @param martId id торгового центра.
   * @param sortField Название поля, по которому надо сортировать результаты.
   * @param isReversed Если true, то сортировать будем в обратном порядке.
   *                   Игнорируется, если sortField не задано.
   * @param onlyEnabled Если true, то будет фильтр settings.supIsEnabled = true.
   * @return Список MShop в неопределённом порядке.
   */
  def findByMartId(martId: MartId_t, sortField: Option[String] = None, isReversed:Boolean = false, onlyEnabled: Boolean = false)(implicit ec:ExecutionContext, client: Client): Future[Seq[MShop]] = {
    var query: QueryBuilder = martIdQuery(martId)
    if (onlyEnabled) {
      val isEnabledFilter = FilterBuilders.termFilter(SETTING_SUP_IS_ENABLED, true)
      query = QueryBuilders.filteredQuery(query, isEnabledFilter)
    }
    val req = client.prepareSearch(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
      .setQuery(query)
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
   * Найти магазины, относящиеся к указанному юзеру.
   * @param personId id юзера.
   * @return Список найденных результатов.
   */
  def findByPersonId(personId: String)(implicit ec: ExecutionContext, client: Client): Future[Seq[MShop]] = {
    val personIdQuery = QueryBuilders.termQuery(PERSON_ID_ESFN, personId)
    client.prepareSearch(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
      .setQuery(personIdQuery)
      .execute()
      .map { searchResp2list }
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

  /**
   * Статическое обновление сеттингов isEnabled и disabledReason.
   * @param shopId id изменяемого магазина
   * @param isEnabled Новое значение поля isEnabled.
   * @param reason Причина изменения статуса.
   * @return Фьючерс. Внутри, скорее всего, лежит UpdateResponse.
   */
  def setIsEnabled(shopId: ShopId_t, isEnabled: Boolean, reason: Option[String])(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[_] = {
    val updatedXCB = XContentFactory.jsonBuilder()
      .startObject()
        .field(SETTING_SUP_IS_ENABLED, isEnabled)
        .field(SETTING_SUP_DISABLE_REASON, reason getOrElse null)
      .endObject()
    val fut = client.prepareUpdate(ES_INDEX_NAME, ES_TYPE_NAME, shopId)
      .setDoc(updatedXCB)
      .execute()
    // Уведомить о переключении состояния магазина
    fut onSuccess { case _ =>
      sn publish MShopOnOffEvent(shopId, isEnabled, reason)
    }
    fut
  }

  /**
   * Обновить в магазине поле с разрешенными дополнительными уровнями отображения.
   * @param mshop экземпляр магазина.
   * @return Фьючерс для синхронизации.
   */
  // TODO Нужен апдейт массива уровней через mvel-скрипт
  private def setShowLevels(mshop: MShop)(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[_] = {
    val shopId = mshop.id.get
    val levels = mshop.settings.supWithLevels
    val updateXCB = XContentFactory.jsonBuilder()
      .startObject()
        .startArray(SETTING_SUP_WITH_LEVELS)
        levels foreach { sl => updateXCB value sl.toString }
        updateXCB.endArray()
      .endObject()
    val fut: Future[_] = client.prepareUpdate(ES_INDEX_NAME, ES_TYPE_NAME, shopId)
      .setDoc(updateXCB)
      .execute()
    fut onSuccess {
      case _  => sn publish MShopSavedEvent(mshop)
    }
    fut
  }

}


import MShop._

case class MShop(
  var companyId   : CompanyId_t,
  var martId      : Option[MartId_t] = None,
  var name        : String,
  var personIds   : List[String],
  var description : Option[String] = None,
  var martFloor   : Option[Int] = None,
  var martSection : Option[Int] = None,
  var id          : Option[MShop.ShopId_t] = None,
  var logoImgId   : Option[String] = None,
  var settings    : MShopSettings = new MShopSettings,
  var dateCreated : DateTime = null
) extends EsModelT[MShop] with BuyPlaceT[MShop] with MCompanySel with MMartOptSel with CompanyMartsSel with ShopPriceListSel with MShopOffersSel {

  def companion = MShop
  def shopId = id.get

  override def writeJsonFields(acc: XContentBuilder) {
    acc.field(COMPANY_ID_ESFN, companyId)
      .field(NAME_ESFN, name)
    if (!personIds.isEmpty)
      acc.array(PERSON_ID_ESFN, personIds : _*)
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
    if (logoImgId.isDefined)
      acc.field(LOGO_IMG_ID, logoImgId.get)
    settings writeXContent acc
    acc.field(DATE_CREATED_ESFN, dateCreated)
  }

  def mainPersonId = personIds.lastOption

  override def save(implicit ec:ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[String] = {
    val fut = super.save
    fut onSuccess { case newId =>
      this.id = Option(newId)
      sn publish MShopSavedEvent(this)
    }
    fut
  }

  /** Обновить текстовые переменные текущего класса с помощью другого класса.
    * @param newMshop Другой экземпляр MShop, содержащий все необходимые данные.
    */
  def loadStringsFrom(newMshop: MShop) {
    if (!(newMshop eq this)) {
      this.companyId = newMshop.companyId
      this.martId = newMshop.martId
      this.name = newMshop.name
      this.description = newMshop.description
      this.martFloor = newMshop.martFloor
      this.martSection = newMshop.martSection
      this.settings.supLShopMaxAdsShown = newMshop.settings.supLShopMaxAdsShown
    }
  }

  def getAllShowLevels: Set[AdShowLevel] = {
    if (settings.supIsEnabled)
      settings.supWithLevels ++ MMartAd.SHOP_ALWAYS_SHOW_LEVELS
    else
      Set.empty
  }

  def saveShopLevels(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[_] = {
    MShop.setShowLevels(this)
  }


  def hasTopLevelAccess = settings.supWithLevels contains AdShowLevels.LVL_MART_SHOWCASE
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


object MShopSettings {

  /** Дефолтовое максимальное кол-во отображаемых карточек в магазине. */
  val MAX_LSHOP_ADS = CONFIG.getInt("mshop.settings.level.shop.shown.max.dflt") getOrElse 2

}

/** Представление распарсенных настроек MShop. */
case class MShopSettings(
  var supIsEnabled: Boolean = true,
  var supDisableReason: Option[String] = None,
  var supWithLevels: Set[AdShowLevel] = Set.empty,
  var supLShopMaxAdsShown: Int = MShopSettings.MAX_LSHOP_ADS
  // !!! Перед добавлением новых полей, надо сначало добавить их в маппинг, а только потом внедрять поле !!!
) {

  /** writer json'а в аккумулятор. Сеттинги записываются прямо в текущем объекте по ключам "setting.*". */
  def writeXContent(acc: XContentBuilder) {
    acc.field(SETTING_SUP_IS_ENABLED, supIsEnabled)
    if (supDisableReason.isDefined)
      acc.field(SETTING_SUP_DISABLE_REASON, supDisableReason.get)
    if (!supWithLevels.isEmpty) {
      acc.startArray(SETTING_SUP_WITH_LEVELS)
      supWithLevels.foreach { i =>
        acc.value(i.toString)
      }
      acc.endArray()
    }
    acc.field(SETTING_SUP_LSHOP_SHOWN_ADS_MAX, supLShopMaxAdsShown)
  }
}

