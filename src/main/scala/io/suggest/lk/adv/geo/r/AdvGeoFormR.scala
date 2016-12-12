package io.suggest.lk.adv.geo.r

import io.suggest.adv.geo.AdvGeoConstants
import io.suggest.common.maps.leaflet.LeafletConstants
import io.suggest.css.Css
import io.suggest.lk.adv.geo.tags.m.MMapGjResp
import io.suggest.lk.adv.geo.u.LkAdvGeoFormUtil
import io.suggest.lk.adv.m.IAdv4FreeProps
import io.suggest.lk.adv.r.Adv4FreeR
import io.suggest.lk.router.jsRoutes
import io.suggest.lk.tags.edit.r.TagsEditR
import io.suggest.lk.vm.LkMessagesWindow.Messages
import io.suggest.sjs.common.xhr.Xhr
import io.suggest.sjs.dt.period.m.IDatesPeriodInfo
import io.suggest.sjs.dt.period.r._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.ErrorMsgs
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import react.leaflet.lmap.LMapR
import react.leaflet.popup.PopupR
import io.suggest.sjs.leaflet.L
import io.suggest.sjs.leaflet.event.LocationEvent
import io.suggest.sjs.leaflet.marker.{Marker, MarkerEvent}
import io.suggest.lk.adv.geo.tags.m.MarkerNodeId
import react.leaflet.control.LocateControlR
import react.leaflet.layer.TileLayerR
import react.leaflet.marker.MarkerClusterGroupR

import scala.concurrent.Future
import scala.scalajs.js
import scala.util.{Failure, Success}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.12.16 18:04
  * Description: Компонент формы георазмещения на базе react.js.
  *
  * Компонент состоит из html-формы и карты leaflet.
  *
  * Десериализацией стартового состояния также занимается этот компонент.
  *
  */
object AdvGeoFormR extends Log {

  /** Модель пропертисов, приходящая свыше из инициализатора. */
  case class Props(
    adId            : String,
    formActionUrl   : String,
    adv4free        : Option[IAdv4FreeProps],
    method          : String = "POST"
  )

  /** Модель состояния.
    * @param rcvrMarkers Маркеры точек-ресиверов для карты ресиверов, полученные с сервера.
    *                    None -- дефолтовое состояние. Почти сразу же заменяется на
    *                    Left(Future[Resp]) -- Запущен XHR за списком маркеров.
    *                    Right([Markers]) -- массив маркеров, передаваемых в MarkerCluster при рендере.
    * @param locationFound false -- значит callback нужен для геолокации.
    *                      true -- геолокация уже была, действия уже были приняты.
    */
  case class State(
    onMainScreen : Boolean = true,
    rcvrMarkers  : Option[Either[Future[_], js.Array[Marker]]] = None,
    locationFound: Boolean = false
    //datesPeriodInfo : Option[IDatesPeriodInfo] = None
  ) {
    def withOnMainScreen(oms2: Boolean) = copy(onMainScreen = oms2)
    def withRcvrMarkers(rm: Option[Either[Future[_], js.Array[Marker]]]) = copy(rcvrMarkers = rm)
  }

  /** Класс для компонента формы. */
  protected class Backend($: BackendScope[Props, State]) {

    def tagsChanged(): Unit = {
      println("tagsChanged()")
    }

    def datePeriodChanged(): Unit = {
      println("datePeriodChanged()")
    }

    def adv4freeChanged(): Unit = {
      println("adv4freeChanged()")
    }

    // TODO Сделать Callback, без runNow()
    def onLocationFound(locEvent: LocationEvent): Unit = {
      val sCb = $.modState { s0 =>
        s0.copy(
          locationFound = true
        )
      }
      sCb.runNow()
    }

    /** Реакция на изменение галочки onMainScreen. */
    def onMainScreenChanged(e: ReactEventI): Callback = {
      val oms2 = e.target.checked
      val sCb = $.modState {
        _.withOnMainScreen( oms2 )
      }
      sCb >> Callback.TODO("onMainScreenChanged()")
    }

    /** Реакция на клик по маркеру. */
    def onMarkerClicked(layerEvent: MarkerEvent): Unit = {
      val marker = layerEvent.layer
      // TODO Почему-то тут не срабатывает implicit convertion... Приходится явно заворачивать
      val nodeId = MarkerNodeId(marker).nodeId
      println( "marker clicked: " + nodeId )
      // TODO Нужно тут запускать получение попапа с сервера, попутно центрируя карту по маркеру...
    }


