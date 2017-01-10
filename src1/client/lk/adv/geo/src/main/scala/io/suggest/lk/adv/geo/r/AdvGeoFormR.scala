package io.suggest.lk.adv.geo.r

import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.adv.geo.MAdv4Free
import io.suggest.common.maps.leaflet.LeafletConstants
import io.suggest.css.Css
import io.suggest.lk.adv.geo.m.{MGeoAdvs, MRad, MRcvr, MRoot}
import io.suggest.lk.adv.geo.r.geo.exist.{ExistPopupR, ExistShapesR}
import io.suggest.lk.adv.geo.r.geo.rad.RadR
import io.suggest.lk.adv.geo.r.mapf.AdvGeoMapR
import io.suggest.lk.adv.geo.r.oms.OnMainScreenR
import io.suggest.lk.adv.geo.r.rcvr.{RcvrMarkersR, RcvrPopupR}
import io.suggest.lk.adv.r.Adv4FreeR
import io.suggest.lk.tags.edit.r.TagsEditR
import io.suggest.sjs.common.geo.json.GjFeature
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.dt.period.r._
import io.suggest.sjs.leaflet.marker.Marker
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import react.leaflet.control.LocateControlR
import react.leaflet.layer.{TileLayerPropsR, TileLayerR}

import scala.scalajs.js

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
  * Этот react-компонент формы должен подключаться в diode circuit через wrap().
  */
object AdvGeoFormR extends Log {

  type Props = ModelProxy[MRoot]


  /** Состояние содержит коннекшены от корневой модели до некоторых обновляемых компонентов.
    *
    * scalac 2.11.8 выдаёт ошибочный warning здесь на тему existential types, см.
    * [[http://scala-language.1934581.n4.nabble.com/Option-What-Option-td4647470.html]]
    * [[https://github.com/scala/scala/pull/4017]]
    */
  case class State(
                    adv4freeConn        : ReactConnectProxy[Option[MAdv4Free]],
                    onMainScrConn       : ReactConnectProxy[OnMainScreenR.PropsVal],
                    rcvrMarkersConn     : ReactConnectProxy[Pot[js.Array[Marker]]],
                    rcvrPopupConn       : ReactConnectProxy[MRcvr],
                    mapPropsConn        : ReactConnectProxy[AdvGeoMapR.PropsVal],
                    geoAdvExistRespConn : ReactConnectProxy[Pot[js.Array[GjFeature]]],
                    geoAdvConn          : ReactConnectProxy[MGeoAdvs],
                    radOptConn          : ReactConnectProxy[Option[MRad]]
                  )


  /** Константный инстанс TileLayer компонента лежит в памяти отдельно, т.к. никаких изменений в нём не требуется. */
  private val _tileLayerU = {
    TileLayerR(
      new TileLayerPropsR {
        override val url           = LeafletConstants.Tiles.URL_OSM_DFLT
        override val detectRetina  = LeafletConstants.Defaults.DETECT_RETINA
        override val attribution   = LeafletConstants.Tiles.ATTRIBUTION_OSM
      }
    )()
  }

  /** Класс для компонента формы. */
  protected class Backend($: BackendScope[Props, _]) {

    /** Рендер всея формы. */
    def render(p: Props, s: State) = {
      // без <form>, т.к. форма теперь сущетсвует на уровне JS в состояние diode.
      <.div(
        ^.`class` := Css.Lk.Adv.FORM_OUTER_DIV,

        s.adv4freeConn { prox =>
          prox()
            .map[ReactElement] { _ =>
              Adv4FreeR(prox.zoom(_.get))
            }
            .orNull
        },

        // Верхняя половина, левая колонка:
        <.div(
          ^.`class` := Css.Lk.Adv.LEFT_BAR,

          // Галочка размещения на главном экране
          s.onMainScrConn( OnMainScreenR.apply ),

          <.br,
          <.br,

          // Компонент подсистемы выбора тегов:
          p.wrap( _.tags )( TagsEditR.apply )
        ),

        // Верхняя половина, правая колонка:
        p.wrap(_.datePeriod)( DatePeriodR.apply ),

        // Тут немного пустоты нужно...
        <.br,
        <.br,

        // Рендер географической карты:
        s.mapPropsConn { mapProps =>
          AdvGeoMapR(mapProps)(

            // Рендерим основную плитку карты.
            _tileLayerU,

            // Плагин для геолокации текущего юзера.
            LocateControlR()(),

            // Георазмещение: рисуем настраиваемый круг для размещения в радиусе:
            s.radOptConn( RadR.apply ),

            // Рендер кружочков текущих размещений.
            s.geoAdvExistRespConn( ExistShapesR.apply ),
            // Рендер попапа над кружочком георазмещения:
            s.geoAdvConn( ExistPopupR.apply ),

            // MarkerCluster для списка ресиверов, если таковой имеется...
            s.rcvrMarkersConn( RcvrMarkersR.apply ),
            // Рендер опционального попапа над ресивером.
            s.rcvrPopupConn( RcvrPopupR.apply )

          )
        }

      )   // top div
    }     // render()

  }


  protected val component = ReactComponentB[Props]("AdvGeoForm")
    .initialState_P { p =>
      State(
        adv4freeConn    = {
          p.connect(_.form.adv4free)
        },
        onMainScrConn   = p.connect { mroot =>
          OnMainScreenR.PropsVal(
            mroot.form.onMainScreen
          )
        },
        rcvrMarkersConn = p.connect(_.rcvr.markers),
        rcvrPopupConn   = p.connect(_.rcvr),
        mapPropsConn = p.connect { mroot =>
          AdvGeoMapR.PropsVal(
            mapState      = mroot.form.mapState,
            locationFound = mroot.form.locationFound
          )
        },
        geoAdvExistRespConn = p.connect(_.geoAdv.existResp),
        geoAdvConn          = p.connect(_.geoAdv),
        radOptConn          = p.connect(_.rad)
      )
    }
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)

}
