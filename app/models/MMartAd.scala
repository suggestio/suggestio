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

  val FONT_ESFN         = "font"
  val SIZE_ESFN         = "size"
  val COLOR_ESFN        = "color"
  val IS_SHOWN_ESFN     = "isShown"

  protected def dummy(id: String) = {
    MMartAd(id=Some(id), offers=Nil, picture=null, martId=null)
  }

  def applyKeyValue(acc: MMartAd): PartialFunction[(String, AnyRef), Unit] = {
    case (MART_ID_ESFN, value)  => acc.martId = martIdParser(value)
    case (SHOP_ID_ESFN, value)  => acc.shopId = Some(shopIdParser(value))
    case (PICTURE_ESFN, value)  => acc.picture = stringParser(value)
    case (PRIO_ESFN, value)     => acc.prio = Some(intParser(value))
    // TODO Opt: Стоит использоваться вместо java-reflections ускоренные scala-json парсеры на базе case-class'ов.
    case (CATEGORY_ESFN, value) => acc.category = Some(JacksonWrapper.convert[IndexableCategory](value))
    case (OFFER_ESFN, value)    => acc.offers = JacksonWrapper.convert[List[MMartAdProduct]](value)
    case (PANEL_ESFN, value)    => acc.panel = Some(JacksonWrapper.convert[MMartAdPanelSettings](value))
  }

  def generateMapping: XContentBuilder = jsonGenerator { implicit b =>
    val fontField = FieldNestedObject(
      id = FONT_ESFN,
      properties = Seq(
        FieldNumber(
          id = SIZE_ESFN,
          fieldType = DocFieldTypes.integer,
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),
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
    val offerField = FieldNestedObject(
      id = OFFER_ESFN,
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

    val pictureField = FieldString(
      id = PICTURE_ESFN,
      index = FieldIndexingVariants.no,
      include_in_all = false
    )
    val categoryField = FieldNestedObject(
      id = CATEGORY_ESFN,
      properties = Seq(
        // Индексируемое текстом название категории. Обычно тут словесный путь до категории, не все названия элементов пути указаны.
        FieldString(
          id = NAME_ESFN,
          include_in_all = true,
          index = FieldIndexingVariants.no
        ),
        // Поле категории. Тут может быть как конкретный id, так и category path для возможности выборки по id категории любого уровня.
        FieldString(
          id = CATEGORY_ID_ESFN,
          include_in_all = false,
          index = FieldIndexingVariants.not_analyzed    // Выборка в рамках категории нужна ли?
        )
      )
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
        categoryField,
        offerField,
        prioField
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
 * @param category Индексируемые данные по категории рекламируемого товара.
 * @param id id товара.
 */
case class MMartAd(
  var martId      : MartId_t,
  var offers      : List[MMartAdOfferT],
  var picture     : String,
  var shopId      : Option[ShopId_t] = None,
  var panel       : Option[MMartAdPanelSettings] = None,
  var prio        : Option[Int] = None,
  var category    : Option[IndexableCategory] = None,
  var taMobile    : Option[String] = None,
  var taTablet    : Option[String] = None,
  id              : Option[String] = None
) extends EsModelT[MMartAd] {
  def companion = MMartAd

  def writeJsonFields(acc: XContentBuilder) {
    acc.field(MART_ID_ESFN, martId)
      .field(PICTURE_ESFN, picture)
    if (prio.isDefined)
      acc.field(PRIO_ESFN, prio.get)
    if (shopId.isDefined)
      acc.field(SHOP_ID_ESFN, shopId.get)
    if (category.isDefined)
      category.get.render(acc)
    if (panel.isDefined)
      panel.get.render(acc)
    // Загружаем офферы
    acc.startArray(OFFER_ESFN)
      offers foreach { offer =>
        offer.renderJson(acc)
      }
    acc.endArray()
  }
}

trait MMartAdOfferT {
  def renderFields(acc: XContentBuilder)
  def renderJson(acc: XContentBuilder) {
    acc.startObject()
    renderFields(acc)
    acc.endObject()
  }
}

case class MMartAdProduct(
  vendor:   StringField,
  model:    StringField,
  oldPrice: Option[FloatField],
  price:    FloatField
) extends MMartAdOfferT {
  def renderFields(acc: XContentBuilder) {
    vendor.render(acc)
    model.render(acc)
    if (oldPrice.isDefined)
      oldPrice.get.render(acc)
    price.render(acc)
  }
}

case class MMartAdDiscount(
  text1: StringField,
  discount: FloatField,
  text2: StringField
) extends MMartAdOfferT {
  def renderFields(acc: XContentBuilder) {
    text1.render(acc)
    discount.render(acc)
    text2.renderFields(acc)
  }
}

sealed trait ValueField {
  def renderFields(acc: XContentBuilder)
  def font: FieldFont
  def render(acc: XContentBuilder) {
    acc.startObject(VENDOR_ESFN)
      renderFields(acc)
      font.render(acc)
    acc.endObject()
  }
}

case class StringField(value:String, font: FieldFont) extends ValueField {
  def renderFields(acc: XContentBuilder) {
    acc.field(VALUE_ESFN, value)
  }
}
case class FloatField(value: Float, font: FieldFont) extends ValueField {
  def renderFields(acc: XContentBuilder) {
    acc.field(VALUE_ESFN, value)
  }
}

case class FieldFont(size: Int, color: String) {
  def render(acc: XContentBuilder) {
    acc.startObject(FONT_ESFN)
      .field(SIZE_ESFN, size)
      .field(COLOR_ESFN, color)
    .endObject()
  }
}

case class IndexableCategory(name: String, ids: List[String]) {
  def render(acc: XContentBuilder) {
    acc.startObject(CATEGORY_ESFN)
      .field(NAME_ESFN, name)
      .array(CATEGORY_ID_ESFN, ids : _*)
    .endObject()
  }
}


case class MMartAdPanelSettings(color: String, isShown: Boolean) {
  def render(acc: XContentBuilder) {
    acc.startObject(PANEL_ESFN)
      .field(IS_SHOWN_ESFN, isShown)
      .field(COLOR_ESFN, color)
    .endObject()
  }
}

