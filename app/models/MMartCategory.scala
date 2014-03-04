package models

import io.suggest.model.{EsModel, EsModelStaticT, EsModelT}
import org.elasticsearch.common.xcontent.XContentBuilder
import EsModel._
import io.suggest.util.SioEsUtil._
import io.suggest.util.SioConstants._
import io.suggest.util.JacksonWrapper

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.02.14 18:32
 * Description: Дерево категорий конкретного ТЦ. Хранится в ES.
 * TODO Каждая категория указывает на YM-категорию.
 */
object MMartCategory extends EsModelStaticT[MMartCategory] {

  val ES_TYPE_NAME = "martCat"

  val YM_CAT_ESFN    = "ymCatId"
  val YM_CAT_ID_ESFN = "ycId"
  val YM_CAT_INHERIT_ESFT = "inherit"
  val CSS_CLASS_ESFN = "cssClass"
  val POSITION_ESFN  = "position"
  val INCLUDE_IN_ALL_ESFN = "iia"

  def generateMapping: XContentBuilder = jsonGenerator { implicit b =>
    IndexMapping(
      typ = ES_TYPE_NAME,
      staticFields = Seq(
        FieldAll(enabled = false, analyzer = FTS_RU_AN),
        FieldSource(enabled = true)
      ),
      properties = Seq(
        FieldString(
          id = NAME_ESFN,
          index = FieldIndexingVariants.no,
          include_in_all = true
        ),
        FieldNestedObject(
          id = YM_CAT_ESFN,
          properties = Seq(
            FieldString(
              id = YM_CAT_ID_ESFN,
              index = FieldIndexingVariants.no,
              include_in_all = false
            ),
            FieldBoolean(
              id = YM_CAT_INHERIT_ESFT,
              index = FieldIndexingVariants.no,
              include_in_all = false
            )
          )
        ),
        FieldString(
          id = PARENT_ID_ESFN,
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),
        FieldNumber(
          id = POSITION_ESFN,
          fieldType = DocFieldTypes.integer,
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),
        FieldString(
          id = CSS_CLASS_ESFN,
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),
        FieldBoolean(
          id = INCLUDE_IN_ALL_ESFN,
          index = FieldIndexingVariants.no,
          include_in_all = false
        )
      )
    )
  }

  def applyKeyValue(acc: MMartCategory): PartialFunction[(String, AnyRef), Unit] = {
    case (NAME_ESFN, value)         => acc.name = stringParser(value)
    case (YM_CAT_ESFN, value)       => acc.ymCatPtr = Some(JacksonWrapper.convert[MMartYmCatPtr](value))
    case (PARENT_ID_ESFN, value)    => acc.parentId = Some(stringParser(value))
    case (POSITION_ESFN,  value)    => acc.position = intParser(value)
    case (CSS_CLASS_ESFN, value)    => acc.cssClass = Some(stringParser(value))
    case (INCLUDE_IN_ALL_ESFN, value) => acc.includeInAll = booleanParser(value)
  }

  protected def dummy(id: String) = {
    MMartCategory(name = null, ymCatPtr = None, parentId = None, position = Int.MaxValue)
  }
}

import MMartCategory._

/**
 * @param name Отображаемое название категории. Наверное, Будет прогонятся через Messages()
 * @param ymCatPtr Данные о backend-категории яндекс-маркета: id и прочие параметры.
 * @param parentId id родительской категории [[MMartCategory]].
 * @param position Позиция в выдаче. Используется при сортировке.
 * @param cssClass Класс css.
 * @param includeInAll Индексировать вместе с товаром.
 * @param id Идентификатор категории.
 */
case class MMartCategory(
  var name      : String,
  var ymCatPtr  : Option[MMartYmCatPtr],
  var parentId  : Option[String],
  var position  : Int,
  var id        : Option[String] = None,
  var cssClass  : Option[String] = None,
  var includeInAll: Boolean = true
) extends EsModelT[MMartCategory] {

  def writeJsonFields(acc: XContentBuilder) {
    acc.field(NAME_ESFN, name)
      .field(POSITION_ESFN, position)
      .field(INCLUDE_IN_ALL_ESFN, includeInAll)
    if (ymCatPtr.isDefined)
      ymCatPtr.get.render(acc)
    if (parentId.isDefined)
      acc.field(PARENT_ID_ESFN, parentId.get)
    if (cssClass.isDefined)
      acc.field(CSS_CLASS_ESFN, cssClass.get)
  }

  def companion: EsModelStaticT[MMartCategory] = MMartCategory
}

/**
 * @param catId id категории в дереве ym-категорий.
 * @param inherit Наследовать прямые подкатегории ym-категории при поиске.
 */
case class MMartYmCatPtr(catId: String, inherit: Boolean = true) {
  def render(acc: XContentBuilder) {
    acc.startObject(YM_CAT_ESFN)
      .field(YM_CAT_ID_ESFN, catId)
      .field(YM_CAT_INHERIT_ESFT, inherit)
    .endObject()
  }
}

