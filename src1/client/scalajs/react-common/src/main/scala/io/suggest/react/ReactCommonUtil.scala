package io.suggest.react

import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.CallbackTo
import japgolly.scalajs.react.internal.OptionLike

import scala.scalajs.js
import scala.language.{implicitConversions, higherKinds}

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
  def cbFun1ToJsCb[Arg, Res](fun: Arg => CallbackTo[Res]): js.Function1[Arg, Res] = {
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


  val VdomNullElement: VdomElement = {
    VdomElement(null)
  }

  final class VdomElOptionExt[O[_], A](o: O[A])(implicit O: OptionLike[O]) {
    def whenDefinedEl(f: A => VdomElement): VdomElement =
      O.fold(o, VdomNullElement)(f)
  }


  object Implicits {

    /** Приведение Option[VdomElement] к VdomElement.
      * Это чтобы не мудрить везде с .orNull и прочими EmptyVdom, и заодно решить проблему с diode connect/wrap.
      */
    implicit def vdomElOptionExt[O[_], A](o: O[A])(implicit O: OptionLike[O]): VdomElOptionExt[O, A] = {
      new VdomElOptionExt(o)
    }

  }

}
