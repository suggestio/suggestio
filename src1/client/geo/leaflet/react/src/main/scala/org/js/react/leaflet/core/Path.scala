package org.js.react.leaflet.core

import io.suggest.sjs.leaflet.layer.group.FeatureGroup
import io.suggest.sjs.leaflet.path.{Path, PathOptions}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.01.2021 15:07
  */
trait PathProps extends InteractiveLayerProps {
  val pathOptions: js.UndefOr[PathOptions] = js.undefined
}


@js.native
@JSImport(PACKAGE_NAME, "usePathOptions")
object usePathOptions extends js.Function2[LeafletElement[FeatureGroup | Path, js.Any], PathProps, Unit] {
  override def apply(
                      element: LeafletElement[FeatureGroup | Path, js.Any],
                      props: PathProps,
                    ): Unit = js.native
}


@js.native
@JSImport(PACKAGE_NAME, "createPathHook")
object createPathHook extends js.Function {
  def apply[E <: FeatureGroup | Path, P <: PathProps]
           (useElement: ElementHook[E, P])
           : js.Function1[P, ElementHookRef[E, js.Any]]
           = js.native
}
