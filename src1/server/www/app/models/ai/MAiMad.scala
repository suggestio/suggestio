package models.ai

import java.time.ZoneId

import io.suggest.util.JacksonParsing.FieldsJsonAcc
import javax.inject.{Inject, Singleton}
import io.suggest.es.model._
import io.suggest.es.util.SioEsUtil._
import io.suggest.primo.id.OptStrId
import models.mproj.ICommonDi
import play.api.libs.json.{JsArray, JsString}
import io.suggest.util.JacksonParsing.{iteratorParser, strListParser, stringParser}
import io.suggest.util.logs.MacroLogsImpl

import scala.collection.Map
import scala.concurrent.ExecutionContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.11.14 11:03
 * Description: Описание для будущих рекламных карточек.
 */

// TODO Объеденить модель с MNodes, когда дойдут до AiMad руки.

@Singleton
class MAiMads @Inject() (
  override val mCommonDi: ICommonDi
)
  extends EsModelStatic
    with MacroLogsImpl
    with EsModelPlayJsonStaticT
{

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
      FieldText(NAME_ESFN, index = true, include_in_all = false),
      FieldKeyword(TPL_AD_ID_ESFN, index = true, include_in_all = false),
      FieldText(DESCR_ESFN, index = true, include_in_all = true),
      FieldKeyword(RENDERERS_ESFN, index = true, include_in_all = true),
      FieldText(TIMEZONE_ESFN, index = true, include_in_all = true),
      FieldObject(SOURCES_ESFN, enabled = true, properties = AiSource.generateMappingProps)
    )
  }

  /**
   * Десериализация одного элементам модели.
   * @param id id документа.
   * @param m Карта, распарсенное json-тело документа.
   * @return Экземпляр модели.
   */
  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): T = {
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
        .fold(ZoneId.systemDefault()) { tzRaw => ZoneId.of(stringParser(tzRaw)) },
      id = id,
      versionOpt = version
    )
  }


  override def writeJsonFields(m: T, acc: FieldsJsonAcc): FieldsJsonAcc = {
    import m._
    var acc1: FieldsJsonAcc =
      NAME_ESFN               -> JsString(name) ::
      SOURCES_ESFN            -> JsArray(sources.map(_.toPlayJson)) ::
      TPL_AD_ID_ESFN          -> JsString(tplAdId) ::
      TARGET_AD_IDS_ESFN      -> JsArray( targetAdIds.map(JsString.apply) ) ::
      TIMEZONE_ESFN           -> JsString(tz.getId) ::
      acc
    if (renderers.nonEmpty)
      acc1 ::= RENDERERS_ESFN -> JsArray(renderers.map(rr => JsString(rr.name)))
    if (descr.isDefined)
      acc1 ::= DESCR_ESFN -> JsString(descr.get)
    acc1
  }

}
/** Интерфейс к полю с DI-инстансом [[MAiMads]]. */
trait IMAiMads {
  def mAiMads: MAiMads
}


case class MAiMad(
                   name            : String,
                   sources         : Seq[AiSource],
                   tplAdId         : String,
                   renderers       : Seq[MAiRenderer],
                   targetAdIds     : Seq[String],
                   tz              : ZoneId,
                   descr           : Option[String] = None,
                   id              : Option[String] = None,
                   versionOpt      : Option[Long] = None
)
  extends EsModelT
    with MAiCtx


/** JMX MBean для модели [[MAiMad]]. */
trait MAiMadJmxMBean extends EsModelJMXMBeanI
/** Реализация JMX MBean для модели [[MAiMad]]. */
final class MAiMadJmx @Inject() (
  override val companion  : MAiMads,
  override val ec         : ExecutionContext
)
  extends EsModelJMXBaseImpl
    with MAiMadJmxMBean
{
  override type X = MAiMad
}


/** Интерфейс разных MAi-моделей: [[MAiMad]] и возможных других в будущем.
  * Нужен, чтобы абстрагироваться от конкретных моделей на уровне общих полей этих моделей. */
trait MAiCtx extends OptStrId {
  def tz: ZoneId
  def name: String
}
/** Враппер для трейта [[MAiCtx]]. Позволяет переопределять значения некоторых полей. */
trait MAiCtxWrapper extends MAiCtx {
  def mAiCtx: MAiCtx

  override def tz     = mAiCtx.tz
  override def name   = mAiCtx.name
  override def id     = mAiCtx.id
}
