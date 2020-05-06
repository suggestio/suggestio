package io.suggest.log

import enumeratum.values.{IntEnum, IntEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.11.16 16:25
  * Description: Sjs-логгирование v2 подразумевает использование маркеров severity
  * для передачи в реализации логгеров вместе с сообщениями и прочими делами.
  */
object LogSeverities extends IntEnum[LogSeverity] {

  case object Error extends LogSeverity( 9 )

  case object Warn extends LogSeverity(7 )

  case object Info extends LogSeverity( 5 )

  case object Log extends LogSeverity( 3 )


  override def values = findValues

}


sealed abstract class LogSeverity( override val value: Int ) extends IntEnumEntry
object LogSeverity {

  @inline implicit def univEq: UnivEq[LogSeverity] = UnivEq.derive

  implicit def logSeverityJson: Format[LogSeverity] =
    EnumeratumUtil.valueEnumEntryFormat( LogSeverities )

}
