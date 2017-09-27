package io.suggest.react

import diode.{ActionType, Effect, EffectSet}
import diode.data.{PendingBase, Pot}
import diode.react.ModelProxy
import japgolly.scalajs.react.{BackendScope, Callback}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

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
    $.props >>= { p =>
      p.dispatchCB( f(p) )
    }
  }


  def isPendingWithStartTime[U](pot: Pot[U], validStartTime: Long): Boolean = {
    pot match {
      case pb: PendingBase => pb.startTime == validStartTime
      case _ => false
    }
  }


  /** Объединение списка эффектов воедино для параллельного запуска всех сразу.
    *
    * @param effects Эффекты.
    * @return None, если передан пустой список эффектов.
    *         Some(fx) с объединённым, либо единственным, эффектом.
    */
  def mergeEffectsSet(effects: TraversableOnce[Effect]): Option[Effect] = {
    if (effects.isEmpty) {
      None
    } else {
      val iter = effects.toIterator
      val fx1 = iter.next()
      val allFx = if (iter.hasNext) {
        new EffectSet(fx1, iter.toSet, defaultExecCtx)
      } else {
        fx1
      }
      Some(allFx)
    }
  }

}
