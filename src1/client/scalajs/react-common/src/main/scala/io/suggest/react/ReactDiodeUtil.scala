package io.suggest.react

import diode._
import diode.react.ModelProxy
import japgolly.scalajs.react.{BackendScope, Callback, Children, ScalaComponent}
import japgolly.scalajs.react.extra.Reusability

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.03.17 22:02
  * Description: Утиль, упрощающая работу с diode и react.
  */
object ReactDiodeUtil {

  /** При react-рендере через backend часто бывает ситуация, что нужно быстро органиновать callback.
    * Props являются modelProxy, $ является scope'ом.
    *
    * @param $ Backend scope as-is.
    * @param msg Сообщение для диспатчинга.
    * @tparam P Тип Backend Props, просто компилятор требует, для метода главное чтобы ModelProxy.
    * @tparam S Тип Backend State, просто компилятор требует, для метода не важен.
    * @tparam A Тип сообщения msg.
    * @return Callback.
    */
  def dispatchOnProxyScopeCB[P <: ModelProxy[_], S, A: ActionType]($: BackendScope[P, S], msg: A): Callback = {
    dispatchOnProxyScopeCBf($)(_ => msg)
  }

  def dispatchOnProxyScopeCBf[P <: ModelProxy[_], S, A: ActionType]($: BackendScope[P, S])(f: P => A): Callback = {
    $.props >>= { p: P =>
      p.dispatchCB( f(p) )
    }
  }


  /** Sjs-react поддержка для связывания FastEq и Reusability классов. */
  implicit class FastEqExtOps[A](val feq: FastEq[A]) extends AnyVal {

    def reusability: Reusability[A] = {
      Reusability[A] { feq.eqv }
    }

  }


  /** Функция извлечения value из ModelProxy.
    * Используется так: compBuilder.initialStateFromProps(modelProxyValueF)
    */
  def modelProxyValueF[T] = { modelProxy: ModelProxy[T] =>
    modelProxy.value
  }

  /** Конфигурирование ModelProxy-компонента, чтобы кэшировал значение из ModelProxy внутри State,
    * и сравнивал его перед апдейтом компонента.
    * Бывает, что ReactConnector нельзя задействовать, но очень хочется. И тут аналог этого.
    *
    * @tparam P ModelProxy[S]
    * @tparam S AnyRef
    * @return Пропатченный компонент, который обновляется (и обновляет S) только если P.value отличается от S.
    */
  def statePropsValShouldComponentUpdate[P <: ModelProxy[S], C <: Children, S <: AnyRef: FastEq, B]: ScalaComponent.Config[P, C, S, B] = {
    _.componentWillReceiveProps { $ =>
      val nextPropsVal = $.nextProps.value
      val currPropsVal = $.state
      if ( implicitly[FastEq[S]].eqv(currPropsVal, nextPropsVal) ) {
        Callback.empty
      } else {
        $.setState(nextPropsVal)
      }
    }.shouldComponentUpdatePure { $ =>
      $.currentState ne $.nextState
    }
  }


  /** Конфигурирование компонента, чтобы сверялись props через FastEq.
    *
    * @tparam P Тип Props с поддержкой FastEq.
    * @return Сконфигуренный компонент.
    */
  def propsFastEqShouldComponentUpdate[P: FastEq, C <: Children, S, B]: ScalaComponent.Config[P, C, S, B] = {
    _.shouldComponentUpdatePure { $ =>
      implicitly[FastEq[P]].neqv( $.currentProps, $.nextProps )
    }
  }

}

