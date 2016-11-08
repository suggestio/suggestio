package io.suggest.maps.c

import io.suggest.sjs.common.msg.WarnMsgs
import io.suggest.sjs.common.util.ISjsLogger
import io.suggest.sjs.leaflet.event.{Event, Events, LocationEvent}
import io.suggest.sjs.leaflet.map.LMap
import io.suggest.sjs.leaflet.{Leaflet => L}

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.11.16 21:53
  * Description: Утиль для поддержки L.control.locate, который живёт в виде плагина.
  */
trait LeafletLocateControlUtil
  extends ISjsLogger
{

  /**
    * Активировать плагин locate.control.
    * Повесить реакцию на доступную геолокацию в locate control.
    *
    * @param lmap Инстанс карты.
    * @param onLocFoundF Функция реакции на location found.
    */
  def _onLocateControl[U](lmap: LMap)(onLocFoundF: LocationEvent => U): Unit = {
    if (L.control.locate.nonEmpty) {
      // Активировать плагин locate, т.к. он установлен.
      val lc = L.control.locate().addTo( lmap )

      // js-тип функции тут обязателен, иначе .off3() работать не будет.
      val onLocFoundF_js: js.Function1[LocationEvent, _] = onLocFoundF
      lmap.on3(Events.LOCATION_FOUND, onLocFoundF_js)

      // После определения геолокации юзера нужно останавливать дальнейшую вакханалию с ежесекундным следованием за юзером по карте.
      val startFollowingF = { e: Event =>
        lc._stopFollowing()
        lmap.off3(Events.LOCATION_FOUND, onLocFoundF_js)
      }
      lmap.on3(Events.LOCATION_FOUND, startFollowingF)

    } else {
      warn( WarnMsgs.LEAFLET_LOCATE_CONTROL_MISSING )
    }
  }

}
