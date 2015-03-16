package io.suggest.adv.ext.view

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.03.15 10:30
 * Description: Метаданные html-элементов на странице runner'а.
 */
object RunnerPage {

  /** id элемента, содержащего в value данные по коннекшену. */
  def ID_WS_URL = "socialApiConnection"

  /** id элемента, куда будут рендерится события. */
  def ID_EVTS_CONTAINER = "adv-events"

  /** Отображать технические подробности ошибки при щелчке на элементы с указанным классом. */
  def CLASS_JSLINK_SHOW_ERROR = "js-social-show-error"

}
