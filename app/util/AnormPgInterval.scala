package util

import org.postgresql.util.PGInterval
import anorm.{ToStatement, TypeDoesNotMatch, Column}
import java.sql.PreparedStatement

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

}