    /** Рендер всея формы. */
    def render(props: Props, state: State) = {
      <.div(
        ^.`class` := Css.Lk.Adv.FORM_OUTER_DIV,

        // Рендер самой формы...
        <.form(
          ^.method := props.method,
          ^.action := props.formActionUrl,

          for (adv4free <- props.adv4free) yield {
            Adv4FreeR(
              Adv4FreeR.Props(
                onChange = adv4freeChanged,
                config   = adv4free
              )
            )
          },

          // Верхняя половина, левая колонка:
          <.div(
            ^.`class` := Css.Lk.Adv.LEFT_BAR,

            // TODO Галочка размещения на главном экране
            <.label(
              <.input(
                ^.`type`    := "checkbox",
                ^.name      := AdvGeoConstants.OnMainScreen.FN,
                ^.checked   := state.onMainScreen,
                ^.onChange ==> onMainScreenChanged
              ),
              <.span(
                ^.`class` := Css.Input.STYLED_CHECKBOX
              ),
              Messages( "Adv.on.main.screen" )
            ),
            <.br,
            <.br,

            // Система выбора тегов:
            TagsEditR(
              TagsEditR.Props(
                tagSearchRoute  = jsRoutes.controllers.LkAdvGeo.tagsSearch,
                onChange        = tagsChanged
              )
            )
          ),

          // Верхняя половина, правая колонка:
          DtpCont(
            DtpOptions(
              DtpOptions.Props(
                onChange = datePeriodChanged
              )
            )
          )

        ),

        // Тут немного пустоты нужно...
        <.br,
        <.br,

        // Карта должна рендерится сюда:
        LMapR(
          center    = L.latLng(20, 20),
          zoom      = 10,
          className = Css.Lk.Adv.Geo.MAP_CONTAINER,
          onLocationFound = if (state.locationFound)
            js.undefined
          else {
            // TODO Нужен Callback тут вместо голой функции
            onLocationFound _
          }
        )(

          // Рендерим основную плитку карты.
          TileLayerR(
            url           = LeafletConstants.Tiles.URL_OSM_DFLT,
            detectRetina  = LeafletConstants.Defaults.DETECT_RETINA,
            attribution   = LeafletConstants.Tiles.ATTRIBUTION_OSM
          )(),

          // Плагин для геолокации текущего юзера.
          LocateControlR()(),

          // MarkerCluster для списка ресиверов, если таковой имеется...
          state.rcvrMarkers
            .flatMap(_.right.toOption)
            .iterator
            .map { markers =>
              MarkerClusterGroupR(
                markers = markers,
                onMarkerClick = onMarkerClicked _
              )()
            }
            .toSeq,

          PopupR( position = L.latLng(50, 50) )(
            <.div(
              <.h1(
                "XYNTA TEST"
              ),
              <.br
            )
          )
        )
      )

    }


    /** Запуск запроса маркеров ресиверов для рендера на карте. */
    def _doMarkersRequest: CallbackTo[Future[js.Array[Marker]]] = {
      for (props <- $.props) yield {
        val route = jsRoutes.controllers.LkAdvGeo.advRcvrsGeoJson( props.adId )

        // Надо запустить запрос на сервер для получения списка узлов.
        for (resp <- Xhr.requestJson(route)) yield {
          val gj = MMapGjResp(resp)
          LkAdvGeoFormUtil.geoJsonToClusterMarkers( gj.featuresIter )
        }
      }
    }

    /**
      * После окончания рендера надо запустить XHR за точками.
      * XHR-запросы рекомендуется запускать только после рендера.
      *
      * @see [[http://stackoverflow.com/a/27139507]]
      */
    def componentDidMount: Callback = {
      $.modStateCB { s0 =>
        if (s0.rcvrMarkers.isEmpty) {
          // Требуется запустить запрос маркеров с сервера
          for (fut <- _doMarkersRequest) yield {
            fut.onComplete {
              case Success(markersArr) =>
                // TODO Тут надо разрулить Callback-по умному как-то.
                markersReady(markersArr).runNow()
              case Failure(ex) =>
                LOG.error( ErrorMsgs.LK_ADV_GEO_MAP_GJ_REQ_FAIL, ex )
            }
            s0.withRcvrMarkers( Some(Left(fut)) )
          }
        } else {
          // Не требуется модификация состояния, .
          CallbackTo(s0)
        }
      }
    }


    /** Реакция на успешное завершение запроса со списком ресиверов. */
    def markersReady(markersArr: js.Array[Marker]): Callback = {
      $.modState { s0 =>
        s0.withRcvrMarkers( Some(Right(markersArr)) )
      }
    }

  }


  protected val component = ReactComponentB[Props]("AdvGeoForm")
    .initialState( State() )
    .renderBackend[Backend]
    .componentDidMount( _.backend.componentDidMount )
    .build

  def apply(props: Props) = component(props)

}
