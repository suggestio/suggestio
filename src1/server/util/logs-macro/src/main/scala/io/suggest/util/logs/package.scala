package io.suggest.util

import scala.sys.process.ProcessLogger

import com.typesafe.scalalogging.{Logger => MacroLogger}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.02.18 12:44
  */
package object logs {

  /** Дополнительное API для макро-логгеров. */
  implicit class MacroLoggerExtOps(val logger: MacroLogger) extends AnyVal {

    def process: ProcessLogger = {
      ProcessLogger(logger.debug(_), logger.error(_))
    }

  }

}
