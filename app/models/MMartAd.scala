package models

import io.suggest.model._
import org.elasticsearch.common.xcontent.XContentBuilder
import io.suggest.util.SioEsUtil._
import io.suggest.util.SioConstants._
import EsModel._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.03.14 18:30
 * Description: Рекламные "плакаты" в торговом центре.
 */
object MMartAd extends EsModelStaticT[MMartAd] {

  override val ES_TYPE_NAME = "martAd"

  val PICTURE_ESFN      = "picture"
  val OFFER_ESFN        = "offers"
  val VENDOR_ESFN       = "vendor"
  val MODEL_ESFN        = "model"
  val PRICE_ESFN        = "price"
  val OLD_PRICE_ESFN    = "oldPrice"

  val FONT_ESFN         = "font"
  val SIZE_ESFN         = "size"
  val COLOR_ESFN        = "color"

  override protected def dummy(id: String) = MMartAd(id=Some(id), offers=Nil, picture=null)

  override def applyKeyValue(acc: MMartAd): PartialFunction[(String, AnyRef), Unit] = {
    case (PICTURE_ESFN, value) => acc.picture = stringParser(value)
    // TODO Написать парсер для сложного поля offers.
  }

  override def generateMapping: XContentBuilder = jsonGenerator { implicit b =>
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
    // Собираем маппинг индекса.
    IndexMapping(
      typ = ES_TYPE_NAME,
      staticFields = Seq(
        FieldAll(enabled = true, analyzer = FTS_RU_AN),
        FieldSource(enabled = true)
      ),
      properties = Seq(
        FieldString(
          id = PICTURE_ESFN,
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),
        FieldNestedObject(
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
        )     // offers
      )       // indexMapping.properties
    )         // indexMapping
  }

}

import MMartAd._

case class MMartAd(
  var offers  : List[MMartAdOffer],
  var picture : String,
  id          : Option[String] = None
) extends EsModelT[MMartAd] {
  override def companion = MMartAd

  override def writeJsonFields(acc: XContentBuilder) {
    acc.field(PICTURE_ESFN, picture)
    // Загружаем офферы
    acc.startArray(OFFER_ESFN)
    offers foreach { offer =>
      offer.render(acc)
    }
    acc.endArray()
  }
}


case class MMartAdOffer(
  vendor:   StringField,
  model:    StringField,
  oldPrice: Option[FloatField],
  price:    FloatField
) {
  def render(acc: XContentBuilder) {
    acc.startObject()
    vendor.render(acc)
    model.render(acc)
    oldPrice foreach { _.render(acc) }
    price.render(acc)
    acc.endObject()
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
