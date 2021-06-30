package io.suggest.xplay.qsb

import io.suggest.url.bind.QsBindable
import play.api.mvc.QueryStringBindable


/** Cross (client & server) URL query-string bindable.
  * Used for re-using shared-code binders (QsBindable) with server-only play.QueryStringBindable.
  */
sealed abstract class CrossQsBindable[T]
  extends AbstractQueryStringBindable[T]
  with QsBindable[T]


object CrossQsBindable {

  /** Adapt Play-framework scala QueryStringBindable. */
  implicit final class PlayScalaQsb[T](qsb: QueryStringBindable[T]) extends CrossQsBindable[T] {
    override def bindF = qsb.bind
    override def unbindF = qsb.unbind
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, T]] =
      qsb.bind(key, params)
    override def unbind(key: String, value: T): String =
      qsb.unbind(key, value)
    override def toString = qsb.toString
  }

  /** Adapt shared suggest.io code QsBindable. */
  implicit final class SioCommonQsB[T](qsb: QsBindable[T]) extends CrossQsBindable[T] {
    override def bindF = qsb.bindF
    override def unbindF = qsb.unbindF
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, T]] =
      qsb.bindF(key, params)
    override def unbind(key: String, value: T): String =
      qsb.unbindF(key, value)
    override def toString = qsb.toString
  }

}
