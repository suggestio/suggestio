package io.suggest.lk.adv.geo.r

import io.suggest.adv.geo.AdvGeoConstants
import io.suggest.common.maps.leaflet.LeafletConstants
import io.suggest.css.Css
import io.suggest.lk.adv.geo.tags.m.MMapGjResp
import io.suggest.lk.adv.geo.tags.vm.AdIdInp
import io.suggest.lk.adv.m.IAdv4FreeProps
import io.suggest.lk.adv.r.Adv4FreeR
import io.suggest.lk.router.jsRoutes
import io.suggest.lk.tags.edit.r.TagsEditR
import io.suggest.lk.vm.LkMessagesWindow.Messages
import io.suggest.sjs.common.xhr.Xhr
import io.suggest.sjs.dt.period.m.IDatesPeriodInfo
import io.suggest.sjs.dt.period.r._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import react.leaflet.lmap.LMapR
import react.leaflet.popup.PopupR
import io.suggest.sjs.leaflet.L
import io.suggest.sjs.leaflet.event.LocationEvent
import io.suggest.sjs.leaflet.marker.Marker
import react.leaflet.control.LocateControlR
import react.leaflet.layer.TileLayerR

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.UndefOr

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
object AdvGeoFormR {

  /** Модель пропертисов, приходящая свыше из инициализатора. */
  case class Props(
    formActionUrl   : String,
    adv4free        : Option[IAdv4FreeProps],
    method          : String = "POST"
  )

  /** Модель состояния.
    * @param rcvrMarkers Маркеры точек-ресиверов для карты ресиверов, полученные с сервера.
    *                    null -- дефолтовое состояние. Почти сразу же заменяется на
    *                    Left(Future[Resp]) -- Запущен XHR за списком маркеров.
    *                    Right([Markers]) -- массив маркеров, передаваемых в MarkerCluster при рендере.
    * @param locationFound false -- значит callback нужен для геолокации.
    *                      true -- геолокация уже была, действия уже были приняты.
    */
  case class State(
    onMainScreen : Boolean = true,
    rcvrMarkers  : Either[Future[_], js.Array[Marker]] = null,
    locationFound: Boolean = false
    //datesPeriodInfo : Option[IDatesPeriodInfo] = None
  ) {
    def withOnMainScreen(oms2: Boolean) = copy(onMainScreen = oms2)
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
    def onLocationFound(): Unit = {
      val sCb = $.modState { s0 =>
        s0.copy(
          locationFound = true
        )
      }
      sCb.runNow()
    }

    def onMainScreenChanged(e: ReactEventI): Callback = {
      val oms2 = e.target.checked
      val sCb = $.modState {
        _.withOnMainScreen( oms2 )
      }
      sCb >> Callback.TODO("onMainScreenChanged()")
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
            UndefOr.any2undefOrA({ _: LocationEvent => onLocationFound() })
          }
        )(

          // Рендерим основную плитку карты.
          TileLayerR(
            url           = LeafletConstants.Tiles.URL_OSM_DFLT,
            detectRetina  = LeafletConstants.Defaults.DETECT_RETINA,
            attribution   = LeafletConstants.Tiles.ATTRIBUTION_OSM
          )(),

          LocateControlR()(),

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
    def _doMarkersRequest(): Unit = {
      for {
        adIdInp <- AdIdInp.find()
        adId    <- adIdInp.adId
      } {
        val route = jsRoutes.controllers.LkAdvGeo.advRcvrsGeoJson(adId)

        // Надо запустить запрос на сервер для получения списка узлов.
        val gjFut = Xhr.requestJson(route)
          .map(MMapGjResp.apply)

        for {
          gj <- gjFut
        } yield {
          // TODO Отрендерить маркеры.
        }
      }

    }

    /**
      * После окончания рендера надо запустить XHR за точками.
      * XHR-запросы рекомендуется запускать только после рендера.
      *
      * @see [[http://stackoverflow.com/a/27139507]]
      */
    def componentDidMount(): Callback = {
      $.state >>= { state =>
        if (state.rcvrMarkers == null) {
          // Требуется запустить запрос маркеров
          Callback.TODO("_doMarkersRequest()")
        } else {
          Callback.empty
        }
      }
    }

  }

  protected val component = ReactComponentB[Props]("AdvGeoForm")
    .initialState( State() )
    .renderBackend[Backend]
    .componentDidMount( _.backend.componentDidMount() )
    .build

  def apply(props: Props) = component(props)

}
