package util.ai

import models.ai.ContentHandlerResult

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.11.14 13:05
 * Description: Утиль для построения автозаполняемых карточек.
 */

/** Интерфейс для чтения результата из ContentHandler'ов */
trait GetParseResult {
  /**
   * Вернуть накопленный результат парсинга.
   * Если результата нет или он заведомо неверный/бесполезный, то должен быть экзепшен с причиной.
   * @return Реализация модели ContentHandlerResult.
   */
  def getParseResult: ContentHandlerResult
}
