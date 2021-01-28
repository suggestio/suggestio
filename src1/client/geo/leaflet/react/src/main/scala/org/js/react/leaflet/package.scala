package org.js.react

import io.suggest.sjs.leaflet.{LatLngBoundsExpression, LatLngExpression, PolygonLatLngs_t}
import io.suggest.sjs.leaflet.control.attribution.{Attribution, AttributionOptions}
import io.suggest.sjs.leaflet.control.scale.{Scale, ScaleOptions}
import io.suggest.sjs.leaflet.control.zoom.{Zoom, ZoomOptions}
import io.suggest.sjs.leaflet.event.LeafletEventHandlerFnMap
import io.suggest.sjs.leaflet.geojson.{GeoJson, GeoJsonOptions}
import io.suggest.sjs.leaflet.layer.LayerOptions
import io.suggest.sjs.leaflet.layer.group.{FeatureGroup, LayerGroup}
import io.suggest.sjs.leaflet.layer.tile.{TileLayer, TileLayerOptions}
import io.suggest.sjs.leaflet.map.LMap
import io.suggest.sjs.leaflet.marker.{Marker, MarkerOptions}
import io.suggest.sjs.leaflet.overlay.{ImageOverlay, SvgOverlay, VideoOverlay, VideoOverlayOptions}
import io.suggest.sjs.leaflet.path.PathOptions
import io.suggest.sjs.leaflet.path.circle.{Circle, CircleMarker}
import io.suggest.sjs.leaflet.path.poly.{Polygon, Polyline, PolylineOptions, Rectangle}
import io.suggest.sjs.leaflet.popup.{Popup, PopupOptions}
import io.suggest.sjs.leaflet.tooltip.{Tooltip, TooltipOptions}
import japgolly.scalajs.react.vdom.VdomNode
import japgolly.scalajs.react.{Children, JsFnComponent, JsForwardRefComponent, raw}
import org.js.react.leaflet.core.{CircleMarkerProps, ElementHookRef, EventedProps, LayerProps, MediaOverlayProps, PathProps}
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.01.2021 18:31
  */
