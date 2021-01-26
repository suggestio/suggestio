package org.js.react

import io.suggest.sjs.leaflet.{LatLngBoundsExpression, LatLngExpression}
import io.suggest.sjs.leaflet.control.attribution.{Attribution, AttributionOptions}
import io.suggest.sjs.leaflet.control.scale.{Scale, ScaleOptions}
import io.suggest.sjs.leaflet.control.zoom.{Zoom, ZoomOptions}
import io.suggest.sjs.leaflet.geojson.{GeoJson, GeoJsonOptions}
import io.suggest.sjs.leaflet.layer.LayerOptions
import io.suggest.sjs.leaflet.layer.group.{FeatureGroup, LayerGroup}
import io.suggest.sjs.leaflet.layer.tile.{TileLayer, TileLayerOptions}
import io.suggest.sjs.leaflet.marker.{Marker, MarkerOptions}
import io.suggest.sjs.leaflet.overlay.{ImageOverlay, SvgOverlay, VideoOverlay, VideoOverlayOptions}
import io.suggest.sjs.leaflet.path.PathOptions
import io.suggest.sjs.leaflet.path.circle.{Circle, CircleMarker}
import io.suggest.sjs.leaflet.path.poly.{Polygon, Polyline, PolylineOptions, Rectangle}
import io.suggest.sjs.leaflet.popup.{Popup, PopupOptions}
import io.suggest.sjs.leaflet.tooltip.{Tooltip, TooltipOptions}
import japgolly.scalajs.react.raw
import org.js.react.leaflet.core.{CircleMarkerProps, ElementHookRef, EventedProps, LayerProps, MediaOverlayProps, PathProps, PropsWithChildren}
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

  @js.native
  @JSImport(REACT_LEAFLET_PACKAGE, "AttributionControl")
  val AttributionControl: raw.React.ForwardRefComponent[AttributionControlProps, Attribution] = js.native

  type CircleProps = CircleMarkerProps

  @js.native
  @JSImport(REACT_LEAFLET_PACKAGE, "Circle")
  val Circle: raw.React.ForwardRefComponent[CircleProps, Circle] = js.native

  @js.native
  @JSImport(REACT_LEAFLET_PACKAGE, "CircleMarker")
  val CircleMarker: raw.React.ForwardRefComponent[CircleMarkerProps, CircleMarker] = js.native


  type ZoomControlProps = ZoomOptions
  @js.native
  @JSImport(REACT_LEAFLET_PACKAGE, "ZoomControl")
  val ZoomControl: raw.React.ForwardRefComponent[ZoomControlProps, Zoom] = js.native


  trait LayerGroupProps extends LayerOptions with EventedProps with PropsWithChildren
  @js.native
  @JSImport(REACT_LEAFLET_PACKAGE, "LayerGroup")
  val LayerGroup: raw.React.ForwardRefComponent[LayerGroupProps, LayerGroup] = js.native


  trait FeatureGroupProps extends LayerGroupProps with PathProps
  @js.native
  @JSImport(REACT_LEAFLET_PACKAGE, "FeatureGroup")
  val FeatureGroup: raw.React.ForwardRefComponent[FeatureGroupProps, FeatureGroup] = js.native


  trait GeoJsonProps extends GeoJsonOptions with LayerGroupProps with PathProps {
    val data: js.Object
  }
  @js.native
  @JSImport(REACT_LEAFLET_PACKAGE, "GeoJSON")
  val GeoJSON: raw.React.ForwardRefComponent[GeoJsonProps, GeoJson] = js.native


  trait ImageOverlayProps extends MediaOverlayProps with PropsWithChildren {
    val url: String
  }
  @js.native
  @JSImport(REACT_LEAFLET_PACKAGE, "ImageOverlay")
  val ImageOverlay: raw.React.ForwardRefComponent[ImageOverlayProps, ImageOverlay] = js.native


  trait MarkerProps extends MarkerOptions with EventedProps with PropsWithChildren {
    val position: LatLngExpression
  }
  @js.native
  @JSImport(REACT_LEAFLET_PACKAGE, "Marker")
  val Marker: raw.React.ForwardRefComponent[MarkerProps, Marker] = js.native


  trait PaneProps extends PropsWithChildren {
    val className: js.UndefOr[String] = js.undefined
    val name: String
    val pane: js.UndefOr[String] = js.undefined
    val style: js.UndefOr[js.Object] = js.undefined  //CSSProperties
  }
  @js.native
  @JSImport(REACT_LEAFLET_PACKAGE, "Pane")
  def Pane(props: PaneProps): raw.React.Node = js.native


  trait PolygonProps extends PolylineOptions with PathProps with PropsWithChildren {
    val positions: js.Array[LatLngExpression] | js.Array[js.Array[LatLngExpression]] | js.Array[js.Array[js.Array[LatLngExpression]]]
  }
  @js.native
  @JSImport(REACT_LEAFLET_PACKAGE, "Polygon")
  val Polygon: raw.React.ForwardRefComponent[PolygonProps, Polygon] = js.native


  trait PolylineProps extends PolylineOptions with PathProps with PropsWithChildren {
    val positions: js.Array[LatLngExpression] | js.Array[js.Array[LatLngExpression]]
  }
  @js.native
  @JSImport(REACT_LEAFLET_PACKAGE, "Polyline")
  val Polyline: raw.React.ForwardRefComponent[PolylineProps, Polyline] = js.native


  trait PopupProps extends PopupOptions with EventedProps with PropsWithChildren {
    val onClose: js.UndefOr[js.Function0[Unit]] = js.undefined
    val onOpen: js.UndefOr[js.Function0[Unit]] = js.undefined
    val position: js.UndefOr[LatLngExpression] = js.undefined
  }
  @js.native
  @JSImport(REACT_LEAFLET_PACKAGE, "Popup")
  val Popup: raw.React.ForwardRefComponent[PopupProps, Popup] = js.native


  trait RectangleProps extends PathOptions with PathProps with PropsWithChildren {
    val bounds: LatLngBoundsExpression
  }
  @js.native
  @JSImport(REACT_LEAFLET_PACKAGE, "Rectangle")
  val Rectangle: raw.React.ForwardRefComponent[RectangleProps, Rectangle] = js.native


  trait SVGOverlayProps extends MediaOverlayProps with PropsWithChildren {
    val attributes: js.UndefOr[js.Dictionary[String]] = js.undefined
  }
  @js.native
  @JSImport(REACT_LEAFLET_PACKAGE, "useSVGOverlayElement")
  val useSVGOverlayElement: ElementHookRef[SvgOverlay, dom.svg.SVG] = js.native

  @js.native
  @JSImport(REACT_LEAFLET_PACKAGE, "useSVGOverlay")
  val useSVGOverlay: js.Function1[SVGOverlayProps, ElementHookRef[SvgOverlay, dom.svg.SVG]] = js.native

  @js.native
  @JSImport(REACT_LEAFLET_PACKAGE, "SvgOverlay")
  val SvgOverlay: raw.React.ForwardRefComponent[SVGOverlayProps, SvgOverlay] = js.native


  type ScaleControlProps = ScaleOptions
  @js.native
  @JSImport(REACT_LEAFLET_PACKAGE, "ScaleControl")
  val Scale: raw.React.ForwardRefComponent[ScaleControlProps, Scale] = js.native


  trait TileLayerProps extends TileLayerOptions with LayerProps {
    val url: String
  }
  @js.native
  @JSImport(REACT_LEAFLET_PACKAGE, "TileLayer")
  val TileLayer: raw.React.ForwardRefComponent[TileLayerProps, TileLayer] = js.native


  trait TooltipProps extends TooltipOptions with EventedProps with PropsWithChildren {
    val onClose: js.UndefOr[js.Function0[Unit]] = js.undefined
    val onOpen: js.UndefOr[js.Function0[Unit]] = js.undefined
    val position: js.UndefOr[LatLngExpression] = js.undefined
  }
  @js.native
  @JSImport(REACT_LEAFLET_PACKAGE, "Tooltip")
  val Tooltip: raw.React.ForwardRefComponent[TooltipProps, Tooltip] = js.native


  trait VideoOverlayProps extends MediaOverlayProps with VideoOverlayOptions with PropsWithChildren {
    val play: js.UndefOr[Boolean] = js.undefined
    val url: String | js.Array[String] | dom.html.Video
  }
  @js.native
  @JSImport(REACT_LEAFLET_PACKAGE, "VideoOverlay")
  val VideoOverlay: raw.React.ForwardRefComponent[VideoOverlayProps, VideoOverlay] = js.native


  // TODO LayersControl.tsx
  // TODO WMSTileLayer.tsx

}
