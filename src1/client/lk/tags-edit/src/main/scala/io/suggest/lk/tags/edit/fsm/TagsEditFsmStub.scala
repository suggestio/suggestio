package io.suggest.lk.tags.edit.fsm

import io.suggest.fsm.StateData
import io.suggest.lk.tags.edit.m.MStateData
import io.suggest.sjs.common.fsm.SjsFsm

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.09.15 16:46
 * Description: Базовый трейт для разработки трейтов-кусков реализации [[TagsEditFsm]].
 */
trait TagsEditFsmStub extends SjsFsm with StateData {

  // Фиксация типов
  override type State_t   = FsmState
  override type SD        = MStateData

  /** Когда что-то изменяется в наборе тегов, необходимо передать управление сюда. */
  protected def _tagsChanged(): Unit = {}

}

