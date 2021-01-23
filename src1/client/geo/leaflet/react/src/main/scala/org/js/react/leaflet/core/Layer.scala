package org.js.react.leaflet.core

import io.suggest.sjs.leaflet.layer.{InteractiveLayerOptions, LayerOptions}
import io.suggest.sjs.leaflet.map.Layer

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.01.2021 23:58
  */
trait LayerProps
  extends EventedProps
  with LayerOptions


trait InteractiveLayerProps
  extends LayerProps
  with InteractiveLayerOptions


@js.native
@JSImport(PACKAGE_NAME, "useLayerLifecycle")
object useLayerLifecycle extends js.Function2[LeafletElement[Layer, js.Any], LeafletContextInterface, Unit] {
  override def apply(element: LeafletElement[Layer, js.Any],
                     context: LeafletContextInterface,
                    ): Unit = js.native
}


@js.native
@JSImport(PACKAGE_NAME, "createLayerHook")
object createLayerHook extends js.Function {
  def apply[E <: Layer, P <: LayerProps]
           (useElement: ElementHook[E, P])
           : js.Function1[P, ElementHookRef[E, js.Any]] = js.native
}
