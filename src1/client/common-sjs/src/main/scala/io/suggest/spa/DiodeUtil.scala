package io.suggest.spa

import diode.data.{PendingBase, Pot}
import diode.{ActionHandler, ActionResult, Effect, EffectSet}
import io.suggest.common.empty.OptionUtil
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.06.18 16:38
  * Description: Без-react утиль для Diode.
  */
object DiodeUtil {

  /** Не очень явные дополнения к API живут тут. */
  object Implicits {

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


      def updatedSilentMaybeEffect(v2: T, effectOpt: Option[Effect]): ActionResult[M] = {
        effectOpt.fold( ah.updatedSilent(v2) ) { fx => ah.updatedSilent(v2, fx) }
      }

    }

  }


}
