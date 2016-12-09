package io.suggest.lk.adv.geo.r

import io.suggest.adv.geo.AdvGeoConstants
import io.suggest.common.maps.leaflet.LeafletConstants
import io.suggest.css.Css
import io.suggest.lk.adv.m.IAdv4FreeProps
import io.suggest.lk.adv.r.Adv4FreeR
import io.suggest.lk.router.jsRoutes
import io.suggest.lk.tags.edit.r.TagsEditR
import io.suggest.lk.vm.LkMessagesWindow.Messages
import io.suggest.sjs.dt.period.m.IDatesPeriodInfo
import io.suggest.sjs.dt.period.r._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import react.leaflet.lmap.LMapR
import react.leaflet.popup.PopupR
import io.suggest.sjs.leaflet.L
import react.leaflet.control.LocateControlR
import react.leaflet.layer.TileLayerR

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

  /** Модель состояния. */
  case class State(
    onMainScreen : Boolean = true
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
          className = Css.Lk.Adv.Geo.MAP_CONTAINER
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

  }

  protected val component = ReactComponentB[Props]("AdvGeoForm")
    .initialState( State() )
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)

}
