package models

import io.suggest.model._
import org.elasticsearch.common.xcontent.XContentBuilder
import io.suggest.util.SioEsUtil._
import io.suggest.util.SioConstants._
import EsModel._
import io.suggest.util.JacksonWrapper
import MShop.ShopId_t, MMart.MartId_t
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.QueryBuilders
import io.suggest.event.SioNotifierStaticClientI
import scala.util.{Failure, Success}
import util.PlayMacroLogsImpl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.03.14 18:30
 * Description: Рекламные "плакаты" в торговом центре.
 * prio используется для задания приоритета в отображении в рамках магазина. На текущий момент там всё просто:
 * если null, то приоритета нет, если 1 то он есть.
 */
object MMartAd extends EsModelStaticT[MMartAd] with PlayMacroLogsImpl {

  import LOGGER._

  val ES_TYPE_NAME      = "martAd"

  val PICTURE_ESFN      = "picture"
  val OFFER_ESFN        = "offers"
  val VENDOR_ESFN       = "vendor"
  val MODEL_ESFN        = "model"
  val PRICE_ESFN        = "price"
  val OLD_PRICE_ESFN    = "oldPrice"
  val PANEL_ESFN        = "panel"
  val USER_CAT_ID_ESFN  = "userCatId"

  val FONT_ESFN         = "font"
  val SIZE_ESFN         = "size"
  val COLOR_ESFN        = "color"
  val IS_SHOWN_ESFN     = "isShown"
  val TEXT_ALIGN_ESFN   = "textAlign"
  val ALIGN_ESFN        = "align"

  val TEXT1_ESFN        = "text1"
  val TEXT2_ESFN        = "text2"
  val DISCOUNT_ESFN     = "discount"
  val DISCOUNT_TPL_ESFN = "discoTpl"

