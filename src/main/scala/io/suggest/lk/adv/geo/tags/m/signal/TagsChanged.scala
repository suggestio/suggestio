package io.suggest.lk.adv.geo.tags.m.signal

import io.suggest.sjs.common.fsm.IFsmMsg

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.03.16 12:47
  * Description: Сигнал о каких-то изменениях в форме.
  */
trait IFormChanged extends IFsmMsg

/** Сигнал изменения в наборе тегов. */
case object TagsChanged extends IFormChanged
