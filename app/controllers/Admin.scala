package controllers

import play.api.mvc.Controller
import util.{Logs, ContextT, AclT}

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
 */

object Admin extends Controller with AclT with ContextT with Logs {



}
