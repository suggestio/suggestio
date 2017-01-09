package io.suggest.sjs.common.model.browser

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.06.15 13:27
 * Description: Интерфейс для реализаций аддонов для детектирования браузеров.
 */
trait IBrowserDetector {

  /**
   * Попытаться определить браузер на основе имеющихся данных окружения.
   * @return Some() если удалось идентифицировать браузер.
   *         Иначе None.
   */
  def detectBrowser(ua: Option[String]): Option[IBrowser] = None

}
