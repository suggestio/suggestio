package io.suggest.react

import diode.ActionType
import diode.react.ModelProxy
import japgolly.scalajs.react.{BackendScope, Callback, ReactEvent}

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
  def dispatchOnProxyScopeCB[P <: ModelProxy[_], S, A]($: BackendScope[P, S], msg: A)
                                                      (implicit ev: ActionType[A]): Callback = {
    $.props >>= { p =>
      p.dispatchCB( msg )
    }
  }


  def eStopPropagationCB(e: ReactEvent): Callback = {
    e.stopPropagationCB
  }

}
