package io.suggest.react

import diode._
import diode.data.Pot
import diode.react.ModelProxy
import japgolly.scalajs.react.{BackendScope, Callback, Children, Reusability, ScalaComponent, UpdateSnapshot}
import japgolly.scalajs.react.vdom.VdomElement

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
  def dispatchOnProxyScopeCB[P: Props2ModelProxy, S, A: ActionType]($: BackendScope[P, S], msg: A): Callback = {
    dispatchOnProxyScopeCBf($)(_ => msg)
  }

  def dispatchOnProxyScopeCBf[P: Props2ModelProxy, S, A: ActionType]($: BackendScope[P, S])(f: P => A): Callback = {
    $.props >>= { p: P =>
      implicitly[Props2ModelProxy[P]]
        .apply(p)
        .dispatchCB( f(p) )
    }
  }


  /** Sjs-react поддержка для связывания FastEq и Reusability классов. */
  implicit class FastEqExtOps[A](val feq: FastEq[A]) extends AnyVal {

    def reusability: Reusability[A] =
      Reusability[A]( feq.eqv )

  }


  /** Функция извлечения value из ModelProxy.
    * Используется так: compBuilder.initialStateFromProps(modelProxyValueF)
    */
  def modelProxyValueF[T]: (ModelProxy[T] => T) = { modelProxy: ModelProxy[T] =>
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
  def p2sShouldComponentUpdate[P <: AnyRef, C <: Children, S <: AnyRef: FastEq, B, US <: UpdateSnapshot](p2s: P => S): ScalaComponent.Config[P, C, S, B, US, US] = {
    _.componentWillReceiveProps { $ =>
      val nextPropsVal = p2s( $.nextProps )
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
  def statePropsValShouldComponentUpdate[P <: ModelProxy[S], C <: Children, S <: AnyRef: FastEq, B, US <: UpdateSnapshot]: ScalaComponent.Config[P, C, S, B, US, US] =
    p2sShouldComponentUpdate( _.value )


  /** Конфигурирование компонента, чтобы сверялись props через FastEq.
    *
    * @tparam P Тип Props с поддержкой FastEq.
    * @return Сконфигуренный компонент.
    */
  def propsFastEqShouldComponentUpdate[P: FastEq, C <: Children, S, B, US <: UpdateSnapshot]: ScalaComponent.Config[P, C, S, B, US, US] = {
    _.shouldComponentUpdatePure { $ =>
      implicitly[FastEq[P]].neqv( $.currentProps, $.nextProps )
    }
  }


  object Implicits {

    implicit class ReactPotExtOps[T](val pot: Pot[T]) extends AnyVal {

      def renderEl(f: T => VdomElement): VdomElement = {
        if (pot.nonEmpty) f(pot.get)
        else ReactCommonUtil.VdomNullElement
      }

    }


    implicit class ModelProxyExt[A]( val model: ModelProxy[A] ) extends AnyVal {

      /** Сброс значения от зуммера на указанное значение. */
      def resetZoom[B <: AnyRef](v: B): ModelProxy[B] = {
        model.copy(
          modelReader = new RootModelRW(v)
        )
      }

    }

  }

}


/** Трейт для поддержки извлечения ModelProxy из любых пропертисов. */
trait Props2ModelProxy[-P] extends (P => ModelProxy[_])
object Props2ModelProxy {
  implicit object Raw extends Props2ModelProxy[ModelProxy[_]] {
    override def apply(v1: ModelProxy[_]) = v1
  }
  implicit def Plain[T]: Props2ModelProxy[ModelProxy[T]] = {
    Raw.asInstanceOf[Props2ModelProxy[ModelProxy[T]]]
  }
}