  protected def dummy(id: String) = {
    MMartAd(
      id = Some(id),
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
    case (SHOP_ID_ESFN, value)      => acc.shopId = Some(shopIdParser(value))
    case (COMPANY_ID_ESFN, value)   => acc.companyId = companyIdParser(value)
    case (PICTURE_ESFN, value)      => acc.picture = stringParser(value)
    case (PRIO_ESFN, value)         => acc.prio = Some(intParser(value))
    // TODO Opt: Стоит использоваться вместо java-reflections ускоренные scala-json парсеры на базе case-class'ов.
    case (USER_CAT_ID_ESFN, value)  => acc.userCatId = Some(stringParser(value))
    case (OFFER_ESFN, value)        => acc.offers = JacksonWrapper.convert[List[MMartAdProduct]](value)
    case (PANEL_ESFN, value)        => acc.panel = Some(JacksonWrapper.convert[MMartAdPanelSettings](value))
    case (IS_SHOWN_ESFN, value)     => acc.isShown = booleanParser(value)
    case (TEXT_ALIGN_ESFN, value)   => acc.textAlign = JacksonWrapper.convert[MMartAdTextAlign](value)
  }

  def generateMapping: XContentBuilder = jsonGenerator { implicit b =>
    val fontField = FieldObject(
      id = FONT_ESFN,
      enabled = false,
      properties = Nil
    )
    val stringValueField = FieldString(
      id = VALUE_ESFN,
      index = FieldIndexingVariants.no,
      include_in_all = true
    )
    def floatValueField(iia: Boolean) = FieldNumber(
      id = VALUE_ESFN,
      fieldType = DocFieldTypes.float,
      index = FieldIndexingVariants.no,
      include_in_all = iia
    )
    // Поле приоритета. На первом этапе null или число.
    val prioField = FieldNumber(
      id = PRIO_ESFN,
      fieldType = DocFieldTypes.integer,
      index = FieldIndexingVariants.not_analyzed,
      include_in_all = false
    )
    val companyIdField = FieldString(
      id = COMPANY_ID_ESFN,
      index = FieldIndexingVariants.no,
      include_in_all = false
    )
    val shopIdField = FieldString(
      id = SHOP_ID_ESFN,
      index = FieldIndexingVariants.not_analyzed,
      include_in_all = false
    )
    val martIdField = FieldString(
      id = MART_ID_ESFN,
      index = FieldIndexingVariants.not_analyzed,
      include_in_all = false
    )
    val textAlignsField = FieldObject(
      id = TEXT_ALIGN_ESFN,
      enabled = false,
      properties = Nil
    )
    val panelField = FieldObject(
      id = PANEL_ESFN,
      enabled = false,
      properties = Seq()
    )
    val offerField = FieldNestedObject(
      id = OFFER_ESFN,
      enabled = true,
      properties = Seq(
        // product-поля
        FieldNestedObject(
          id = VENDOR_ESFN,
          properties = Seq(
            stringValueField,
            fontField
          )
        ),
        FieldNestedObject(
          id = MODEL_ESFN,
          properties = Seq(
            stringValueField,
            fontField
          )
        ),
        FieldNestedObject(
          id = PRICE_ESFN,
          properties = Seq(
            floatValueField(iia = true),  // TODO нужно как-то проанализировать цифры эти, округлять например.
            fontField
          )
        ),
        FieldNestedObject(
          id = OLD_PRICE_ESFN,
          properties = Seq(
            floatValueField(iia = false),
            fontField
          )
        ),
        // discount-поля
        FieldNestedObject(
          id = TEXT1_ESFN,
          properties = Seq(
            stringValueField,
            fontField
          )
        ),
        FieldNestedObject(
          id = DISCOUNT_ESFN,
          properties = Seq(
            floatValueField(iia = true),
            fontField
          )
        ),
        FieldNestedObject(
          id = DISCOUNT_TPL_ESFN,
          enabled = false,
          properties = Nil
        ),
        FieldNestedObject(
          id = TEXT2_ESFN,
          properties = Seq(
            stringValueField,
            fontField
          )
        )
      )   // offer.properties
    )
    val isShownField = FieldBoolean(
      id = IS_SHOWN_ESFN,
      index = FieldIndexingVariants.no,
      include_in_all = false
    )
    val pictureField = FieldString(
      id = PICTURE_ESFN,
      index = FieldIndexingVariants.no,
      include_in_all = false
    )
    val categoryField = FieldString(
      id = USER_CAT_ID_ESFN,
      include_in_all = false,
      index = FieldIndexingVariants.not_analyzed
    )
    // Собираем маппинг индекса.
    IndexMapping(
      typ = ES_TYPE_NAME,
      staticFields = Seq(
        FieldAll(enabled = true, analyzer = FTS_RU_AN),
        FieldSource(enabled = true)
      ),
      properties = Seq(
        pictureField,
        shopIdField,
        martIdField,
        companyIdField,
        categoryField,
        offerField,
        prioField,
        panelField,
        textAlignsField,
        isShownField
      )
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
  var userCatId   : Option[String] = None,
  var isShown     : Boolean = false,
  var id          : Option[String] = None
) extends EsModelT[MMartAd] {
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
    // Также рендерим данные по textAlign на устройствах.
    acc.field(TEXT_ALIGN_ESFN)
    textAlign.render(acc)
  }

  /**
   * Сохранить экземпляр в хранилище ES, проверив важные поля.
   * @return Фьючерс с новым/текущим id
   */
  override def save(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[String] = {
    if (picture == null || offers.isEmpty || shopId == null || companyId == null || martId == null) {
      throw new IllegalStateException("Some or all important fields have invalid values: " + this)
    } else {
      super.save
    }
  }
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
    acc.startObject(DISCOUNT_ESFN)
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


case class MMartAdTAPhone(align: String) {
  def render(acc: XContentBuilder) {
    acc.startObject()
      .field("align", align)
    .endObject()
  }
}

case class MMartAdTATablet(alignTop: String, alignBottom: String) {
  def render(acc: XContentBuilder) {
    acc.startObject()
      .field("alignTop", alignTop)
      .field("alignBottom", alignBottom)
    .endObject()
  }
}

case class MMartAdTextAlign(phone: MMartAdTAPhone, tablet: MMartAdTATablet) {
  def render(acc: XContentBuilder) {
    acc.startObject()
      // телефон
      acc.field("phone")
      phone render acc
      // планшет
      acc.field("tablet")
      tablet render acc
    .endObject()
  }
}


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


