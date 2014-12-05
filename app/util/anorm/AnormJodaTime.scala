package util.anorm

import anorm.SqlParser._
import anorm._
import org.joda.time._
import org.joda.time.format._
import play.api.Play.current
import play.api.db.DB

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.04.13 11:40
 * Description: Функции поддержки joda-time в anorm. Запись и чтение.
 * http://stackoverflow.com/a/11975107
 */

object AnormJodaTime {

  private def dateFormatGeneration: DateTimeFormatter = DateTimeFormat.forPattern("yyyyMMddHHmmssSS")

  implicit def rowToDateTime: Column[DateTime] = Column.nonNull1 { (value, meta) =>
    value match {
      case ts: java.sql.Timestamp => Right(new DateTime(ts.getTime))
      case d: java.sql.Date => Right(new DateTime(d.getTime))
      case str: java.lang.String => Right(dateFormatGeneration.parseDateTime(str))
      case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass) )
    }
  }

  implicit val dateTimeToStatement = new ToStatement[DateTime] {
    def set(s: java.sql.PreparedStatement, index: Int, aValue: DateTime) {
      s.setTimestamp(index, new java.sql.Timestamp(aValue.withMillisOfSecond(0).getMillis) )
    }
  }


  private val dtParser = get[DateTime]("dt")

  /**
   * Распарсить дату, введеную пользователем.
   * В постгресе хороший, простой и универсальный парсер дат, но дергать базу для этого неправильно.
   * Следует задействовать парсеры из JodaTime.
   * @param dtStr строка даты в некотором формате.
   * @return
   */
  def parseDtStr(dtStr:String) = {
    DB.withConnection { implicit c =>
      SQL("SELECT {dtStr}::timestamptz AS dt")
      .on('dtStr -> dtStr)
      .as(dtParser single)
    }
  }

}
