package io.suggest.spa

import diode.data.{FailedBase, PendingBase, Pot}
import diode._
import io.suggest.common.empty.OptionUtil
import io.suggest.err.ErrorConstants
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import japgolly.univeq._

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.06.18 16:38
  * Description: Без-react утиль для Diode.
  */
object DiodeUtil {

  /** Сборка функции, возвращающей одноразовый результат, который будет сброшен в null. */
  def mkOneShotFunction[T <: AnyRef](retValue: T): () => T = {
    var v = retValue

    {() =>
      ErrorConstants.assertArg( v != null )
      val ret = v
      v = null.asInstanceOf[T]
      ret
    }
  }


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
      def mergeEffects: Option[Effect] = {
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

      def isPendingWithStartTime(startTime: Long): Boolean =
        pendingOpt.exists(_.startTime ==* startTime)

      /** Вообще пустой Pot без намёков на наполнение в ближайшем времени. */
      def isTotallyEmpty: Boolean =
        pot.isEmpty && !pot.isPending

      /** Свёрстка Try[T] поверх Pot[T]. */
      def withTry(tryRes: Try[T]): Pot[T] =
        tryRes.fold( pot.fail, pot.ready )

      /** zero-instance доступ к exception без использования Option.
        * @return null, если нет exception.
        */
      def exceptionOrNull: Throwable = {
        pot match {
          case failed: FailedBase => failed.exception
          case _                  => null
        }
      }

    }


    /** Расширенное API для ActionHandler'ов. */
    implicit class ActionHandlerExt[M, T](val ah: ActionHandler[M, T]) extends AnyVal {

      def updateMaybeSilent(silent: Boolean)(v2: T): ActionResult[M] = {
        if (silent) ah.updatedSilent(v2)
        else ah.updated(v2)
      }

      def updateMaybeSilentFx(silent: Boolean)(v2: T, fx: Effect): ActionResult[M] = {
        if (silent) ah.updatedSilent(v2, fx)
        else ah.updated(v2, fx)
      }

      def updatedFrom( actRes: ActionResult[T]): ActionResult[M] =
        optionalResult( actRes.newModelOpt, actRes.effectOpt )
      def updatedFrom( actResOpt: Option[ActionResult[T]]): ActionResult[M] =
        actResOpt.fold( ah.noChange )( updatedFrom )

      def optionalResult(v2Opt: Option[T] = None, fxOpt: Option[Effect] = None, silent: Boolean = false): ActionResult[M] = {
        (v2Opt, fxOpt) match {
          case (Some(v2), Some(fx)) => if (silent) ah.updatedSilent(v2, fx) else ah.updated(v2, fx)
          case (Some(v2), None)     => if (silent) ah.updatedSilent(v2) else ah.updated(v2)
          case (None, Some(fx))     => ah.effectOnly(fx)
          case (None, None)         => ah.noChange
        }
      }

      def maybeUpdated(v2Opt: Option[T]): ActionResult[M] =
        v2Opt.fold( ah.noChange )( ah.updated(_) )


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
