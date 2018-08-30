package io.suggest.maps.r.userloc

import diode.react.ModelProxy
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.geo.MGeoLoc
import io.suggest.maps.u.MapIcons

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.08.18 11:00
  * Description: UserLocShape - компонент для рендера локации юзера на карте.
  * Вид такой же как и в LocateControl: accuracy-круг с точкой в центре.
  */
object LocShapeR {

  val component = ScalaComponent
    .builder[ModelProxy[Option[MGeoLoc]]](getClass.getSimpleName)
    .stateless
    .render_P { userLocOptProxy =>
      userLocOptProxy.value.whenDefinedEl { userLoc =>
        MapIcons.userLocCircle( userLoc )
      }
    }
    .build

}
