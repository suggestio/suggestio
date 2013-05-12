package util

import play.api.Logger

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.04.13 18:13
 * Description: Добавить инстанс play-only логгера для текущего модуля.
 */

trait Logs {

  protected val logger = Logger(getClass.getName)

}
