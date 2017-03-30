package io.suggest.lk.r

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
    * @tparam A Тип сообщения.
    * @tparam P Тип Backend Props, просто компилятор требует, для метода не важен.
    * @tparam S Тип Backend State, просто компилятор требует, для метода не важен.
    * @return Callback.
    */

  def dispatchOnProxyScopeCB[A, P, S]($: BackendScope[ModelProxy[P], S], msg: A)(implicit ev: ActionType[A]): Callback = {
    $.props >>= { p =>
      p.dispatchCB( msg )
    }
  }


  def eStopPropagationCB(e: ReactEvent): Callback = {
    e.stopPropagationCB
  }

}