package object leaflet {

  final val REACT_LEAFLET_PACKAGE = "react-leaflet"

  type AttributionControlProps = AttributionOptions

  object AttributionControl {
    @js.native
    @JSImport(REACT_LEAFLET_PACKAGE, "AttributionControl")
    val componentRaw : raw.React.ForwardRefComponent[AttributionControlProps, Attribution] = js.native
    val component = JsForwardRefComponent[AttributionControlProps, Children.None, Attribution]( componentRaw )
    def apply(props: AttributionControlProps = new AttributionControlProps {}) =
      component( props )
  }

  type CircleProps = CircleMarkerProps

  object Circle {
    @js.native
    @JSImport(REACT_LEAFLET_PACKAGE, "Circle")
    val componentRaw: raw.React.ForwardRefComponent[CircleProps, Circle] = js.native
    val component = JsForwardRefComponent[CircleProps, Children.Varargs, Circle]( componentRaw )
  }


  object CircleMarker {
    @js.native
    @JSImport(REACT_LEAFLET_PACKAGE, "CircleMarker")
    val componentRaw: raw.React.ForwardRefComponent[CircleMarkerProps, CircleMarker] = js.native
    val component = JsForwardRefComponent[CircleMarkerProps, Children.Varargs, CircleMarker]( componentRaw )
  }


  type ZoomControlProps = ZoomOptions
  object ZoomControl {
    @js.native
    @JSImport(REACT_LEAFLET_PACKAGE, "ZoomControl")
    val componentRaw: raw.React.ForwardRefComponent[ZoomControlProps, Zoom] = js.native
    val component = JsForwardRefComponent[ZoomControlProps, Children.None, Zoom]( componentRaw )
    def apply( props: ZoomControlProps = new ZoomControlProps {} ) =
      component( props )
  }


  trait LayerGroupProps extends LayerOptions with EventedProps
  object LayerGroup {
    @js.native
    @JSImport(REACT_LEAFLET_PACKAGE, "LayerGroup")
    val componentRaw: raw.React.ForwardRefComponent[LayerGroupProps, LayerGroup] = js.native
    val component = JsForwardRefComponent[LayerGroupProps, Children.Varargs, LayerGroup]( componentRaw )
    def apply(props: LayerGroupProps = new LayerGroupProps {})(children: VdomNode*) =
      component( props )( children: _* )
  }


  trait FeatureGroupProps extends LayerGroupProps with PathProps
  object FeatureGroup {
    @js.native
    @JSImport(REACT_LEAFLET_PACKAGE, "FeatureGroup")
    val componentRaw: raw.React.ForwardRefComponent[FeatureGroupProps, FeatureGroup] = js.native
    val component = JsForwardRefComponent[FeatureGroupProps, Children.Varargs, FeatureGroupProps]( componentRaw )
    def apply(props: FeatureGroupProps = new FeatureGroupProps {})(children: VdomNode*) =
      component( props )(children: _*)
  }


  trait GeoJsonProps extends GeoJsonOptions with LayerGroupProps with PathProps {
    val data: js.Object
  }
  object GeoJson {
    @js.native
    @JSImport(REACT_LEAFLET_PACKAGE, "GeoJSON")
    val componentRaw: raw.React.ForwardRefComponent[GeoJsonProps, GeoJson] = js.native
    val component = JsForwardRefComponent[GeoJsonProps, Children.Varargs, GeoJson]( componentRaw )
  }


  trait ImageOverlayProps extends MediaOverlayProps {
    val url: String
  }
  object ImageOverlay {
    @js.native
    @JSImport(REACT_LEAFLET_PACKAGE, "ImageOverlay")
    val componentRaw: raw.React.ForwardRefComponent[ImageOverlayProps, ImageOverlay] = js.native
    val component = JsForwardRefComponent[ImageOverlayProps, Children.Varargs, ImageOverlay]( componentRaw )
  }


  trait MarkerProps extends MarkerOptions with EventedProps {
    val position: LatLngExpression
  }
  object Marker {
    @js.native
    @JSImport(REACT_LEAFLET_PACKAGE, "Marker")
    val componentRaw: raw.React.ForwardRefComponent[MarkerProps, Marker] = js.native
    val component = JsForwardRefComponent[MarkerProps, Children.Varargs, Marker]( componentRaw )
  }


  trait PaneProps extends js.Object {
    val className: js.UndefOr[String] = js.undefined
    val name: String
    val pane: js.UndefOr[String] = js.undefined
    val style: js.UndefOr[js.Object] = js.undefined  //CSSProperties
  }
  object Pane {
    @js.native
    @JSImport(REACT_LEAFLET_PACKAGE, "Pane")
    val componentRaw: raw.React.StatelessFunctionalComponent[PaneProps] = js.native
    val component = JsFnComponent[PaneProps, Children.Varargs]( componentRaw )
  }


  trait PolygonProps extends PolylineOptions with PathProps {
    val positions: PolygonLatLngs_t
  }
  object Polygon {
    @js.native
    @JSImport(REACT_LEAFLET_PACKAGE, "Polygon")
    val componentRaw: raw.React.ForwardRefComponent[PolygonProps, Polygon] = js.native
    val component = JsForwardRefComponent[PolygonProps, Children.Varargs, Polygon]( componentRaw )
  }


  trait PolylineProps extends PolylineOptions with PathProps {
    val positions: js.Array[LatLngExpression] | js.Array[js.Array[LatLngExpression]]
  }
  object Polyline {
    @js.native
    @JSImport(REACT_LEAFLET_PACKAGE, "Polyline")
    val componentRaw: raw.React.ForwardRefComponent[PolylineProps, Polyline] = js.native
    val component = JsForwardRefComponent[PolylineProps, Children.Varargs, Polyline]( componentRaw )
  }


  trait PopupProps extends PopupOptions with EventedProps {
    val onClose: js.UndefOr[js.Function0[Unit]] = js.undefined
    val onOpen: js.UndefOr[js.Function0[Unit]] = js.undefined
    val position: js.UndefOr[LatLngExpression] = js.undefined
  }
  object Popup {
    @js.native
    @JSImport(REACT_LEAFLET_PACKAGE, "Popup")
    val componentRaw: raw.React.ForwardRefComponent[PopupProps, Popup] = js.native
    val component = JsForwardRefComponent[PopupProps, Children.Varargs, Popup]( componentRaw )
    def apply(props: PopupProps = new PopupProps {})(children: VdomNode*) =
      component(props)( children: _* )
  }


  trait RectangleProps extends PathOptions with PathProps {
    val bounds: LatLngBoundsExpression
  }
  object Rectangle {
    @js.native
    @JSImport(REACT_LEAFLET_PACKAGE, "Rectangle")
    val componentRaw: raw.React.ForwardRefComponent[RectangleProps, Rectangle] = js.native
    val component = JsForwardRefComponent[RectangleProps, Children.Varargs, Rectangle]( componentRaw )
  }


  trait SVGOverlayProps extends MediaOverlayProps {
    val attributes: js.UndefOr[js.Dictionary[String]] = js.undefined
  }
  object SvgOverlay {
    @js.native
    @JSImport(REACT_LEAFLET_PACKAGE, "useSVGOverlayElement")
    val useSVGOverlayElement: ElementHookRef[SvgOverlay, dom.svg.SVG] = js.native

    @js.native
    @JSImport(REACT_LEAFLET_PACKAGE, "useSVGOverlay")
    val useSVGOverlay: js.Function1[SVGOverlayProps, ElementHookRef[SvgOverlay, dom.svg.SVG]] = js.native

    @js.native
    @JSImport(REACT_LEAFLET_PACKAGE, "SvgOverlay")
    val componentRaw: raw.React.ForwardRefComponent[SVGOverlayProps, SvgOverlay] = js.native

    val component = JsForwardRefComponent[SVGOverlayProps, Children.Varargs, SvgOverlay]( componentRaw )
  }


  type ScaleControlProps = ScaleOptions
  object ScaleControl {
    @js.native
    @JSImport(REACT_LEAFLET_PACKAGE, "ScaleControl")
    val componentRaw: raw.React.ForwardRefComponent[ScaleControlProps, Scale] = js.native
    val component = JsForwardRefComponent[ScaleControlProps, Children.None, Scale]( componentRaw )
    def apply( props: ScaleControlProps = new ScaleControlProps{} ) =
      component(props)
  }


  trait TileLayerProps extends TileLayerOptions with LayerProps {
    val url: String
  }
  object TileLayer {
    @js.native
    @JSImport(REACT_LEAFLET_PACKAGE, "TileLayer")
    val componentRaw: raw.React.ForwardRefComponent[TileLayerProps, TileLayer] = js.native
    val component = JsForwardRefComponent[TileLayerProps, Children.None, TileLayer]( componentRaw )
  }


  trait TooltipProps extends TooltipOptions with EventedProps {
    val onClose: js.UndefOr[js.Function0[Unit]] = js.undefined
    val onOpen: js.UndefOr[js.Function0[Unit]] = js.undefined
    val position: js.UndefOr[LatLngExpression] = js.undefined
  }
  object Tooltip {
    @js.native
    @JSImport(REACT_LEAFLET_PACKAGE, "Tooltip")
    val componentRaw: raw.React.ForwardRefComponent[TooltipProps, Tooltip] = js.native
    val component = JsForwardRefComponent[TooltipProps, Children.Varargs, Tooltip]( componentRaw )
  }


  trait VideoOverlayProps extends MediaOverlayProps with VideoOverlayOptions {
    val play: js.UndefOr[Boolean] = js.undefined
    val url: String | js.Array[String] | dom.html.Video
  }
  object VideoOverlay {
    @js.native
    @JSImport(REACT_LEAFLET_PACKAGE, "VideoOverlay")
    val componentRaw: raw.React.ForwardRefComponent[VideoOverlayProps, VideoOverlay] = js.native
    val component = JsForwardRefComponent[VideoOverlayProps, Children.Varargs, VideoOverlay]( componentRaw )
  }

  // TODO LayersControl.tsx
  // TODO WMSTileLayer.tsx


  // hooks.ts:

  @js.native
  @JSImport(REACT_LEAFLET_PACKAGE, "useMap")
  def useMap(): LMap = js.native


  @js.native
  @JSImport(REACT_LEAFLET_PACKAGE, "useMapEvent")
  def useMapEvent[E <: js.Any](eventType: String, handler: js.Function1[E, Unit]): LMap = js.native


  @js.native
  @JSImport(REACT_LEAFLET_PACKAGE, "useMapEvents")
  def useMapEvents(handlers: LeafletEventHandlerFnMap): LMap = js.native

}
