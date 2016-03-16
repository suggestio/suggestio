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


/** Сигнал изменений каких-то данных, задаваемых на карте. */
trait IMapDataChanged extends IFormChanged

/** Сигнал об изменении радиуса на карте. */
case object RadiusChanged extends IMapDataChanged
