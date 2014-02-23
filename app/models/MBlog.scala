package models

import org.joda.time.DateTime
import util._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import scala.collection.Map
import org.elasticsearch.common.xcontent.XContentBuilder
import EsModel._
import io.suggest.util.SioEsUtil._
import io.suggest.util.SioConstants._

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

  override def generateMapping: XContentBuilder = jsonGenerator { implicit b =>
    IndexMapping(
      typ = ES_TYPE_NAME,
      static_fields = Seq(
        FieldSource(enabled = true),
        FieldAll(enabled = false, analyzer = FTS_RU_AN)
      ),
      properties = Seq(
        FieldString(
          id = TITLE_ESFN,
          include_in_all = true,
          index = FieldIndexingVariants.no
        ),
        FieldString(
          id = DESCRIPTION_ESFN,
          include_in_all = true,
          index = FieldIndexingVariants.no
        ),
        FieldString(
          id = BG_IMAGE_ESFN,
          include_in_all = false,
          index = FieldIndexingVariants.no
        ),
        FieldString(
          id = BG_COLOR_ESFN,
          include_in_all = false,
          index = FieldIndexingVariants.no
        ),
        FieldString(
          id = TEXT_ESFN,
          include_in_all = true,
          index = FieldIndexingVariants.no
        ),
        FieldDate(
          id = DATE_ESFN,
          include_in_all = false,
          index = FieldIndexingVariants.no
        )
      )
    )
  }

  override def applyMap(m: Map[String, AnyRef], acc: MBlog): MBlog = {
    m foreach {
      case (TITLE_ESFN, value)        => acc.title = stringParser(value)
      case (DESCRIPTION_ESFN, value)  => acc.description = stringParser(value)
      case (BG_IMAGE_ESFN, value)     => acc.bgImage = stringParser(value)
      case (BG_COLOR_ESFN, value)     => acc.bgColor = stringParser(value)
      case (TEXT_ESFN, value)         => acc.text = stringParser(value)
      case (DATE_ESFN, value)         => acc.date = dateTimeParser(value)
    }
    acc
  }

  override protected def dummy(id: String) = MBlog(
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

  override def writeJsonFields(acc: XContentBuilder) {
    acc.field(TITLE_ESFN, title)
      .field(DESCRIPTION_ESFN, description)
      .field(BG_IMAGE_ESFN, bgImage)
      .field(BG_COLOR_ESFN, bgColor)
      .field(TEXT_ESFN, text)
    if (date == null)
      date = DateTime.now()
    acc.field(DATE_ESFN, date)
  }
}

