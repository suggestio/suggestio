package io.suggest.sc.sjs.m.msc.fsm

import io.suggest.sc.sjs.m.msc.fsm.state._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.05.15 17:36
 * Description: Одно состояние выдачи в целом. На стороне сервера есть модель MScJsState,
 * для сериализации/десериализации данных этого состояния.
 *
 * Модель строиться на аддонах, реализующих [[IScState]], каждый аддон добавляет какое-то поле в модель.
 */
trait MScStateT extends RcvrAdnIdT with CatT with SearchPanelOpened {

  override type T = MScStateT

}


/** Интерфейс для сборки stackable-аддонов модели. */
trait IScState {

  /** Сборка типа результирующего состояния с участием аддонов. */
  type T <: IScState

   /**
   * Накатить разницу между этим и другим состоянием на реальное состояние выдачи.
   * Текущее состояние считается более новым.
   * @param oldState Предыдущее состояние.
   */
  def applyChangesSince(oldState: T): Unit = {}

}

/** Дефолтовая реализация [[MScStateT]]. */
case class MScState(
  override val rcvrAdnId          : Option[String]      = None,
  override val cat                : Option[MCatMeta]    = None,
  override val searchPanelOpened  : Boolean             = false
)
  extends MScStateT

