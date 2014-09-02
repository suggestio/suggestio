package models

import _root_.util.PlayMacroLogsImpl
import org.joda.time.DateTime
import io.suggest.model._
import EsModel._
import io.suggest.util.SioEsUtil._
import play.api.libs.json._
import io.suggest.util.SioEsUtil.FieldAll
import io.suggest.util.SioEsUtil.FieldDate
import io.suggest.util.SioEsUtil.FieldString
import io.suggest.util.SioEsUtil.FieldSource
import play.api.libs.json.JsString
import io.suggest.event.SioNotifierStaticClientI
import scala.collection.Map
import scala.concurrent.ExecutionContext
import org.elasticsearch.client.Client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.05.13 14:37
 * Description: Записи в блоге. По сути json-файлы в хранилище.
 * Порт модели blog_record из старого sioweb.
 */

object MBlog extends EsModelMinimalStaticT with PlayMacroLogsImpl {
  override val ES_TYPE_NAME = "blog"

  override type T = MBlog

  val TITLE_ESFN    = "title"
  val BG_IMAGE_ESFN = "bgImage"
  val BG_COLOR_ESFN = "bgColor"
  val TEXT_ESFN     = "text"
  val DATE_ESFN     = "date"


  override def generateMappingStaticFields: List[Field] = List(
    FieldSource(enabled = true),
    FieldAll(enabled = false)
  )

  override def generateMappingProps: List[DocField] = List(
    FieldString(TITLE_ESFN, include_in_all = true, index = FieldIndexingVariants.no),
    FieldString(DESCRIPTION_ESFN, include_in_all = true, index = FieldIndexingVariants.no),
    FieldString(BG_IMAGE_ESFN, include_in_all = false, index = FieldIndexingVariants.no),
    FieldString(BG_COLOR_ESFN, include_in_all = false, index = FieldIndexingVariants.no),
    FieldString(TEXT_ESFN, include_in_all = true, index = FieldIndexingVariants.no),
    FieldDate(DATE_ESFN, include_in_all = false, index = FieldIndexingVariants.no)
  )

  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): T = {
    MBlog(
      title = stringParser( m(TITLE_ESFN) ),
      description = stringParser( m(DESCRIPTION_ESFN) ),
      bgImage = stringParser( m(BG_IMAGE_ESFN) ),
      bgColor = stringParser( m(BG_COLOR_ESFN) ),
      text    = stringParser( m(TEXT_ESFN) ),
      id = id,
      date    = m.get(DATE_ESFN).fold(DateTime.now)(dateTimeParser)
    )
  }

}

import MBlog._

final case class MBlog(
  title         : String,
  description   : String,
  bgImage       : String,
  bgColor       : String,
  text          : String,
  id            : Option[String] = None,
  date          : DateTime = DateTime.now()
) extends EsModelT {

  override type T = MBlog

  override def companion = MBlog
  override def versionOpt = None

  def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    TITLE_ESFN -> JsString(title) ::
      DESCRIPTION_ESFN -> JsString(description) ::
      BG_IMAGE_ESFN -> JsString(bgImage) ::
      BG_COLOR_ESFN -> JsString(bgColor) ::
      TEXT_ESFN -> JsString(text) ::
      DATE_ESFN -> EsModel.date2JsStr(date) ::
      acc
  }

}


trait MBlogJmxMBean extends EsModelJMXMBeanCommon
final class MBlogJmx(implicit val ec: ExecutionContext, val client: Client, val sn: SioNotifierStaticClientI)
  extends EsModelJMXBase
  with MBlogJmxMBean
{
  override def companion = MBlog
}


