package io.suggest.sjs.common.empty

import scala.scalajs.js

import scala.language.implicitConversions

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.01.17 15:26
  * Description: Доп.утиль для работы с option'ами на уровне js.
  */
object JsOptionUtil {

  object Implicits {

    implicit class JsOptionExt[T](val opt: Option[T]) extends AnyVal {

      @inline
      def flatMapDefined[X](f: T => js.UndefOr[X]): js.UndefOr[X] =
        opt.fold [js.UndefOr[X]] (js.undefined)(f)

      @inline
      def mapDefined[X](f: T => X): js.UndefOr[X] =
        flatMapDefined(m => js.defined(f(m)))

      @inline
      def toUndef: js.UndefOr[T] =
        mapDefined(identity)

    }


    implicit class UndefExt[T <: AnyRef](val und: js.UndefOr[T]) extends AnyVal {

      /** Приведеление null/undefined к None, значение - к Some(). */
      def toOptionNullable: Option[T] = {
        und
          .toOption
          // фильтруем null внутри undefined:
          .flatMap( Option.apply )
      }

    }

  }


  def maybeDefined[T](isDefined: Boolean)(f: => T): js.UndefOr[T] = {
    if (isDefined)
      js.defined(f)
    else
      js.undefined
  }

}
