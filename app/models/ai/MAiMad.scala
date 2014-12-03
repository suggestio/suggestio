package models.ai

import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model.EsModel.FieldsJsonAcc
import io.suggest.model._
import io.suggest.util.SioEsUtil._
import org.elasticsearch.client.Client
import org.joda.time.DateTimeZone
import org.xml.sax.helpers.DefaultHandler
import play.api.libs.json.{JsObject, JsArray, JsString}
import util.PlayMacroLogsImpl
import util.ai.GetParseResult
import util.ai.mad.render.{ScalaStiRenderer, MadAiRenderedT}
import util.ai.sax.weather.gidromet.GidrometRssSax
import java.{util => ju}

import scala.collection.Map
import scala.concurrent.ExecutionContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.11.14 11:03
 * Description: Описание для будущих рекламных карточек.
 */
object MAiMad extends EsModelStaticT with PlayMacroLogsImpl {

  override type T = MAiMad

  override val ES_TYPE_NAME = "aim"

  // Названия JSON-полей
  val NAME_ESFN               = "n"
  val TPL_AD_ID_ESFN          = "tpl"
  val TARGET_AD_IDS_ESFN      = "tgts"
  val DESCR_ESFN              = "d"
  val RENDERERS_ESFN          = "rr"
  val TIMEZONE_ESFN           = "tz"
  val SOURCES_ESFN            = "src"

  override def generateMappingStaticFields: List[Field] = {
    List(
      FieldAll(enabled = true),
      FieldSource(enabled = true)
    )
  }

  override def generateMappingProps: List[DocField] = {
    List(
      FieldString(NAME_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = false),
      FieldString(TPL_AD_ID_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldString(DESCR_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = true),
      FieldString(RENDERERS_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = true),
      FieldString(TIMEZONE_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = true),
      AiSource.generateMapping
    )
  }

  /**
   * Десериализация одного элементам модели.
   * @param id id документа.
   * @param m Карта, распарсенное json-тело документа.
   * @return Экземпляр модели.
   */
  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): T = {
    import EsModel.{stringParser, strListParser, iteratorParser}
    MAiMad(
      name = stringParser(m(NAME_ESFN)),
      sources = iteratorParser( m(SOURCES_ESFN) )
        .map { AiSource.parseJson }
        .toSeq,
      tplAdId = stringParser(m(TPL_AD_ID_ESFN)),
      renderers = iteratorParser(m(RENDERERS_ESFN))
        .map { s => MAiRenderers.withName(stringParser(s)) : MAiRenderer }
        .toSeq,
      targetAdIds = strListParser(m(TARGET_AD_IDS_ESFN)),
      descr = m.get(DESCR_ESFN)
        .map(stringParser),
      tz = m.get(TIMEZONE_ESFN)
        .fold(DateTimeZone.getDefault) { tzRaw => DateTimeZone.forID(stringParser(tzRaw)) },
      id = id,
      versionOpt = version
    )
  }

}


import MAiMad._


case class MAiMad(
  name            : String,
  sources         : Seq[AiSource],
  tplAdId         : String,
  renderers       : Seq[MAiRenderer],
  targetAdIds     : Seq[String],
  tz              : DateTimeZone,
  descr           : Option[String] = None,
  id              : Option[String] = None,
  versionOpt      : Option[Long] = None
) extends EsModelT with EsModelPlayJsonT {

  override type T = this.type
  override def companion = MAiMad

  override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    var acc1: FieldsJsonAcc =
      NAME_ESFN               -> JsString(name) ::
      SOURCES_ESFN            -> JsArray(sources.map(_.toPlayJson)) ::
      TPL_AD_ID_ESFN          -> JsString(tplAdId) ::
      TARGET_AD_IDS_ESFN      -> JsArray( targetAdIds.map(JsString.apply) ) ::
      TIMEZONE_ESFN           -> JsString(tz.getID) ::
      acc
    if (renderers.nonEmpty)
      acc1 ::= RENDERERS_ESFN -> JsArray(renderers.map(rr => JsString(rr.name)))
    if (descr.isDefined)
      acc1 ::= DESCR_ESFN -> JsString(descr.get)
    acc1
  }

}


