package io.suggest.sjs.common.log

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.11.16 16:25
  * Description: Sjs-логгирование v2 подразумевает использование маркеров severity
  * для передачи в реализации логгеров вместе с сообщениями и прочими делами.
  */
object Severities {

  val Error : Severity = 9

  val Warn  : Severity = 7

  val Info  : Severity = 5

  val Log   : Severity = 3

}
