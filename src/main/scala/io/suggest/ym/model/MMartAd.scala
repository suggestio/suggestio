package io.suggest.ym.model

import io.suggest.model._
import org.elasticsearch.common.xcontent.{XContentFactory, XContentBuilder}
import io.suggest.util.SioEsUtil._
import io.suggest.util.SioConstants._
import EsModel._
import io.suggest.util.{MacroLogsImpl, JacksonWrapper}
import MShop.ShopId_t, MMart.MartId_t
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.QueryBuilders
import io.suggest.event.{AdSavedEvent, SioNotifierStaticClientI}
import scala.collection.JavaConversions._
import scala.util.{Failure, Success}
import io.suggest.model.inx2.MMartInx
import org.elasticsearch.action.search.SearchResponse

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

  val PICTURE_ESFN      = "picture"
  val OFFER_ESFN        = "offers"
  val VENDOR_ESFN       = "vendor"
  val MODEL_ESFN        = "model"
  val PRICE_ESFN        = "price"
  val OLD_PRICE_ESFN    = "oldPrice"
  val PANEL_ESFN        = "panel"
  // Категория по дефолту задана через id. Но при индексации заполняется ещё str, который include in all и помогает в поиске.
  val USER_CAT_ID_ESFN  = "userCat.id"
  val USER_CAT_STR_ESFN = "userCat.str"
  val SHOW_LEVELS_ESFN  = "showLevels"

  val FONT_ESFN         = "font"
  val SIZE_ESFN         = "size"
  val COLOR_ESFN        = "color"
  val TEXT_ALIGN_ESFN   = "textAlign"
  val ALIGN_ESFN        = "align"

  val TEXT1_ESFN        = "text1"
  val TEXT2_ESFN        = "text2"
  val DISCOUNT_ESFN     = "discount"
  val DISCOUNT_TPL_ESFN = "discoTpl"

  def dummy(id: String) = {
    MMartAd(
      id = Option(id),
      offers = Nil,
      picture = null,
      martId = null,
      companyId = null,
      shopId = null,
      textAlign = null
    )
  }

  private def shopIdQuery(shopId: ShopId_t) = QueryBuilders.termQuery(SHOP_ID_ESFN, shopId)

  /**
   * Найти все рекламные карточки магазина.
   * @param shopId id магазина
   * @return Список результатов.
   */
  def findForShop(shopId: ShopId_t)(implicit ec: ExecutionContext, client: Client): Future[Seq[MMartAd]] = {
    client.prepareSearch(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
      .setQuery(shopIdQuery(shopId))
      .execute()
      .map { searchResp2list }
  }

  def applyKeyValue(acc: MMartAd): PartialFunction[(String, AnyRef), Unit] = {
    case (MART_ID_ESFN, value)      => acc.martId = martIdParser(value)
    case (SHOP_ID_ESFN, value)      => acc.shopId = Option(shopIdParser(value))
    case (COMPANY_ID_ESFN, value)   => acc.companyId = companyIdParser(value)
    case (PICTURE_ESFN, value)      => acc.picture = stringParser(value)
    case (PRIO_ESFN, value)         => acc.prio = Option(intParser(value))
    case ("userCatId", value)       => acc.userCatId = Option(stringParser(value))    // TODO Удалить после 2014.apr.01
    case (USER_CAT_ID_ESFN, value)  => acc.userCatId = Option(stringParser(value))
    case (OFFER_ESFN, value: java.util.ArrayList[_]) =>
      acc.offers = value.toList.map {
        case jsObject: java.util.HashMap[_, _] =>
          if (jsObject containsKey VENDOR_ESFN)
            JacksonWrapper.convert[MMartAdProduct](jsObject)
          else if (jsObject containsKey DISCOUNT_ESFN)
            JacksonWrapper.convert[MMartAdDiscount](jsObject)
          else ???
      }
    case (PANEL_ESFN, value)        => acc.panel = Option(JacksonWrapper.convert[MMartAdPanelSettings](value))
    case (TEXT_ALIGN_ESFN, value)   => acc.textAlign = JacksonWrapper.convert[MMartAdTextAlign](value)
    case (SHOW_LEVELS_ESFN, sls: java.lang.Iterable[_]) =>
      acc.showLevels = AdShowLevels.deserializeLevelsFrom(sls)
  }

  def generateMappingStaticFields = List(
    FieldAll(enabled = true, analyzer = FTS_RU_AN),
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
    val offerField = FieldNestedObject(OFFER_ESFN,  enabled = true, properties = Seq(
      // product-поля
      FieldObject(VENDOR_ESFN, properties = Seq(stringValueField(1.5F), fontField)),
      FieldObject(MODEL_ESFN, properties = Seq(stringValueField(), fontField)),
      // TODO нужно как-то проанализировать цифры эти, округлять например.
      FieldObject(PRICE_ESFN,  properties = Seq(floatValueField(iia = true), fontField)),
      FieldObject(OLD_PRICE_ESFN,  properties = Seq(floatValueField(iia = false), fontField)),
      // discount-поля
      FieldObject(TEXT1_ESFN, properties = Seq(stringValueField(1.1F), fontField)),
      FieldObject(DISCOUNT_ESFN, properties = Seq(floatValueField(iia = true), fontField)),
      FieldObject(DISCOUNT_TPL_ESFN, enabled = false, properties = Nil),
      FieldObject(TEXT2_ESFN, properties = Seq(stringValueField(0.9F), fontField))
    ))
    List(
      FieldString(COMPANY_ID_ESFN,  index = FieldIndexingVariants.no,  include_in_all = false),
      FieldString(MART_ID_ESFN, index = FieldIndexingVariants.not_analyzed,  include_in_all = false),
      FieldString(SHOP_ID_ESFN, index = FieldIndexingVariants.not_analyzed,  include_in_all = false),
      FieldString(PICTURE_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
      FieldObject(TEXT_ALIGN_ESFN,  enabled = false,  properties = Nil),
      FieldString(USER_CAT_ID_ESFN, include_in_all = false, index = FieldIndexingVariants.not_analyzed),
      FieldString(USER_CAT_STR_ESFN, include_in_all = true, boost = Some(0.5F), index = FieldIndexingVariants.no),
      FieldObject(PANEL_ESFN,  enabled = false,  properties = Nil),
      FieldNumber(PRIO_ESFN,  fieldType = DocFieldTypes.integer,  index = FieldIndexingVariants.not_analyzed,  include_in_all = false),
      offerField,
      FieldString(SHOW_LEVELS_ESFN, include_in_all = false, index = FieldIndexingVariants.not_analyzed)
    )
  }

  /**
   * Прочитать pictureId для указанного элемента.
   * @param adId id рекламного документа.
   * @return id картинки, если такая реклама вообще существует.
   */
  def getPictureIdFor(adId: String)(implicit ec: ExecutionContext, client: Client): Future[Option[String]] = {
    client.prepareGet(ES_INDEX_NAME, ES_TYPE_NAME, adId)
      .setFields(PICTURE_ESFN)
      .execute()
      .map { getResp =>
        Option(getResp.getField(PICTURE_ESFN))
          .map(_.getValue.asInstanceOf[String])
      }
  }

  /**
   * Удалить документ по id.
   * @param id id документа.
   * @return true, если документ найден и удалён. Если не найден, то false
   */
  override def deleteById(id: String)(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Boolean] = {
    // удалить связанную картинку из хранилища
    val getPictFut = getPictureIdFor(id)
    getPictFut onSuccess {
      case Some(pictureId) =>
        MPict.deleteFully(pictureId) onComplete {
          case Success(_)  => trace("Successfuly erased picture: " + pictureId)
          case Failure(ex) => error("Failed to delete associated picture: " + pictureId, ex)
        }

      case None => debug("PictureId unexpectedly not found for adId = " + id)
    }
    getPictFut flatMap { _ =>
      super.deleteById(id)
    }
  }


  /**
   * Обновить допустимые уровни отображения рекламы на указанное значение.
   * @param adId id рекламы.
   * @param showLevels Новое значение showLevels.
   * @return Фьючерс для синхронизации.
   */
  def setShowLevels(adId: String, showLevels: collection.Set[AdShowLevel])(implicit ec: ExecutionContext, client: Client): Future[_] = {
    val newDocFieldsXCB = XContentFactory.jsonBuilder()
      .startObject()
      .startArray(SHOW_LEVELS_ESFN)
    showLevels.foreach { sl =>
      newDocFieldsXCB.value(sl.toString)
    }
    newDocFieldsXCB.endArray()
    client.prepareUpdate(ES_INDEX_NAME, ES_TYPE_NAME, adId)
      .setDoc(newDocFieldsXCB)
      .execute()
  }

}

import MMartAd._

/**
 * Экземпляр модели.
 * @param offers Список рекламных офферов (как правило из одного элемента). Используется прямое кодирование в json
 *               без промежуточных YmOfferDatum'ов. Поля оффера также хранят в себе данные о своём дизайне.
 * @param picture id картинки.
 * @param prio Приоритет. На первом этапе null или минимальное значение для обозначения главного и вторичных плакатов.
 * @param userCatId Индексируемые данные по категории рекламируемого товара.
 * @param companyId id компании-владельца в рамках модели MCompany.
 * @param showLevels Список уровней, на которых должна отображаться эта реклама.
 * @param id id товара.
 */
case class MMartAd(
  var martId      : MartId_t,
  var offers      : List[MMartAdOfferT],
  var picture     : String,
  var textAlign   : MMartAdTextAlign,
  var shopId      : Option[ShopId_t] = None,
  var companyId   : MCompany.CompanyId_t,
  var panel       : Option[MMartAdPanelSettings] = None,
  var prio        : Option[Int] = None,
  var showLevels  : Set[AdShowLevel] = Set.empty,
  var userCatId   : Option[String] = None,
  var id          : Option[String] = None
) extends MMartAdT[MMartAd] {
  def companion = MMartAd

  def writeJsonFields(acc: XContentBuilder) {
    acc.field(MART_ID_ESFN, martId)
      .field(PICTURE_ESFN, picture)
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
      acc.startArray(OFFER_ESFN)
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
    // TextAlign. Reflections из-за проблем с XCB.
    acc.rawField(TEXT_ALIGN_ESFN, JacksonWrapper.serialize(textAlign).getBytes())
  }


  /** Перед сохранением можно проверять состояние экземпляра. */
  override def isFieldsValid: Boolean = {
    super.isFieldsValid &&
      picture != null && !offers.isEmpty && shopId != null && companyId != null && martId != null
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
}

/** Интерфейс экземпляра модели для возможности создания классов-врапперов. */
trait MMartAdT[T <: MMartAdT[T]] extends EsModelT[T] {
  def martId      : MartId_t
  def offers      : List[MMartAdOfferT]
  def picture     : String
  def textAlign   : MMartAdTextAlign
  def shopId      : Option[ShopId_t]
  def companyId   : MCompany.CompanyId_t
  def panel       : Option[MMartAdPanelSettings]
  def prio        : Option[Int]
  def showLevels  : Set[AdShowLevel]
  def userCatId   : Option[String]
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
  def picture = mmartAd.picture
  def offers = mmartAd.offers
  def martId = mmartAd.martId
  def id = mmartAd.id

  def writeJsonFields(acc: XContentBuilder) = mmartAd.writeJsonFields(acc)
  def companion: EsModelMinimalStaticT[T] = mmartAd.companion
  override def isFieldsValid: Boolean = super.isFieldsValid && mmartAd.isFieldsValid
}


trait MMartAdOfferT extends Serializable {
  def isProduct: Boolean
  def renderFields(acc: XContentBuilder)
  def renderJson(acc: XContentBuilder) {
    acc.startObject()
    renderFields(acc)
    acc.endObject()
  }
}

case class MMartAdProduct(
  vendor:   MMAdStringField,
  price:    MMAdFloatField,
  oldPrice: Option[MMAdFloatField]
) extends MMartAdOfferT {

  def isProduct = true

  def renderFields(acc: XContentBuilder) {
    acc.field(VENDOR_ESFN)
    vendor.render(acc)
    if (oldPrice.isDefined) {
      acc.field(OLD_PRICE_ESFN)
      oldPrice.get.render(acc)
    }
    acc.field(PRICE_ESFN)
    price.render(acc)
  }
}

case class MMartAdDiscount(
  text1: Option[MMAdStringField],
  discount: MMAdFloatField,
  template: DiscountTemplate,
  text2: Option[MMAdStringField]
) extends MMartAdOfferT {

  def isProduct = false

  def renderFields(acc: XContentBuilder) {
    if (text1.isDefined) {
      acc.field(TEXT1_ESFN)
      text1.get.render(acc)
    }
    acc.field(DISCOUNT_ESFN)
    discount.render(acc)
    template.render(acc)
    if (text2.isDefined) {
      acc.field(TEXT2_ESFN)
      text2.get.renderFields(acc)
    }
  }
}

case class DiscountTemplate(id: Int, color: String) {
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


/** MMartAdIndexed - экспорт-модель для MMartAd. Нужна для сохранения MMartAd при индексации. */
object MMartAdIndexed extends MacroLogsImpl {
  import LOGGER._

  private def dummy(id: String, inx2: MMartInx) = MMartAdIndexed(MMartAd.dummy(id), null, inx2)

  /**
   * Десериализация одного элементам модели.
   * @param id id документа.
   * @param m Карта, распарсенное json-тело документа.
   * @return Экземпляр модели.
   */
  private def deserializeOne(id: String, m: collection.Map[String, AnyRef], inx2: MMartInx): MMartAdIndexed = {
    val acc = dummy(id, inx2)
    m foreach applyKeyValue(acc)
    acc
  }

  def applyKeyValue(acc: MMartAdIndexed): PartialFunction[(String, AnyRef), Unit] = {
    // Собираем partial-функцию, которая будет всё делать как надо. Чтобы типы аккамуляторов и врапперов были совместимы, тут небольшой велосипед:
    val fm = MMartAd.applyKeyValue(acc.mmartAd)
    val pf: PartialFunction[(String, AnyRef), Unit] = {
      case (USER_CAT_STR_ESFN, value) => acc.userCatStr = Option(stringParser(value))
      case other => fm(other)
    }
    pf
  }

  /**
   * Выбрать ряд из таблицы по id.
   * @param id Ключ магазина.
   * @return Экземпляр сабжа, если такой существует.
   */
  def getById(id: String, inx2: MMartInx)(implicit ec:ExecutionContext, client: Client): Future[Option[MMartAdIndexed]] = {
    val maybeRk = getRoutingKey(id)
    val req = client.prepareGet(inx2.targetEsInxName, inx2.esType, id)
    if (maybeRk.isDefined)
      req.setRouting(maybeRk.get)
    req.execute()
      .map { getResp =>
        if (getResp.isExists) {
          val result = deserializeOne(getResp.getId, getResp.getSourceAsMap, inx2)
          Some(result)
        } else {
          None
        }
      }
  }

  /** Список результатов с source внутри перегнать в распарсенный список. */
  protected def searchResp2list(searchResp: SearchResponse, inx2: MMartInx): Seq[MMartAdIndexed] = {
    searchResp.getHits.getHits.toSeq.map { hit =>
      deserializeOne(hit.getId, hit.getSource, inx2)
    }
  }


  /**
   * Выдать все магазины. Метод подходит только для административных задач.
   * @return Список магазинов в порядке их создания.
   */
  def getAll(inx2: MMartInx)(implicit ec:ExecutionContext, client: Client): Future[Seq[MMartAdIndexed]] = {
    client.prepareSearch(inx2.targetEsInxName)
      .setTypes(inx2.esType)
      .setQuery(QueryBuilders.matchAllQuery())
      .execute()
      .map { searchResp2list(_, inx2) }
  }


  /**
   * Удалить документ по id.
   * @param id id документа.
   * @return true, если документ найден и удалён. Если не найден, то false
   */
  def deleteById(id: String, inx2: MMartInx)(implicit ec:ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Boolean] = {
    client.prepareDelete(inx2.targetEsInxName, inx2.esType, id)
      .execute()
      .map { _.isFound }
  }

}

/**
 * Экземпляр хорошо индексируемого [[MMartAd]]. Обладает полями, содержащими данные об индексе и индексируемом
 * названии категории.
 * @param mmartAd Исходный [[MMartAd]].
 * @param userCatStr Строка, собранная из названий индексируемых категорий.
 * @param inx2 Данные об используемом индексе. НЕ сохраняются в БД.
 */
case class MMartAdIndexed(
  mmartAd: MMartAd,
  var userCatStr: Option[String],
  inx2: MMartInx
) extends MMartAdWrapperT[MMartAd] {

  override def isFieldsValid: Boolean = super.isFieldsValid && inx2 != null

  override def writeJsonFields(acc: XContentBuilder) {
    super.writeJsonFields(acc)
    if (userCatStr.isDefined)
      acc.field(USER_CAT_STR_ESFN, userCatStr.get)
  }

  override def esIndexName: String = inx2.targetEsInxName
  override def esTypeName: String  = inx2.esTypeName

  /**
   * Удалить текущий ряд из таблицы. Если ключ не выставлен, то сразу будет экзепшен.
   * @return true - всё ок, false - документ не найден.
   */
  override def delete(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Boolean] = {
    MMartAdIndexed.deleteById(id.get, inx2)
  }

}

