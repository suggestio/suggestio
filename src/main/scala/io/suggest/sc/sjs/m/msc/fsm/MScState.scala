package io.suggest.sc.sjs.m.msc.fsm

import io.suggest.sc.sjs.m.msc.fsm.state._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.05.15 17:36
 * Description: Декларативное состояние выдачи в целом. На стороне сервера есть модель MScJsState,
 * для сериализации/десериализации данных этого состояния.
 *
 * Модель строиться на аддонах, реализующих [[IScState]], каждый аддон добавляет какое-то поле (поля) в модель.
 * Здесь только высокоуровневые данные по состоянию. Всякие внутренние состояния конткретных fsm выдачи здесь НЕ живту.
 */
@deprecated("FSM-MVM", "2015.aug.11")
trait MScStateT extends RcvrAdnIdT with CatT with SearchPanelOpened with NavPanelOpened with CurrGnl with FtsSearch
with FocusedT {

  override type T = MScStateT

}


/** Интерфейс для сборки stackable-аддонов модели. */
@deprecated("FSM-MVM", "2015.aug.11")
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
@deprecated("FSM-MVM", "2015.aug.11")
case class MScState(
  override val rcvrAdnId          : Option[String]          = None,
  override val cat                : Option[MCatMeta]        = None,
  override val searchPanelOpened  : Boolean                 = false,
  override val navPanelOpened     : Boolean                 = false,
  override val currGnlIndex       : Option[Int]             = None,
  override val ftsSearch          : Option[String]          = None,
  override val focOffset          : Option[Int]             = None
)
  extends MScStateT

