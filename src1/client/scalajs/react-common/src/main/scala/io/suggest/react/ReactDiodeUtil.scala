package io.suggest.react

import diode._
import diode.data.{PendingBase, Pot}
import diode.react.ModelProxy
import io.suggest.common.empty.OptionUtil
import japgolly.scalajs.react.{BackendScope, Callback}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import japgolly.univeq._

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


  /** Расширенное API для коллекций эффектов.
    *
    * @param effects Эффекты.
    */
  implicit class EffectsOps(val effects: TraversableOnce[Effect]) extends AnyVal {

    /** Объединение списка эффектов воедино для параллельного запуска всех сразу.
      *
      * @return None, если передан пустой список эффектов.
      *         Some(fx) с объединённым, либо единственным, эффектом.
      */
    def mergeEffectsSet: Option[Effect] = {
      OptionUtil.maybe(effects.nonEmpty) {
        val iter = effects.toIterator
        val fx1 = iter.next()
        if (iter.hasNext) {
          new EffectSet(fx1, iter.toSet, defaultExecCtx)
        } else {
          fx1
        }
      }
    }

  }


  /** Расширенная утиль для Pot'ов. */
  implicit class PotOpsExt[T](val pot: Pot[T]) extends AnyVal {

    def pendingOpt: Option[PendingBase] = {
      pot match {
        case pb: PendingBase => Some(pb)
        case _ => None
      }
    }

    def isPendingWithStartTime(startTime: Long): Boolean = {
      pendingOpt.exists(_.startTime ==* startTime)
    }

    /** Вообще пустой Pot без намёков на наполнение в ближайшем времени. */
    def isTotallyEmpty: Boolean = {
      pot.isEmpty && !pot.isPending
    }

  }


  /** Расширенное API для ActionHandler'ов. */
  implicit class ActionHandlerExt[M, T](val ah: ActionHandler[M, T]) extends AnyVal {

    def updateMaybeSilent(silent: Boolean)(v2: T): ActionResult[M] = {
      if (silent)
        ah.updatedSilent(v2)
      else
        ah.updated(v2)
    }

    def updateMaybeSilentFx(silent: Boolean)(v2: T, fx: Effect): ActionResult[M] = {
      if (silent)
        ah.updatedSilent(v2, fx)
      else
        ah.updated(v2, fx)
    }

    def optionalResult(v2Opt: Option[T] = None, fxOpt: Option[Effect] = None): ActionResult[M] = {
      (v2Opt, fxOpt) match {
        case (Some(v2), Some(fx)) => ah.updated(v2, fx)
        case (Some(v2), None)     => ah.updated(v2)
        case (None, Some(fx))     => ah.effectOnly(fx)
        case (None, None)         => ah.noChange
      }
    }

    def optionalSilentResult(v2Opt: Option[T] = None, fxOpt: Option[Effect] = None): ActionResult[M] = {
      (v2Opt, fxOpt) match {
        case (Some(v2), Some(fx)) => ah.updatedSilent(v2, fx)
        case (Some(v2), None)     => ah.updatedSilent(v2)
        case (None, Some(fx))     => ah.effectOnly(fx)
        case (None, None)         => ah.noChange
      }
    }


    def updatedMaybeEffect(v2: T, effectOpt: Option[Effect]): ActionResult[M] = {
      effectOpt.fold( ah.updated(v2) ) { fx => ah.updated(v2, fx) }
    }

    def maybeEffectOnly(effectOpt: Option[Effect]): ActionResult[M] = {
      effectOpt.fold( ah.noChange ) { ah.effectOnly }
    }

  }

}

