package io.suggest.sjs.common.util

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.04.15 11:37
 * Description: Утиль для подавления исключений при выховах.
 */
trait SafeSyncVoid extends ISjsLogger {

  protected def _safeSyncVoid(f: () => Unit): Unit = {
    try {
      f()
    } catch {
      case ex: Throwable =>
        error("safe(): Error suppressed", ex)
    }
  }

}
