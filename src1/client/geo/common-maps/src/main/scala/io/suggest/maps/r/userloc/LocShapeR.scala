package io.suggest.maps.r.userloc

import diode.react.ModelProxy
import japgolly.scalajs.react.ScalaComponent
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.geo.MGeoLoc
import io.suggest.maps.u.MapIcons
import io.suggest.react.ReactDiodeUtil
import io.suggest.spa.OptFastEq

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
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .render_P { userLocOptProxy =>
      userLocOptProxy.value.whenDefinedEl { userLoc =>
        MapIcons.userLocCircle( userLoc )
      }
    }
    .configure(
      ReactDiodeUtil.statePropsValShouldComponentUpdate(
        OptFastEq.Wrapped( MGeoLoc.GeoLocNearbyFastEq )
      )
    )
    .build

}
