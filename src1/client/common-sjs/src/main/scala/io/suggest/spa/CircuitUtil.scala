package io.suggest.spa

import diode.{Circuit, FastEq, ModelRO, ModelRW}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.02.19 18:20
  * Description: Статическая утиль для Diode-circuit.
  */
object CircuitUtil {

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
                                                       ( implicit feq: FastEq[Child_t] ): ModelRW[Root_t, Child_t] = {
    mkLensZoomRwUsing(lens) { modelRW.zoomRW[Child_t](_)(_)(_) }
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
                                                 ( implicit feq: FastEq[Child_t] ): ModelRW[Root_t, Child_t] = {
    mkLensZoomRwUsing(lens) { circuit.zoomRW[Child_t](_)(_)(_) }
  }


  /** Сборка ModelRW на основе ModelRW-функции и lens.
    * Метод нужен, т.к. есть два метода zoomRW с одинаковой сигнатурой: Circuit.zoomRW и ModelRW.zoomRW.
    * Метод можно будет заинлайнить в mkLensZoomRW(), когда private val modelRW перестанет быть private.
    *
    * @param lens Линза.
    * @param mkRwF Фунция, унифицирующая доступ к обоим .zoomRW-методам.
    * @param feq FastEq Child-модели.
    * @tparam Root_t Тип корневой модели circuit.
    * @tparam Parent_t Тип модели внутри root-модели.
    * @tparam Child_t Тип дочерней модели отновительно Parent-модели.
    * @return Инстанс ModelRW Root<=>Child.
    */
  private def mkLensZoomRwUsing[Root_t <: AnyRef, Parent_t, Child_t]
  (lens: monocle.Lens[Parent_t, Child_t] )
  ( mkRwF: (Parent_t => Child_t, (Parent_t, Child_t) => Parent_t, FastEq[_ >: Child_t]) => ModelRW[Root_t, Child_t] )
  ( implicit feq: FastEq[Child_t] ): ModelRW[Root_t, Child_t] = {
    mkRwF(
      lens.get,
      (parent0, child2) => lens.set(child2)(parent0),
      feq
    )
  }


  /** Сборка RO-модели на основе родительской модели и lens.
    *
    * @param modelRO Read-Only-модель Parent=>Child.
    * @param lens Линза.
    * @param feq FastEq Child-модели.
    * @tparam Root_t Тип корневой модели circuit.
    * @tparam Parent_t Тип модели внутри root-модели.
    * @tparam Child_t Тип дочерней модели отновительно Parent-модели.
    * @return Инстанс ModelRO Parent=>Child.
    */
  def mkLensZoomRO[Parent_t, Child_t](modelRO: ModelRO[Parent_t], lens: monocle.Lens[Parent_t, Child_t] )
                                     (implicit feq: FastEq[_ >: Child_t]): ModelRO[Child_t] = {
    modelRO.zoom( lens.get )(feq)
  }

}
