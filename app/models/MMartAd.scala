package models

import io.suggest.model._
import org.elasticsearch.common.xcontent.XContentBuilder
import io.suggest.util.SioEsUtil._
import io.suggest.util.SioConstants._
import EsModel._
import io.suggest.util.JacksonWrapper
import MShop.ShopId_t, MMart.MartId_t

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.03.14 18:30
 * Description: Рекламные "плакаты" в торговом центре.
 * prio используется для задания приоритета в отображении в рамках магазина. На текущий момент там всё просто:
 * если null, то приоритета нет, если 1 то он есть.
 */
object MMartAd extends EsModelStaticT[MMartAd] {

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

  protected def dummy(id: String) = {
    MMartAd(id=Some(id), offers=Nil, picture=null, martId=null, companyId = null)
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
    case (TEXT_ALIGN_ESFN, value)   => acc.textAligns = JacksonWrapper.convert[List[MMartAdTextAlign]](value)
  }

  def generateMapping: XContentBuilder = jsonGenerator { implicit b =>
    val fontField = FieldNestedObject(
      id = FONT_ESFN,
      enabled = false,
      properties = Seq(
        FieldString(
          id = COLOR_ESFN,
          index = FieldIndexingVariants.no,
          include_in_all = false
        )
      )
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
            floatValueField(iia = true),  // Вероятно, стоит всё-таки инклюдить эту цену в индекс
            fontField
          )
        ),
        FieldNestedObject(
          id = OLD_PRICE_ESFN,
          properties = Seq(
            floatValueField(iia = false),
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
  var shopId      : Option[ShopId_t] = None,
  var companyId   : MCompany.CompanyId_t,
  var panel       : Option[MMartAdPanelSettings] = None,
  var prio        : Option[Int] = None,
  var userCatId   : Option[String] = None,
  var textAligns  : List[MMartAdTextAlign] = Nil,
  var isShown     : Boolean = false,
  id              : Option[String] = None
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
    if (!textAligns.isEmpty) {
      acc.startArray(TEXT_ALIGN_ESFN)
        textAligns foreach { _ render acc }
      acc.endArray()
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
  model:    Option[MMAdStringField],
  oldPrice: Option[MMAdFloatField],
  price:    MMAdFloatField
) extends MMartAdOfferT {

  def isProduct = true

  def renderFields(acc: XContentBuilder) {
    vendor.render(acc)
    if (model.isDefined)
      model.get.render(acc)
    if (oldPrice.isDefined)
      oldPrice.get.render(acc)
    price.render(acc)
  }
}

case class MMartAdDiscount(
  text1: Option[MMAdStringField],
  discount: MMAdFloatField,
  text2: Option[MMAdStringField]
) extends MMartAdOfferT {

  def isProduct = false

  def renderFields(acc: XContentBuilder) {
    if (text1.isDefined)
      text1.get.render(acc)
    discount.render(acc)
    if (text2.isDefined)
      text2.get.renderFields(acc)
  }
}

sealed trait MMAdValueField {
  def renderFields(acc: XContentBuilder)
  def font: MMAdFieldFont
  def render(acc: XContentBuilder) {
    acc.startObject(VENDOR_ESFN)
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


case class MMartAdTextAlign(id: String, align: String) {
  def render(acc: XContentBuilder) {
    acc.startObject(id)
      .field(TEXT_ALIGN_ESFN, align)
    .endObject()
  }
}

