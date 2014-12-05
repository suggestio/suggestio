package util.ai

import models.ai.AiParsers.AiParser
import models.ai.ContentHandlerResult

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.11.14 13:05
 * Description: Утиль для построения автозаполняемых карточек.
 */

/** Интерфейс для чтения результата из ContentHandler'ов */
trait AiContentHandler {

  /**
   * Ключ-идентификатор, используемый для формирования карты результатов.
   * @return Строка, маленькими буквами и без пробелов.
   */
  def stiResKey: String

  /**
   * Вернуть накопленный результат парсинга.
   * Если результата нет или он заведомо неверный/бесполезный, то должен быть экзепшен с причиной.
   * @return Реализация модели ContentHandlerResult.
   */
  def getParseResult: ContentHandlerResult

  /**
   * Какой парсер сырца надо использовать с этим ContentHandler'ом?
   * @return tika, sax и др.
   */
  def sourceParser: AiParser

}
