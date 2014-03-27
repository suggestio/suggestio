package io.suggest.ym.model

import io.suggest.model._
import org.elasticsearch.common.xcontent.{XContentFactory, XContentBuilder}
import io.suggest.util.SioEsUtil._
import EsModel._
import io.suggest.util.{MacroLogsImpl, JacksonWrapper}
import MShop.ShopId_t, MMart.MartId_t
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.{FilterBuilders, QueryBuilder, QueryBuilders}
import io.suggest.event.{AdDeletedEvent, AdSavedEvent, SioNotifierStaticClientI}
import scala.collection.JavaConversions._
import scala.util.{Failure, Success}
import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonIgnore}
import org.elasticsearch.action.update.UpdateResponse
import org.elasticsearch.search.sort.SortOrder
import org.joda.time.DateTime
import com.github.nscala_time.time.OrderingImplicits._
import java.util.Currency
import io.suggest.util.SioConstants.CURRENCY_CODE_DFLT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.03.14 18:30
 * Description: Рекламные "плакаты" в торговом центре.
 * prio используется для задания приоритета в отображении в рамках магазина. На текущий момент там всё просто:
 * если null, то приоритета нет, если 1 то он есть.
 */
object MMartAd extends EsModelStaticT[MMartAd] with MacroLogsImpl {

  import LOGGER._

  val ES_TYPE_NAME      = "martAd"

  val IMG_ESFN          = "img"
  val OFFERS_ESFN       = "offers"
  val OFFER_BODY_ESFN   = "offerBody"
  val VENDOR_ESFN       = "vendor"
  val MODEL_ESFN        = "model"
  val PRICE_ESFN        = "price"
  val OLD_PRICE_ESFN    = "oldPrice"
  val CURRENCY_CODE_ESFN = "currencyCode"
  val PANEL_ESFN        = "panel"
  // Категория по дефолту задана через id. Но при индексации заполняется ещё str, который include in all и помогает в поиске.
  val USER_CAT_ID_ESFN  = "userCat.id"
  val SHOW_LEVELS_ESFN  = "showLevels"
  val OFFER_TYPE_ESFN   = "offerType"

  val FONT_ESFN         = "font"
  val SIZE_ESFN         = "size"
  val COLOR_ESFN        = "color"
  val TEXT_ALIGN_ESFN   = "textAlign"
  val ALIGN_ESFN        = "align"

  val TEXT_ESFN         = "text"
  val TEXT1_ESFN        = "text1"
  val TEXT2_ESFN        = "text2"
  val DISCOUNT_ESFN     = "discount"
  val DISCOUNT_TPL_ESFN = "discoTpl"


  /** Перманентные уровни отображения для рекламных карточек магазина. Если магазин включен, то эти уровни всегда доступны. */
  val SHOP_ALWAYS_SHOW_LEVELS: Set[AdShowLevel] = Set(AdShowLevels.LVL_SHOP, AdShowLevels.LVL_MART_SHOPS)

  /** Список уровней, которые могут быть активны только у одной карточки в рамках магазина. */
  val SHOP_LEVELS_SINGLETON: Set[AdShowLevel] = Set(AdShowLevels.LVL_MART_SHOWCASE, AdShowLevels.LVL_MART_SHOPS)

  /** Перманентные уровни отображения для рекламных карточек ТЦ. */
  val MART_ALWAYS_SHOW_LEVELS: Set[AdShowLevel] = Set(AdShowLevels.LVL_MART_SHOWCASE)

  def dummy(id: String) = {
    MMartAd(
      id = Option(id),
      offers = Nil,
      img = null,
      martId = null,
      companyId = null,
      shopId = None,
      textAlign = null
    )
  }

  def shopIdQuery(shopId: ShopId_t) = QueryBuilders.termQuery(SHOP_ID_ESFN, shopId)

