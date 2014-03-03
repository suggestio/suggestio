package models

import io.suggest.model.{EsModel, EsModelStaticT, EsModelT}
import org.elasticsearch.common.xcontent.XContentBuilder
import EsModel._
import io.suggest.util.SioEsUtil._
import io.suggest.util.SioConstants._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.02.14 18:32
 * Description: Дерево категорий конкретного ТЦ. Хранится в ES.
 * TODO Каждая категория указывает на YM-категорию.
 */
object MMartCategory extends EsModelStaticT[MMartCategory] {

  val ES_TYPE_NAME = "martCat"

  val YM_CAT_ID_ESFN = "ymCatId"
  val CSS_CLASS_ESFN = "cssClass"
  val POSITION_ESFN  = "position"

  def generateMapping: XContentBuilder = jsonGenerator { implicit b =>
    IndexMapping(
      typ = ES_TYPE_NAME,
      static_fields = Seq(
        FieldAll(enabled = false, analyzer = FTS_RU_AN),
        FieldSource(enabled = true)
      ),
      properties = Seq(
        FieldString(
          id = NAME_ESFN,
          index = FieldIndexingVariants.no,
          include_in_all = true
        ),
        FieldString(
          id = YM_CAT_ID_ESFN,
          index = FieldIndexingVariants.no,
          include_in_all = false
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
        )
      )
    )
  }


  def applyKeyValue(acc: MMartCategory): PartialFunction[(String, AnyRef), Unit] = {
    case (NAME_ESFN, value)         => acc.name = stringParser(value)
    case (YM_CAT_ID_ESFN, value)    => acc.ymCatId = stringParser(value)
    case (PARENT_ID_ESFN, value)    => acc.parentId = Some(stringParser(value))
    case (POSITION_ESFN,  value)    => acc.position = intParser(value)
    case (CSS_CLASS_ESFN, value)    => acc.cssClass = Some(stringParser(value))
  }

  protected def dummy(id: String) = {
    MMartCategory(name = null, ymCatId = null, parentId = None, position = Int.MaxValue)
  }
}

import MMartCategory._

case class MMartCategory(
  var name      : String,
  var ymCatId   : String,
  var parentId  : Option[String],
  var position  : Int,
  var cssClass  : Option[String] = None,
  var id        : Option[String] = None
) extends EsModelT[MMartCategory] {

  override def writeJsonFields(acc: XContentBuilder) {
    acc.field(NAME_ESFN, name)
      .field(YM_CAT_ID_ESFN, ymCatId)
      .field(POSITION_ESFN, position)
    if (parentId.isDefined)
      acc.field(PARENT_ID_ESFN, parentId.get)
    if (cssClass.isDefined)
      acc.field(CSS_CLASS_ESFN, cssClass.get)
  }

  override def companion: EsModelStaticT[MMartCategory] = MMartCategory
}
