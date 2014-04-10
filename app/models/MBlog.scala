package models

import org.joda.time.DateTime
import org.elasticsearch.common.xcontent.XContentBuilder
import io.suggest.model.{EsModelT, EsModel, EsModelStaticT}
import EsModel._
import io.suggest.util.SioEsUtil._
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.05.13 14:37
 * Description: Записи в блоге. По сути json-файлы в хранилище.
 * Порт модели blog_record из старого sioweb.
 */

object MBlog extends EsModelStaticT[MBlog] {
  override val ES_TYPE_NAME: String = "blog"

  val TITLE_ESFN    = "title"
  val BG_IMAGE_ESFN = "bgImage"
  val BG_COLOR_ESFN = "bgColor"
  val TEXT_ESFN     = "text"
  val DATE_ESFN     = "date"


  def generateMappingStaticFields: List[Field] = List(
    FieldSource(enabled = true),
    FieldAll(enabled = false)
  )

  def generateMappingProps: List[DocField] = List(
    FieldString(TITLE_ESFN, include_in_all = true, index = FieldIndexingVariants.no),
    FieldString(DESCRIPTION_ESFN, include_in_all = true, index = FieldIndexingVariants.no),
    FieldString(BG_IMAGE_ESFN, include_in_all = false, index = FieldIndexingVariants.no),
    FieldString(BG_COLOR_ESFN, include_in_all = false, index = FieldIndexingVariants.no),
    FieldString(TEXT_ESFN, include_in_all = true, index = FieldIndexingVariants.no),
    FieldDate(DATE_ESFN, include_in_all = false, index = FieldIndexingVariants.no)
  )


  def applyKeyValue(acc: MBlog): PartialFunction[(String, AnyRef), Unit] = {
    case (TITLE_ESFN, value)          => acc.title = stringParser(value)
    case (DESCRIPTION_ESFN, value)    => acc.description = stringParser(value)
    case (BG_IMAGE_ESFN, value)       => acc.bgImage = stringParser(value)
    case (BG_COLOR_ESFN, value)       => acc.bgColor = stringParser(value)
    case (TEXT_ESFN, value)           => acc.text = stringParser(value)
    case (DATE_ESFN, value)           => acc.date = dateTimeParser(value)
  }

  protected def dummy(id: String) = MBlog(
    id = Option(id),
    title = null,
    description = null,
    bgImage = null,
    bgColor = null,
    text = null
  )
}

import MBlog._

case class MBlog(
  var title     : String,
  var description : String,
  var bgImage   : String,
  var bgColor   : String,
  var text      : String,
  id            : Option[String] = None,
  var date      : DateTime = null
) extends EsModelT[MBlog] {

  def companion = MBlog

  def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    if (date == null)
      date = DateTime.now
    TITLE_ESFN -> JsString(title) ::
      DESCRIPTION_ESFN -> JsString(description) ::
      BG_IMAGE_ESFN -> JsString(bgImage) ::
      BG_COLOR_ESFN -> JsString(bgColor) ::
      TEXT_ESFN -> JsString(text) ::
      DATE_ESFN -> EsModel.date2JsStr(date) ::
      acc
  }

}

