package io.suggest.spa

import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import diode.{Circuit, FastEq, ModelR, ModelRO, ModelRW}
import io.suggest.async.IValueCompleter
import io.suggest.sjs.common.model.TimeoutPromise
import io.suggest.sjs.dom2.DomQuick

import scala.concurrent.{Future, Promise}
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.02.19 18:20
  * Description: Статическая утиль для Diode-circuit.
  */
object CircuitUtil {

  // TODO Надо как-то заменить FastEq[T] на FastEq[_ <: T].

  /** Сборка ModelRW на основе родителькой модели и Lens поля.
    *
    * @param modelRW Исходная RW-модель.
    * @param lens Линза от значения до поля.
    * @param feq FastEq под-модели.
    * @tparam Root_t Тип корневой модели circuit.
    * @tparam Parent_t Тип модели внутри root-модели.
    * @tparam Child_t Тип дочерней модели отновительно Parent-модели.
    * @return Инстанс ModelRW Root<=>Child.
    */
  def mkLensZoomRW[Root_t <: AnyRef, Parent_t, Child_t]( modelRW: ModelRW[Root_t, Parent_t], lens: monocle.Lens[Parent_t, Child_t] )
                                                       ( implicit feq: FastEq[_ >: Child_t] ): ModelRW[Root_t, Child_t] = {
    mkLensZoomRwUsing(lens) { modelRW.zoomRW[Child_t](_)(_)(feq) }
  }

  /** Сборка ModelRW для корневой модели, которая недоступна напрямую, на основе lens.
    *
    * @param circuit Текущая цепь.
    * @param lens Линза до нужного поля модели.
    * @param feq FastEq под-модели.
    * @tparam Root_t Тип корневой модели circuit.
    * @tparam Child_t Тип дочерней модели отновительно Root-модели.
    * @return Инстанс ModelRW Root<=>Child.
    */
  def mkLensRootZoomRW[Root_t <: AnyRef, Child_t]( circuit: Circuit[Root_t], lens: monocle.Lens[Root_t, Child_t] )
                                                 ( implicit feq: FastEq[_ >: Child_t] ): ModelRW[Root_t, Child_t] = {
    mkLensZoomRwUsing(lens) { circuit.zoomRW[Child_t](_)(_)(feq) }
  }


  /** Сборка ModelRW на основе ModelRW-функции и lens.
    * Метод нужен, т.к. есть два метода zoomRW с одинаковой сигнатурой: Circuit.zoomRW и ModelRW.zoomRW.
    * Метод можно будет заинлайнить в mkLensZoomRW(), когда private val modelRW перестанет быть private.
    *
    * @param lens Линза.
    * @param mkRwF Фунция, унифицирующая доступ к обоим .zoomRW-методам.
    * @tparam Root_t Тип корневой модели circuit.
    * @tparam Parent_t Тип модели внутри root-модели.
    * @tparam Child_t Тип дочерней модели отновительно Parent-модели.
    * @return Инстанс ModelRW Root<=>Child.
    */
  private def mkLensZoomRwUsing[Root_t <: AnyRef, Parent_t, Child_t]
              (lens: monocle.Lens[Parent_t, Child_t] )
              (mkRwF: (Parent_t => Child_t, (Parent_t, Child_t) => Parent_t) => ModelRW[Root_t, Child_t]): ModelRW[Root_t, Child_t] = {
    mkRwF(
      lens.get,
      (parent0, child2) => (lens set child2)(parent0),
    )
  }


  /** Сборка RO-модели на основе родительской модели и lens.
    *
    * @param modelRO Read-Only-модель Parent=>Child.
    * @param lens Линза.
    * @param feq FastEq Child-модели.
    * @tparam Parent_t Тип модели внутри root-модели.
    * @tparam Child_t Тип дочерней модели отновительно Parent-модели.
    * @return Инстанс ModelRO/ModelR Parent=>Child.
    */
  def mkLensZoomRO[Parent_t, Child_t, M <: ModelRO[Parent_t]](modelRO: M, lens: monocle.Lens[Parent_t, Child_t])
                                                             (implicit feq: FastEq[_ >: Child_t]): modelRO.NewR[Child_t] = {
    modelRO.zoom( lens.get )(feq)
  }

  def mkLensRootZoomRO[Root_t <: AnyRef, Child_t]( circuit: Circuit[Root_t], lens: monocle.Lens[Root_t, Child_t] )
                                                 ( implicit feq: FastEq[_ >: Child_t] ): ModelR[Root_t, Child_t] = {
    circuit.zoom( lens.get )(feq)
  }


  /** To subscribe promise completion to some changing in time value, accessible via circuit zooming. */
  case class promiseSubscribe( doneP: Promise[None.type] = Promise() ) {

    var _timeoutPromiseOpt = Option.empty[TimeoutPromise[None.type]]

    def withTimeout(timeoutMs: Int): this.type = {
      val tpOk = DomQuick.timeoutPromise( timeoutMs )
      doneP.completeWith( tpOk.fut )
      _timeoutPromiseOpt = Some( tpOk )
      this
    }

    def zooming[M <: AnyRef, T: IValueCompleter](circuit: Circuit[M], zoomR: ModelR[M, T]): Future[None.type] = {
      // To monitor readyness of value...
      val readySub = implicitly[IValueCompleter[T]]
      def maybeComplete(v: T) = {
        readySub.maybeCompletePromise( doneP, v )
        // If promise already completed, cancel timeout timer (if defined).
        if (doneP.isCompleted)
          for (tp <- _timeoutPromiseOpt) {
            DomQuick.clearTimeout( tp.timerId )
            _timeoutPromiseOpt = None
          }
      }

      val unsubscribeF = circuit.subscribe( zoomR ) { valueRO =>
        if (!doneP.isCompleted) {
          val v = valueRO.value
          maybeComplete( v )
        }
      }

      maybeComplete( zoomR.value )

      doneP
        .future
        .andThen { case _ => unsubscribeF() }
    }

  }

}
