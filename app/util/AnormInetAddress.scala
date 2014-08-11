package util

import java.net.InetAddress
import java.sql.PreparedStatement

import anorm.{ToStatement, TypeDoesNotMatch, Column}
import org.postgresql.util.PGobject

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.08.14 16:43
 * Description: Хелперы для поддержки java.net.InetAddress в anorm.
 */
sealed trait AnormNetAddrs {
  def PG_TYPE: String

  implicit def column2inetAddr: Column[InetAddress] = Column.nonNull { (value, meta) =>
    value match {
      case pgo: PGobject =>
        // TODO Проверять pgo.getType?
        Right(InetAddress.getByName(pgo.getValue))
      case s: String =>
        Right(InetAddress.getByName(s))
      case _ =>
        Left(TypeDoesNotMatch("Cannot convert " + value + ": " + value.asInstanceOf[AnyRef].getClass))
    }
  }

  implicit def inetAddr2column = new ToStatement[InetAddress] {
    override def set(s: PreparedStatement, index: Int, v: InetAddress) {
      val pgo = new PGobject()
      pgo setType PG_TYPE
      pgo setValue v.getHostAddress
      s.setObject(index, pgo)
    }
  }

}

object AnormInetAddress extends AnormNetAddrs {
  override def PG_TYPE = "inet"
}
