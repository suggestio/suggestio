package org.js.react.leaflet.core

import io.suggest.sjs.leaflet.control.{Control, ControlOptions}
import io.suggest.sjs.leaflet.layer.group.FeatureGroup
import io.suggest.sjs.leaflet.map.Layer
import io.suggest.sjs.leaflet.path.Path
import japgolly.scalajs.react.raw

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.01.2021 18:48
  */

//trait LayerWithChildrenProps extends LayerProps with PropsWithChildren

//trait PathWithChildrenProps extends PathProps with PropsWithChildren


@js.native
@JSImport(PACKAGE_NAME, "createControlComponent")
object createControlComponent extends js.Function {
  def apply[E <: Control, P <: ControlOptions]
           (createInstance: js.Function1[P, E])
           : raw.React.ForwardRefComponent[P, E]
           = js.native
}


@js.native
@JSImport(PACKAGE_NAME, "createLayerComponent")
object createLayerComponent extends js.Function {
  def apply[E <: Layer, P <: LayerProps /*LayerWithChildrenProps*/]
           (createElement: js.Function2[P, LeafletContextInterface, LeafletElement[E, js.Any]],
            updateElement: js.Function3[E, P, P, Unit] = js.native)
           : raw.React.ForwardRefComponent[P, E]
           = js.native
}


@js.native
@JSImport(PACKAGE_NAME, "createOverlayComponent")
object createOverlayComponent extends js.Function {
  def apply[E <: DivOverlay, P <: LayerProps /*LayerWithChildrenProps*/]
           (createElement: js.Function2[P, LeafletContextInterface, LeafletElement[E, js.Any]],
            useLifecycle: DivOverlayLifecycleHook[E, P])
           : raw.React.ForwardRefComponent[P, E]
           = js.native
}


@js.native
@JSImport(PACKAGE_NAME, "createPathComponent")
object createPathComponent extends js.Function {
  def apply[E <: FeatureGroup | Path, P <: PathProps /*PathWithChildrenProps*/]
           (createElement: js.Function2[P, LeafletContextInterface, LeafletElement[E, js.Any]],
            updateElement: js.Function3[E, P, P, Unit])
           : raw.React.ForwardRefComponent[P, E]
           = js.native
}


@js.native
@JSImport(PACKAGE_NAME, "createTileLayerComponent")
object createTileLayerComponent extends js.Function {
  def apply[E <: Layer, P <: LayerProps]
           (createElement: js.Function2[P, LeafletContextInterface, LeafletElement[E, js.Any]],
            updateElement: js.Function3[E, P, P, Unit])
           : raw.React.ForwardRefComponent[P, E]
           = js.native
}
