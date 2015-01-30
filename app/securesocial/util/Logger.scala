package securesocial.util

import play.api.Logger

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.12.14 17:46
 * Description: Logger implementations for any classes/objects.
 */
trait ILogger {
  def logger: Logger
}

trait LoggerImpl extends ILogger {
  override val logger = Logger(getClass)
}

trait LazyLoggerImpl extends ILogger {
  override lazy val logger = Logger(getClass)
}
