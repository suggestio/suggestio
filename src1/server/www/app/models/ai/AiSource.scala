package models.ai

import io.suggest.es.util.SioEsUtil._
import _root_.play.api.libs.json.{JsArray, JsObject, JsString}
import java.{util => ju}

import io.suggest.util.JacksonParsing

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.12.14 19:56
 * Description: AiSource описывает источник данных и экстракторы контента, применяемые для него.
 */

object AiSource {

  val URL_ESFN                = "url"
  val CONTENT_HANDLERS_ESFN   = "chs"

  def generateMappingProps: Seq[DocField] = {
    Seq(
      FieldString(URL_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = true),
      FieldString(CONTENT_HANDLERS_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false)
    )
  }

  /** Распарсить из jackson json ранее сериализованный экземпляр [[AiSource]]. */
  def parseJson(x: Any): AiSource = {
    x match {
      case jmap: ju.Map[_,_] =>
        AiSource(
          url = Option( jmap.get(URL_ESFN) )
            .map(JacksonParsing.stringParser)
            .getOrElse(""),
          contentHandlers = Option( jmap.get(CONTENT_HANDLERS_ESFN) )
            .map { rawChs =>
              JacksonParsing.iteratorParser(rawChs)
                .map { rawChId => MAiMadContentHandlers.withName(JacksonParsing.stringParser(rawChId)): MAiMadContentHandler }
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
