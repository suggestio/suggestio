package io.suggest.lk.adv.geo.tags.m.signal

import io.suggest.lk.adv.m.IAdvFormChanged
import io.suggest.maps.rad.m.signal.IRadMapChanged
import io.suggest.sjs.common.fsm.IFsmEventMsgCompanion
import org.scalajs.dom.Event

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.03.16 12:47
  * Description: Сигнал о каких-то изменениях в форме.
  */
trait IAgtFormChanged extends IAdvFormChanged

/** Сигнал изменения в наборе тегов. */
case object TagsChanged extends IAgtFormChanged


/** Сигнал изменений каких-то данных, задаваемых на карте. */
trait IMapDataChanged extends IAgtFormChanged with IRadMapChanged

/** Сигнал об изменении радиуса на карте. */
case object RadiusChanged extends IMapDataChanged


case class OnMainScreenChanged(event: Event) extends IAgtFormChanged
object OnMainScreenChanged extends IFsmEventMsgCompanion
