package util.ai.sax.currency.cbrf

import models.ai.AiParsers.AiParser
import models.ai.{AiParsers, ContentHandlerResult}
import org.xml.sax.helpers.DefaultHandler
import util.PlayLazyMacroLogsImpl
import util.ai.AiContentHandler
import util.ai.sax.StackFsmSax

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.12.14 19:49
 * Description: Парсер для страниц сайта ЦБ РФ, содержащих XML-таблицу курсов за конкретный день.
 * @see [[http://www.cbr.ru/scripts/XML_daily.asp]]
 * @see [[http://www.cbr.ru/scripts/XML_daily_eng.asp]]
 */
class CbrfCurBaseDaySax
  extends DefaultHandler
  with StackFsmSax
  with AiContentHandler
  with PlayLazyMacroLogsImpl
{

  //var _accRev: List[]

  override def sourceParser: AiParser = AiParsers.SaxTolerant

  /**
   * Ключ-идентификатор, используемый для формирования карты результатов.
   * @return Строка, маленькими буквами и без пробелов.
   */
  override def stiResKey: String = "currency"

  /**
   * Вернуть накопленный результат парсинга.
   * Если результата нет или он заведомо неверный/бесполезный, то должен быть экзепшен с причиной.
   * @return Реализация модели ContentHandlerResult.
   */
  override def getParseResult: ContentHandlerResult = ???
}
