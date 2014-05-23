package util

import org.postgresql.util.PGInterval
import anorm.{ToStatement, TypeDoesNotMatch, Column}
import java.sql.PreparedStatement
import org.joda.time.Period

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.04.14 19:20
 * Description: Поддержка PGInterval в anorm. Позволяет прозрачно работать с периодами.
 */
object AnormPgInterval {

  implicit def rowToPgInterval: Column[PGInterval] = Column.nonNull { (value, meta) =>
    value match {
      case pgi: PGInterval =>
        Right(pgi)
      case str: String =>
        Right(new PGInterval(str))
      case _ =>
        Left(TypeDoesNotMatch("Cannot convert " + value + ": " + value.asInstanceOf[AnyRef].getClass) )
    }
  }

  implicit val pgIntervalToStatement = new ToStatement[PGInterval] {
    override def set(s: PreparedStatement, index: Int, v: PGInterval) {
      s.setObject(index, v)
    }
  }

  /** Конвертор PGInterval в joda.Period. */
  implicit def pgInterval2jodaPeriod(pgi: PGInterval): Period = {
    // Костыли с секундами и миллисекундами.
    val seconds = pgi.getSeconds.toInt
    val ms = ((pgi.getSeconds - seconds) * 1000).toInt
    new Period(pgi.getYears, pgi.getMonths, 0, pgi.getDays,
               pgi.getHours, pgi.getMinutes, seconds, ms)
  }

  /** Конвертор joda.time.Period -> PGInterval, т.е. обратный к [[pgInterval2jodaPeriod()]]. */
  implicit def jodaPeriod2pgInterval(period: Period): PGInterval = {
    new PGInterval(
      period.getYears, period.getMonths, period.getDays,
      period.getHours, period.getMinutes, period.getSeconds
    )
  }

  /** Поддержка joda period в SQL.on(). */
  implicit val periodToStatement = new ToStatement[Period] {
    override def set(s: PreparedStatement, index: Int, v: Period) = {
      val pgi = jodaPeriod2pgInterval(v)
      s.setObject(index, pgi)
    }
  }

}

