package io.suggest.sjs.common.empty

import scala.scalajs.js
import scala.scalajs.js.UndefOr

import scala.language.implicitConversions

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.01.17 15:26
  * Description: Доп.утиль для работы с option'ами на уровне js.
  */
object JsOptionUtil {

  object Implicits {

    implicit class JsOptionExt[T](opt: Option[T]) {

      @inline
      def flatMapDefined[X](f: T => js.UndefOr[X]): js.UndefOr[X] =
        opt.fold [UndefOr[X]] (js.undefined)(f)

      @inline
      def mapDefined[X](f: T => X): js.UndefOr[X] =
        flatMapDefined(m => js.defined(f(m)))

      @inline
      def toUndef: js.UndefOr[T] =
        mapDefined(identity)

    }

  }


  def maybeDefined[T](isDefined: Boolean)(f: => T): js.UndefOr[T] = {
    if (isDefined)
      js.defined(f)
    else
      js.undefined
  }

}
