package controllers

import play.api.mvc.Controller
import util.{Logs, ContextT, AclT}
import models._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.06.13 14:59
 * Description: Пользовательская админка сайтов. Основные функции:
 * - Рендер основной формы.
 * - Валидация доменов (не-qi).
 * - Управление доменами поиска.
 * - Сохранения настроек поиска.
 * - Другие команды.
 *
 * Исходное API сохраняется (как в прошлой версии).
 */

object Admin extends Controller with AclT with ContextT with Logs {

  /**
   * Юзер заходит в админку, на главную её страницу.
   * @return Нужно отрендерить главную форму админки со списком доменов и прочими причиндалами.
   */
  def index = isAuthenticated { implicit pw_opt => implicit request =>
    val pw = pw_opt.get
    val personDomains = pw.allDomainsAuthz
    // Т.к. в фоне будет запущена валидация доменов, надо ещё запустить очередь новостей, которая потом будет перецеплена на веб-сокет.

    ???
  }

}