  /**
   * Найти все рекламные карточки магазина.
   * @param shopId id магазина
   * @return Список результатов.
   */
  def findForShop(shopId: ShopId_t)(implicit ec: ExecutionContext, client: Client): Future[Seq[MMartAd]] = {
    client.prepareSearch(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
      .setQuery(shopIdQuery(shopId))
      .addSort(DATE_CREATED_ESFN, SortOrder.DESC)
      .execute()
      .map { searchResp2list }
  }


  /**
   * Найти все рекламные карточки магазина с поправкой на реалтаймовое обновление индекса.
   * @param shopId id магазина
   * @return Список результатов.
   */
  def findForShopRt(shopId: ShopId_t)(implicit ec: ExecutionContext, client: Client): Future[List[MMartAd]] = {
    findRt(shopIdQuery(shopId))
  }

  // TODO Почему-то сортировка работает задом наперёд, и reverse не требует.
  private def sortResults(mads: List[MMartAd]) = mads.sortBy(_.dateCreated)

  /**
   * Реалтаймовый поиск карточек в рамках ТЦ для отображения в ЛК ТЦ.
   * @param martId id ТЦ
   * @param shopMustMiss true, если нужно найти карточки, не относящиеся к магазинам. Т.е. собственные
   *                     карточки ТЦ.
   *                     false - в выдачу также попадут карточки магазинов.
   * @return Список карточек, относящихся к ТЦ.
   */
  def findForMartRt(martId: MartId_t, shopMustMiss: Boolean)(implicit ec: ExecutionContext, client: Client): Future[List[MMartAd]] = {
    var query: QueryBuilder = QueryBuilders.termQuery(MART_ID_ESFN, martId)
    if (shopMustMiss) {
      val shopMissingFilter = FilterBuilders.missingFilter(SHOP_ID_ESFN)
      query = QueryBuilders.filteredQuery(query, shopMissingFilter)
    }
    findRt(query)
  }


  /** common-функция для запросов в реальном времени. */
  private def findRt(query: QueryBuilder)(implicit ec: ExecutionContext, client: Client): Future[List[MMartAd]] = {
    client.prepareSearch(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
      .setQuery(query)
      .setNoFields()
      .execute()
      .flatMap { searchResp2RtMultiget }
      .map { sortResults }
  }


  def applyKeyValue(acc: MMartAd): PartialFunction[(String, AnyRef), Unit] = {
    case (MART_ID_ESFN, value)      => acc.martId = martIdParser(value)
    case (SHOP_ID_ESFN, value)      => acc.shopId = Option(shopIdParser(value))
    case (COMPANY_ID_ESFN, value)   => acc.companyId = companyIdParser(value)
    case (PRIO_ESFN, value)         => acc.prio = Option(intParser(value))
    case (USER_CAT_ID_ESFN, value)  => acc.userCatId = Option(stringParser(value))
    case (OFFERS_ESFN, value: java.util.ArrayList[_]) =>
      acc.offers = value.toList.map {
        case jsObject: java.util.HashMap[_, _] =>
          jsObject.get(OFFER_TYPE_ESFN) match {
            case ots: String =>
              MMartAdOfferTypes.maybeWithName(ots) match {
                case Some(ot) =>
                  val offerBody = jsObject.get(OFFER_BODY_ESFN)
                  import MMartAdOfferTypes._
                  ot match {
                    case PRODUCT  => MMartAdProduct.deserialize(offerBody)
                    case DISCOUNT => MMartAdDiscount.deserialize(offerBody)
                    case TEXT     => MMartAdText.deserialize(offerBody)
                  }

                case None => ???
              }
            // совместимость со старыми объектами, когда не было поля типа оффера. TODO Удалить после 2014.mart.25
            case null =>
              if (jsObject containsKey VENDOR_ESFN)
                JacksonWrapper.convert[MMartAdProduct](jsObject)
              else if (jsObject containsKey DISCOUNT_ESFN)
                JacksonWrapper.convert[MMartAdDiscount](jsObject)
              else ???
          }

      }
    case (PANEL_ESFN, value)        => acc.panel = Option(JacksonWrapper.convert[MMartAdPanelSettings](value))
    case (TEXT_ALIGN_ESFN, value)   => acc.textAlign = JacksonWrapper.convert[MMartAdTextAlign](value)
    case (SHOW_LEVELS_ESFN, sls: java.lang.Iterable[_]) =>
      acc.showLevels = AdShowLevels.deserializeLevelsFrom(sls)
    case (DATE_CREATED_ESFN, value) => acc.dateCreated = dateCreatedParser(value)
    case ("picture", value)         => acc.img = MImgInfo(stringParser(value))  // TODO Удалить после сброса индексов после 26.mar.2014
    case (IMG_ESFN, value)          =>
      acc.img = JacksonWrapper.convert[MImgInfo](value)
  }

  def generateMappingStaticFields = List(
    FieldAll(enabled = true),
    FieldSource(enabled = true)
  )

  /** Генератор пропертисов для маппигов индексов этой модели. */
  def generateMappingProps: List[DocField] = {
    val fontField = FieldObject(FONT_ESFN, enabled = false, properties = Nil)
    def stringValueField(boost: Float = 1.0F) = FieldString(
      VALUE_ESFN,
      index = FieldIndexingVariants.no,
      include_in_all = true,
      boost = Some(boost)
    )
    def floatValueField(iia: Boolean) = {
      FieldNumber(VALUE_ESFN,  fieldType = DocFieldTypes.float,  index = FieldIndexingVariants.no,  include_in_all = iia)
    }
    // Поле приоритета. На первом этапе null или число.
    val offerBodyProps = Seq(
      // product-поля
      FieldObject(VENDOR_ESFN, properties = Seq(stringValueField(1.5F), fontField)),
      FieldObject(MODEL_ESFN, properties = Seq(stringValueField(), fontField)),
      // TODO нужно как-то проанализировать цифры эти, округлять например.
      FieldObject(PRICE_ESFN,  properties = Seq(floatValueField(iia = true), fontField)),
      FieldObject(OLD_PRICE_ESFN,  properties = Seq(floatValueField(iia = false), fontField)),
      FieldString(CURRENCY_CODE_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
      // discount-поля
      FieldObject(TEXT1_ESFN, properties = Seq(stringValueField(1.1F), fontField)),
      FieldObject(DISCOUNT_ESFN, properties = Seq(floatValueField(iia = true), fontField)),
      FieldObject(DISCOUNT_TPL_ESFN, enabled = false, properties = Nil),
      FieldObject(TEXT2_ESFN, properties = Seq(stringValueField(0.9F), fontField)),
      // text-поля
      FieldObject(TEXT_ESFN, properties = Seq(
        // HTML будет пострипан тут автоматом.
        FieldString(VALUE_ESFN, index = FieldIndexingVariants.no, include_in_all = true),
        stringValueField(),
        fontField
      ))
    )
    val offersField = FieldNestedObject(OFFERS_ESFN, enabled = true, properties = Seq(
      FieldString(OFFER_TYPE_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldObject(OFFER_BODY_ESFN, enabled = true, properties = offerBodyProps)
    ))
    List(
      FieldString(COMPANY_ID_ESFN,  index = FieldIndexingVariants.no,  include_in_all = false),
      FieldString(MART_ID_ESFN, index = FieldIndexingVariants.not_analyzed,  include_in_all = false),
      FieldString(SHOP_ID_ESFN, index = FieldIndexingVariants.not_analyzed,  include_in_all = false),
      FieldObject(IMG_ESFN, enabled = false, properties = Nil),
      FieldObject(TEXT_ALIGN_ESFN,  enabled = false,  properties = Nil),
      FieldString(USER_CAT_ID_ESFN, include_in_all = false, index = FieldIndexingVariants.not_analyzed),
      FieldObject(PANEL_ESFN,  enabled = false,  properties = Nil),
      FieldNumber(PRIO_ESFN,  fieldType = DocFieldTypes.integer,  index = FieldIndexingVariants.not_analyzed,  include_in_all = false),
      offersField,
      FieldString(SHOW_LEVELS_ESFN, include_in_all = false, index = FieldIndexingVariants.not_analyzed),
      FieldDate(DATE_CREATED_ESFN, include_in_all = false, index = FieldIndexingVariants.no)
    )
  }


  /**
   * Удалить документ по id.
   * @param id id документа.
   * @return true, если документ найден и удалён. Если не найден, то false
   */
  override def deleteById(id: String)(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Boolean] = {
    // удалить связанную картинку из хранилища
    val adOptFut = getById(id)
    adOptFut flatMap {
      case Some(ad) =>
        val imgId = ad.img.id
        MPict.deleteFully(imgId) onComplete {
          case Success(_)  => trace("Successfuly erased picture: " + imgId)
          case Failure(ex) => error("Failed to delete associated picture: " + imgId, ex)
        }
        val resultFut = super.deleteById(id)
        resultFut onSuccess { case _ =>
          sn publish AdDeletedEvent(ad)
        }
        resultFut

      case None => Future successful false
    }
  }


  /** Для апдейта уровней необходимо использовать json, описывающий изменённые поля документа. Тут идёт сборка такого JSON. */
  private def mkLevelsUpdateDoc(newLevels: Iterable[AdShowLevel]): XContentBuilder = {
    val newDocFieldsXCB = XContentFactory.jsonBuilder()
      .startObject()
      .startArray(SHOW_LEVELS_ESFN)
    newLevels.foreach { sl =>
      newDocFieldsXCB.value(sl.toString)
    }
    newDocFieldsXCB.endArray().endObject()
  }

  /**
   * Обновить допустимые уровни отображения рекламы на указанное значение.
   * @return Фьючерс для синхронизации.
   */
  private def setShowLevels(thisAd: MMartAd)(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[_] = {
    // Если в новый уровнях нет уровней, относящихся к singleton-уровням, то обновляем по-быстрому.
    val singletonLevels = thisAd.showLevels intersect SHOP_LEVELS_SINGLETON
    val adId = thisAd.id.get
    if (!thisAd.isShopAd || singletonLevels.isEmpty) {
      val resultFut: Future[UpdateResponse] = client.prepareUpdate(ES_INDEX_NAME, ES_TYPE_NAME, adId)
        .setDoc(mkLevelsUpdateDoc(thisAd.showLevels))
        .execute()
      // Уведомить всех о том, что в текущей рекламе были изменения.
      resultFut onSuccess { case updateResp =>
        sn publish AdSavedEvent(thisAd)
      }
      resultFut
    } else {
      trace(s"setShowLevels(${thisAd.id.get}): Singleton level(s) enabled: ${singletonLevels.mkString(", ")}")
      // Это магазинная реклама, и в текущем adId есть [новые] уровни, которые затрагивают singleton-ограничение. Нужно сделать апдейт во всех рекламах магазина, убрав их оттуда.
      findForShop(thisAd.shopId.get) flatMap { allShopsAds =>
        val (brb, mads) = allShopsAds.iterator
          .map { mad =>
            val lvls1 = if (mad.idOrNull == adId) {
              thisAd.showLevels
            } else {
              mad.showLevels -- singletonLevels
            }
            mad.showLevels = lvls1
            val urb = client.prepareUpdate(mad.esIndexName, mad.esTypeName, mad.id.get)
              .setDoc(mkLevelsUpdateDoc(lvls1))
            mad -> urb
          }.foldLeft (client.prepareBulk() -> List.empty[MMartAd]) {
            case ((bulk1, madsAcc), (mad, urb))  =>  bulk1.add(urb) -> (mad :: madsAcc)
          }
        val resultFut = laFuture2sFuture(brb.execute())
        resultFut onSuccess { case bulkResp =>
          // Сообщить всем, что имело место обновления записей.
          mads foreach { mad =>
            sn publish AdSavedEvent(mad)
          }
        }
        resultFut
      }
    }
  }

}

import MMartAd._

/**
 * Экземпляр модели.
 * @param offers Список рекламных офферов (как правило из одного элемента). Используется прямое кодирование в json
 *               без промежуточных YmOfferDatum'ов. Поля оффера также хранят в себе данные о своём дизайне.
 * @param img Данные по используемой картинке.
 * @param prio Приоритет. На первом этапе null или минимальное значение для обозначения главного и вторичных плакатов.
 * @param userCatId Индексируемые данные по категории рекламируемого товара.
 * @param companyId id компании-владельца в рамках модели MCompany.
 * @param showLevels Список уровней, на которых должна отображаться эта реклама.
 * @param id id товара.
 */
case class MMartAd(
  var martId      : MartId_t,
  var offers      : List[MMartAdOfferT],
  var img         : MImgInfo,
  var textAlign   : MMartAdTextAlign,
  var shopId      : Option[ShopId_t] = None,
  var companyId   : MCompany.CompanyId_t,
  var panel       : Option[MMartAdPanelSettings] = None,
  var prio        : Option[Int] = None,
  var showLevels  : Set[AdShowLevel] = Set.empty,
  var userCatId   : Option[String] = None,
  var id          : Option[String] = None,
  var dateCreated : DateTime = DateTime.now
) extends MMartAdT[MMartAd] {

  def companion = MMartAd

  /** Перед сохранением можно проверять состояние экземпляра. */
  @JsonIgnore override def isFieldsValid: Boolean = {
    super.isFieldsValid &&
      img != null && !offers.isEmpty && shopId != null && companyId != null && martId != null
  }


  /** Можно делать какие-то действия после десериализации. Например, можно исправлять значения после эволюции схемы. */
  override def postDeserialize(): Unit = {
    super.postDeserialize()
    if (img == null) {
      img = MImgInfo("BROKEN_DATA")
    }
  }

  /**
   * Сохранить экземпляр в хранилище ES и сгенерить уведомление, если всё ок.
   * @return Фьючерс с новым/текущим id
   */
  override def save(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[String] = {
    val resultFut = super.save
    resultFut onSuccess { case adId =>
      this.id = Option(adId)
      sn publish AdSavedEvent(this)
    }
    resultFut
  }

  /** Короткий враппер над статическим [[MMartAd.setShowLevels]]. */
  def saveShowLevels(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[_] = {
    MMartAd.setShowLevels(this)
  }
}


/** Интерфейс экземпляра модели для возможности создания классов-врапперов. */
trait MMartAdT[T <: MMartAdT[T]] extends EsModelT[T] {
  def martId      : MartId_t
  def offers      : List[MMartAdOfferT]
  def textAlign   : MMartAdTextAlign
  def shopId      : Option[ShopId_t]
  def companyId   : MCompany.CompanyId_t
  def panel       : Option[MMartAdPanelSettings]
  def prio        : Option[Int]
  def showLevels  : Set[AdShowLevel]
  def userCatId   : Option[String]
  def dateCreated : DateTime
  def img         : MImgInfo

  @JsonIgnore def isShopAd = shopId.isDefined

  def writeJsonFields(acc: XContentBuilder) {
    acc.field(MART_ID_ESFN, martId)
      .field(COMPANY_ID_ESFN, companyId)
    if (userCatId.isDefined)
      acc.field(USER_CAT_ID_ESFN, userCatId.get)
    if (prio.isDefined)
      acc.field(PRIO_ESFN, prio.get)
    if (shopId.isDefined)
      acc.field(SHOP_ID_ESFN, shopId.get)
    if (panel.isDefined)
      panel.get.render(acc)
    // Загружаем офферы
    if (!offers.isEmpty) {
      acc.startArray(OFFERS_ESFN)
        offers foreach { _ renderJson acc }
      acc.endArray()
    }
    if (!showLevels.isEmpty) {
      acc.startArray(SHOW_LEVELS_ESFN)
      showLevels.foreach { sl =>
        acc.value(sl.toString)
      }
      acc.endArray()
    }
    acc.rawField(IMG_ESFN, JacksonWrapper.serialize(img).getBytes)
    acc.field(DATE_CREATED_ESFN, dateCreated)
    // TextAlign. Reflections из-за проблем с XCB.
    acc.rawField(TEXT_ALIGN_ESFN, JacksonWrapper.serialize(textAlign).getBytes)
  }
}


/** Враппер для моделей [[MMartAdT]]. Позволяет легко и быстро написать wrap-модель над уже готовым
  * экземпляром [[MMartAdT]]. Полезно на экспорт-моделях, которые занимаются сохранением расширенных экземпляров
  * [[MMartAdT]] в другие ES-индексы. */
trait MMartAdWrapperT[T <: MMartAdT[T]] extends MMartAdT[T] {
  def mmartAd: MMartAdT[T]

  def userCatId = mmartAd.userCatId
  def showLevels = mmartAd.showLevels
  def prio = mmartAd.prio
  def panel = mmartAd.panel
  def companyId = mmartAd.companyId
  def shopId = mmartAd.shopId
  def textAlign = mmartAd.textAlign
  def offers = mmartAd.offers
  def martId = mmartAd.martId
  def id = mmartAd.id
  def dateCreated = mmartAd.dateCreated
  def img = mmartAd.img

  @JsonIgnore def companion: EsModelMinimalStaticT[T] = mmartAd.companion
  @JsonIgnore override def isFieldsValid: Boolean = super.isFieldsValid && mmartAd.isFieldsValid
}


sealed trait MMartAdOfferT extends Serializable {
  @JsonIgnore def offerType: MMartAdOfferType
  def renderJson(acc: XContentBuilder) {
    acc.startObject()
    acc.field(OFFER_TYPE_ESFN, offerType.toString)
    val offerBodyJson = JacksonWrapper.serialize(this)
    acc.rawField(OFFER_BODY_ESFN, offerBodyJson.getBytes)
    acc.endObject()
  }
}


// MImgInfo* надо бы вынести за пределы этой модели на уровне сорцов.
/** Объект содержит данные по картинке. Данные не индексируются, и их схему можно менять на лету. */
@JsonIgnoreProperties(ignoreUnknown = true)
case class MImgInfo(id: String, meta: Option[MImgInfoMeta] = None) {
  override def hashCode(): Int = id.hashCode()
}
case class MImgInfoMeta(height: Int, width: Int)


/** Известные системе типы офферов. */
object MMartAdOfferTypes extends Enumeration {
  type MMartAdOfferType = Value

  val PRODUCT   = Value("p")
  val DISCOUNT  = Value("d")
  val TEXT      = Value("t")

  def maybeWithName(n: String): Option[MMartAdOfferType] = {
    try {
      Some(withName(n))
    } catch {
      case ex: Exception => None
    }
  }
}


object MMartAdProduct {
  def deserialize(jsObject: Any) = JacksonWrapper.convert[MMartAdProduct](jsObject)
}

case class MMartAdProduct(
  vendor:   MMAdStringField,
  price:    MMAdFloatField,
  oldPrice: Option[MMAdFloatField],
  var currencyCode: String = CURRENCY_CODE_DFLT
) extends MMartAdOfferT {
  // TODO convert не подхватывает дефолтовые значения если валюта в json отсутствует, и получается null в currencyCode.
  //      Из-за этого var + костыль в конструкторе
  if (currencyCode == null)
    currencyCode = CURRENCY_CODE_DFLT

  @JsonIgnore def offerType = MMartAdOfferTypes.PRODUCT
  @JsonIgnore lazy val currency = Currency.getInstance(currencyCode)
}

object MMartAdDiscount {
  def deserialize(jsObject: Any) = JacksonWrapper.convert[MMartAdDiscount](jsObject)
}
case class MMartAdDiscount(
  text1: Option[MMAdStringField],
  discount: MMAdFloatField,
  template: MMartAdDiscountTemplate,
  text2: Option[MMAdStringField]
) extends MMartAdOfferT {
  @JsonIgnore def offerType = MMartAdOfferTypes.DISCOUNT
}

object MMartAdText {
  def deserialize(jsObject: Any) = JacksonWrapper.convert[MMartAdText](jsObject)
}
case class MMartAdText(text: MMAdStringField) extends MMartAdOfferT {
  @JsonIgnore def offerType = MMartAdOfferTypes.TEXT
}


case class MMartAdDiscountTemplate(id: Int, color: String) {
  def render(acc: XContentBuilder) {
    acc.startObject(DISCOUNT_TPL_ESFN)
      .field("id", id)
      .field(COLOR_ESFN, color)
    .startObject()
  }
}

sealed trait MMAdValueField {
  def renderFields(acc: XContentBuilder)
  def font: MMAdFieldFont
  def render(acc: XContentBuilder) {
    acc.startObject()
      renderFields(acc)
      font.render(acc)
    acc.endObject()
  }
}

case class MMAdStringField(value:String, font: MMAdFieldFont) extends MMAdValueField {
  def renderFields(acc: XContentBuilder) {
    acc.field(VALUE_ESFN, value)
  }
}
case class MMAdFloatField(value: Float, font: MMAdFieldFont) extends MMAdValueField {
  def renderFields(acc: XContentBuilder) {
    acc.field(VALUE_ESFN, value)
  }
}

case class MMAdFieldFont(color: String) {
  def render(acc: XContentBuilder) {
    acc.startObject(FONT_ESFN)
      .field(COLOR_ESFN, color)
    .endObject()
  }
}


case class MMartAdPanelSettings(color: String) {
  def render(acc: XContentBuilder) {
    acc.startObject(PANEL_ESFN)
      .field(COLOR_ESFN, color)
    .endObject()
  }
}


case class MMartAdTAPhone(align: String)
case class MMartAdTATablet(alignTop: String, alignBottom: String)
case class MMartAdTextAlign(phone: MMartAdTAPhone, tablet: MMartAdTATablet)


/** Допустимые значения textAlign-полей. */
object TextAlignValues extends Enumeration {
  type TextAlignValue = Value
  val left, right = Value

  def maybeWithName(n: String): Option[TextAlignValue] = {
    try {
      Some(withName(n))
    } catch {
      case _: Exception => None
    }
  }
}


/** Уровни отображения рекламы. Используется как bitmask, но через денормализацию поля. */
object AdShowLevels extends Enumeration with MacroLogsImpl {
  import LOGGER._
  import scala.collection.JavaConversions._

  type AdShowLevel = Value

  /** Отображать на нулевом уровне, т.е. при входе в магазин. */
  val LVL_MART_SHOWCASE = Value("d")

  /** Отображать на списке витрин ТЦ. */
  val LVL_MART_SHOPS = Value("h")

  /** Отображать эту рекламу внутри магазина. */
  val LVL_SHOP = Value("m")

  def maybeWithName(n: String): Option[AdShowLevel] = {
    try {
      Some(withName(n))
    } catch {
      case _: Exception => None
    }
  }

  /** Десериализатор значений из самых примитивных типов и коллекций. */
  val deserializeLevelsFrom: PartialFunction[Any, Set[AdShowLevel]] = {
    case v: java.lang.Iterable[_] =>
      v.foldLeft[List[AdShowLevel]] (Nil) { (acc, slRaw) =>
        AdShowLevels.maybeWithName(slRaw.toString) match {
          case Some(sl) => sl :: acc
          case None =>
            warn(s"Unable to deserialize show level '$slRaw'. Possible levels are: ${AdShowLevels.values.mkString(", ")}")
            acc
        }
      }.toSet
  }

}

