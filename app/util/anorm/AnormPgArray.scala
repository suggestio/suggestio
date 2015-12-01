package util.anorm

import java.sql
import java.sql.{Connection, PreparedStatement}

import anorm._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.04.13 10:32
 * Description: Кое-какие функции для работы с массивами постгреса.
 */

object AnormPgArray {

  /**
   * Конверсия pg-типа integer[] в скаловский Seq[Int]
   * https://groups.google.com/d/msg/play-framework/qs7mbWwIjjY/a6M_xPaNFggJ
   * @return
   */
  implicit def rowToSeqInt: Column[Seq[Int]] = Column.nonNull1 { (value, meta) =>
    value match {
      case arr: java.sql.Array =>
        Right(arr.getArray.asInstanceOf[Array[Integer]].toSeq.map(_.intValue) )
      case _ =>
        Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass + " to Seq[String] for column " + meta.column))
    }
  }


  implicit def rowToStringSet: Column[Set[String]] = Column.nonNull1 { (value, meta) =>
    value match {
      case arr: java.sql.Array =>
        Right(arr.getArray.asInstanceOf[Array[String]].toSet)
      case _ =>
        Left(TypeDoesNotMatch(s"Cannot conv. $value :: ${value.asInstanceOf[AnyRef].getClass} to Set[String] for column ${meta.column}"))
    }
  }


  def seqInt2pgArray(l: TraversableOnce[Int])(implicit c:Connection) = {
    c.createArrayOf("int", l.toIterator.map(int2Integer).toArray)
  }

  def strings2pgArray(l: TraversableOnce[String])(implicit c: Connection) = {
    c.createArrayOf("varchar", l.toArray)
  }

  implicit val arrayToStatement = new ToStatement[java.sql.Array] {
    def set(s: PreparedStatement, index: Int, v: sql.Array) {
      s.setArray(index, v)
    }
  }

  /**
   * Распознование массива строк в Seq[String].
   * @return Seq[String] или экзепшен.
   */
  implicit def rowToSeqString: Column[Seq[String]] = Column.nonNull1 { (value, meta) =>
    value match {
      case arr: java.sql.Array =>
        Right(arr.getArray.asInstanceOf[Array[String]].toSeq)
      case _ =>
        Left(TypeDoesNotMatch("Cannot convert %s: %s to Seq[String] for column %s".format(value, value.getClass, meta.column)))
    }
  }

}
