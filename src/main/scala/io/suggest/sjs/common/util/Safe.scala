package io.suggest.sjs.common.util

import io.suggest.sjs.common.log.ILog

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.04.15 11:37
 * Description: Утиль для подавления исключений при выховах.
 */
trait SafeSyncVoid extends ILog {

  protected def _safeSyncVoid(f: () => Unit): Unit = {
    try {
      f()
    } catch {
      case ex: Throwable =>
        LOG.error(msg = "safe(): Error suppressed", ex = ex)
    }
  }

}
