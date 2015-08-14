package util.qsb

import play.api.mvc.QueryStringBindable
import scala.language.implicitConversions

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.08.15 13:36
 * Description: QSB для коллекций/массивов строк, сериализованных в одну строку.
 */

trait CharDelimitedStringSeq {

  protected trait CharDelimitedStringSeqQsb extends QueryStringBindable[Seq[String]] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Seq[String]]] = {
      val ch = cdssDelimChar
      val res = params.get(key)
        .iterator
        .flatMap(_.iterator)
        .filter { !_.isEmpty }
        .flatMap { v => v.split(ch) }
        .toSeq
      Some(Right(res))
    }

    override def unbind(key: String, value: Seq[String]): String = {
      value.mkString(cdssDelimChar.toString)
    }
  }

  /** char. delimiter. */
  protected def cdssDelimChar: Char

  implicit def cdssQsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[Seq[String]] = {
    new CharDelimitedStringSeqQsb {}
  }

}

trait CommaDelimitedStringSeq extends CharDelimitedStringSeq {
  override protected def cdssDelimChar: Char = ','
}
