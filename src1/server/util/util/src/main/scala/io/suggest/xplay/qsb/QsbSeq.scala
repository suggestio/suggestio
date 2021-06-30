package io.suggest.xplay.qsb

import io.suggest.url.bind.QsbSeqUtil
import japgolly.univeq.UnivEq
import play.api.mvc.QueryStringBindable

/** Marker class container for wrapping immutable sequence.
  * Play router will implicitly compile Seq[] binder against [[QsbSeq]], when Seq wrapped inside QsbSeq. */
final case class QsbSeq[T]( items: Seq[T] )


object QsbSeq {

  @inline implicit def univEq[T: UnivEq]: UnivEq[QsbSeq[T]] = UnivEq.derive

  def seqQsb[T](implicit innerB: QueryStringBindable[T]): QueryStringBindable[Seq[T]] = {
    import CrossQsBindable._
    QsbSeqUtil.qsbSeqQsB(innerB)
  }

  /** Indexed QSB for sequences. */
  implicit def qsbSeqQsb[T](implicit innerB: QueryStringBindable[T]): QueryStringBindable[QsbSeq[T]] = {
    seqQsb(innerB).transform[QsbSeq[T]]( QsbSeq.apply, _.items )
  }

}