/** JMX MBean для модели [[MAiMad]]. */
trait MAiMadJmxMBean extends EsModelJMXMBeanI
/** Реализация JMX MBean для модели [[MAiMad]]. */
final class MAiMadJmx(implicit val ec: ExecutionContext, val client: Client, val sn: SioNotifierStaticClientI)
  extends EsModelJMXBase with MAiMadJmxMBean {
  override def companion = MAiMad
}


object AiSource {

  val URL_ESFN                = "url"
  val CONTENT_HANDLERS_ESFN   = "chs"

  def generateMapping: DocField = {
    FieldObject(SOURCES_ESFN, enabled = true, properties = Seq(
      FieldString(URL_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = true),
      FieldString(CONTENT_HANDLERS_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false)
    ))
  }

  /** Распарсить из jackson json ранее сериализованный экземпляр [[AiSource]]. */
  def parseJson(x: Any): AiSource = {
    x match {
      case jmap: ju.Map[_,_] =>
        AiSource(
          url = Option( jmap.get(URL_ESFN) )
            .map(EsModel.stringParser)
            .getOrElse(""),
          contentHandlers = Option( jmap.get(CONTENT_HANDLERS_ESFN) )
            .map { rawChs =>
              EsModel.iteratorParser(rawChs)
                .map { rawChId => MAiMadContentHandlers.withName(EsModel.stringParser(rawChId)): MAiMadContentHandler }
                .toSeq
            }
            .getOrElse(Seq.empty)
        )
    }
  }

}

/**
 * Source описывает источник данных: ссылка с сырыми данными и парсеры, применяемые для извлечения полезной нагрузки.
 * @param url Ссылка или её шаблон.
 * @param contentHandlers Экстракторы контента.
 */
case class AiSource(
  url: String,
  contentHandlers: Seq[MAiMadContentHandler]
) {
  import AiSource._

  /** Сериализация экземпляра. */
  def toPlayJson: JsObject = {
    JsObject(Seq(
      URL_ESFN -> JsString(url),
      CONTENT_HANDLERS_ESFN -> JsArray( contentHandlers.map(ch => JsString(ch.toString())) )
    ))
  }

}


/** Модель с доступными обработчиками контента. */
object MAiMadContentHandlers extends EnumMaybeWithName {
  protected sealed abstract class Val(val name: String) extends super.Val(name) {
    /** Собрать новый инстанс sax-парсера. */
    def newInstance(maim: MAiMad): DefaultHandler with GetParseResult
  }

  type MAiMadContentHandler = Val
  override type T = MAiMadContentHandler


  // Тут всякие доступные content-handler'ы.
  /** Sax-парсер для rss-прогнозов росгидромета. */
  val GidrometRss: MAiMadContentHandler = new Val("gidromet.rss") {
    override def newInstance(maim: MAiMad) = new GidrometRssSax(maim)
  }

}


/** Абстрактный результат работы Content-Handler'а. Это JavaBean-ы, поэтому должны иметь Serializable. */
trait ContentHandlerResult extends Serializable


/** Модель доступных рендереров динамических рекламных карточек. */
object MAiRenderers extends EnumMaybeWithName {
  /** Экземпляр модели. */
  protected sealed abstract class Val(val name: String) extends super.Val(name) {
    def getRenderer(): MadAiRenderedT
  }

  type MAiRenderer = Val
  override type T = MAiRenderer

  /** Вызов рендера шаблонов scalasti. */
  val ScalaSti: MAiRenderer = new Val("scalasti") {
    override def getRenderer() = ScalaStiRenderer
  }

}
