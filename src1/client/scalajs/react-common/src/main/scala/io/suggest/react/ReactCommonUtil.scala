package io.suggest.react

import japgolly.scalajs.react.{CallbackTo, ReactElement}

import scala.scalajs.js

import scala.language.implicitConversions

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.01.17 15:35
  * Description: Вспомогательная утиль для react-компонентов.
  */
object ReactCommonUtil {

  /**
    * Приведение функции, возвращающей js Callback к js-функции-листенеру.
    * Это некая замена "==>" для pure-js-компонентов.
    * Без implicit, т.к. оно не работает в данном нетривиальном случае.
    *
    * @param fun Исходная фунция, возвращающая Callback.
    * @tparam Arg Тип исходного аргумента.
    * @tparam Res Тип возвращаемого из Callback'а значения.
    * @return js.Function1
    *
    * @see [[https://github.com/japgolly/scalajs-react/issues/210#issuecomment-149991727]]
    */
  def cbFun1TojsCallback[Arg, Res](fun: Arg => CallbackTo[Res]): js.Function1[Arg, Res] = {
    fun
      .andThen( _.runNow() )
  }

  // unused
  def cbFun2TojsCallback[Arg1, Arg2, Res](fun: (Arg1, Arg2) => CallbackTo[Res]): js.Function2[Arg1, Arg2, Res] = {
    { (arg1, arg2) =>
      fun(arg1, arg2)
        .runNow()
    }
  }


  object Implicits {

    /** Приведение Option[ReactElement] к ReactElement. Чтобы не писать везде .orNull */
    implicit def reactElOpt2reactEl[T <: ReactElement](rElOpt: Option[T]): T = {
      // TODO Option.orNull тут не компилится почему-то.
      if (rElOpt.isEmpty) null.asInstanceOf[T] else rElOpt.get
    }

  }

}
