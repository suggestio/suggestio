package io.suggest.url.bind

import japgolly.univeq._

/** Cross-platform trait for play.QueryStringBindable .
  * Used for writing cross-platform URL QS bindables, fully compatible with play.QSB.
  * Interface slightly different: bind/unbind method replaced with functions to simplify client-server code intergration.
  *
  * @tparam T Type of bindable value.
  */
trait QsBindable[T] {

  /** URL query-string binding function. */
  def bindF: QsBinderF[T]

  /** URL query-string unbinding function. */
  def unbindF: QsUnbinderF[T]

}

object QsBindable {

  implicit def qsBindableOption[T](implicit innerB: QsBindable[T]): QsBindable[Option[T]] = {
    new QsBindable[Option[T]] {
      override def bindF: QsBinderF[Option[T]] =
        (key, params) =>
          innerB.bindF(key, params)
            .map( _.map(Some.apply) )
            .orElse( Some(Right(Option.empty[T])) )

      override def unbindF: QsUnbinderF[Option[T]] = {
        (key, valueOpt) =>
          valueOpt.fold("")( innerB.unbindF(key, _) )
      }
    }
  }


  def optionDefaulted[T: UnivEq](default: => T)(implicit innerOptB: QsBindable[Option[T]]): QsBindable[T] = {
    new QsBindable[T] {
      override def bindF: QsBinderF[T] = { (key, params) =>
        innerOptB.bindF(key, params)
          .map(_.map(_ getOrElse default))
      }
      override def unbindF: QsUnbinderF[T] = { (key, value) =>
        innerOptB.unbindF( key, Option.when(value !=* default )(value) )
      }
    }
  }


  def defaulted[T: UnivEq](default: => T)(implicit innerB: QsBindable[T]): QsBindable[T] = {
    new QsBindable[T] {
      override def bindF: QsBinderF[T] = { (key, params) =>
        innerB.bindF( key, params )
          .orElse( Some(Right(default)) )
      }
      override def unbindF: QsUnbinderF[T] = {
        implicit val optB = qsBindableOption(innerB)
        optionDefaulted(default).unbindF
      }
    }
  }

}