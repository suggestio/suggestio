package util.anorm

import java.sql.PreparedStatement

import anorm.{MayErr, ToStatement, TypeDoesNotMatch, Column}
import io.suggest.ym.model.common.AdnSinks
import models.AdnSink

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.08.14 16:13
 * Description: Утиль для прозрачной поддержки типа AdnSink в anorm'е.
 */
object AnormAdnSink {

  implicit def rowToSink: Column[AdnSink] = Column.nonNull { (value, meta) =>
    val res = value match {
      case s: String =>
        Right( AdnSinks.withName(s) : AdnSink )
      case c: Char =>
        Right( AdnSinks.withName(c.toString) : AdnSink )
      case other =>
        Left(TypeDoesNotMatch(s"Cannot convert $other : ${other.getClass.getName} to AdnSink"))
    }
    MayErr(res)
  }


  implicit val sinkToRow = new ToStatement[AdnSink] {
    override def set(s: PreparedStatement, index: Int, v: AdnSink): Unit = {
      s.setString(index, v.name)
    }
  }

}
