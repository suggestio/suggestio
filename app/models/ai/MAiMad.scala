package models.ai

import io.suggest.model.EsModel.FieldsJsonAcc
import io.suggest.model._
import io.suggest.util.SioEsUtil._
import org.xml.sax.helpers.DefaultHandler
import play.api.libs.json.{JsArray, JsString}
import util.PlayMacroLogsImpl
import util.ai.GetParseResult
import util.ai.mad.render.{ScalaStiRenderer, MadAiRenderedT}
import util.ai.sax.weather.gidromet.GidrometRssSax

import scala.collection.Map

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
  val URL_ESFN                = "url"
  val CONTENT_HANDLERS_ESFN   = "chs"
  val TPL_AD_ID_ESFN          = "tpl"
  val TARGET_AD_IDS_ESFN      = "tgts"
  val DESCR_ESFN              = "d"
  val RENDERERS_ESFN          = "rr"

  override def generateMappingStaticFields: List[Field] = {
    List(
      FieldAll(enabled = true),
      FieldSource(enabled = true)
    )
  }

  override def generateMappingProps: List[DocField] = {
    List(
      FieldString(NAME_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = false),
      FieldString(URL_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = true),
      FieldString(CONTENT_HANDLERS_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldString(TPL_AD_ID_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldString(DESCR_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = true),
      FieldString(RENDERERS_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = true)
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
      url  = stringParser(m(URL_ESFN)),
      contentHandlers = iteratorParser( m(CONTENT_HANDLERS_ESFN) )
        .map { chId => MAiMadContentHandlers.withName(stringParser(chId)): MAiMadContentHandler }
        .toSeq,
      tplAdId = stringParser(m(TPL_AD_ID_ESFN)),
      renderers = iteratorParser(m(RENDERERS_ESFN))
        .map { s => MAiRenderers.withName(stringParser(s)) : MAiRenderer }
        .toSeq,
      targetAdIds = strListParser(m(TARGET_AD_IDS_ESFN)),
      descr = m.get(DESCR_ESFN).map(stringParser),
      id = id,
      versionOpt = version
    )
  }

}


import MAiMad._


case class MAiMad(
  name            : String,
  url             : String,
  contentHandlers : Seq[MAiMadContentHandler],
  tplAdId         : String,
  renderers       : Seq[MAiRenderer],
  targetAdIds     : Seq[String],
  descr           : Option[String] = None,
  id              : Option[String] = None,
  versionOpt      : Option[Long] = None
) extends EsModelT with EsModelPlayJsonT {

  override type T = this.type
  override def companion = MAiMad

  override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    var acc1: FieldsJsonAcc =
      NAME_ESFN               -> JsString(name) ::
      URL_ESFN                -> JsString(url) ::
      CONTENT_HANDLERS_ESFN   -> JsArray( contentHandlers.map(ch => JsString(ch.toString())) ) ::
      TPL_AD_ID_ESFN          -> JsString(tplAdId) ::
      TARGET_AD_IDS_ESFN      -> JsArray( targetAdIds.map(JsString.apply) ) ::
      acc
    if (renderers.nonEmpty)
      acc1 ::= RENDERERS_ESFN -> JsArray(renderers.map(rr => JsString(rr.name)))
    if (descr.isDefined)
      acc1 ::= DESCR_ESFN -> JsString(descr.get)
    acc
  }

}


/** Модель с доступными обработчиками контента. */
object MAiMadContentHandlers extends EnumMaybeWithName {
  protected sealed abstract class Val(val name: String) extends super.Val(name) {
    /** Собрать новый инстанс sax-парсера. */
    def newInstance: DefaultHandler with GetParseResult
  }

  type MAiMadContentHandler = Val
  override type T = MAiMadContentHandler


  // Тут всякие доступные content-handler'ы.
  /** Sax-парсер для rss-прогнозов росгидромета. */
  val GidrometRss: MAiMadContentHandler = new Val("gidromet.rss") {
    override def newInstance = new GidrometRssSax
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
