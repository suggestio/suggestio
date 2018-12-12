package io.suggest.lk.adv.geo.r

import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.bill.price.dsl.IPriceDslTerm
import io.suggest.css.Css
import io.suggest.geo.json.GjFeature
import io.suggest.lk.adv.geo.m._
import io.suggest.lk.adv.geo.r.oms.OnMainScreenR
import io.suggest.lk.adv.geo.r.rcvr.RcvrPopupR
import io.suggest.lk.adv.r.{Adv4FreeR, ItemsPricesR}
import io.suggest.lk.tags.edit.r.TagsEditR
import io.suggest.maps.m.{MExistGeoPopupS, MGeoMapPropsR, MRad}
import io.suggest.maps.r.rad.{RadEnabledR, RadR}
import io.suggest.maps.r._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.sjs.dt.period.r._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import react.leaflet.control.LocateControlR
import io.suggest.spa.OptFastEq.Wrapped
import io.suggest.maps.nodes.MGeoNodesResp
import io.suggest.react.ReactCommonUtil
import react.leaflet.lmap.LMapR
import react.leaflet.popup.LPopupR

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
object AdvGeoFormR {

  // Без пинка, FastEq не подцеплялись к работе и вызывали лишней re-render внутри коннекшенов.
  import MRcvr.MRcvrFastEq
  import MGeoMapPropsR.MGeoMapPropsRFastEq
  import MExistGeoPopupS.MGeoCurPopupSFastEq
  import MRad.MRadFastEq
  import io.suggest.lk.tags.edit.m.MTagsEditState.MTagsEditStateFastEq


  type Props = ModelProxy[MRoot]


  /** Состояние содержит коннекшены от корневой модели до некоторых обновляемых компонентов.
    *
    * scalac 2.11.8 выдаёт ошибочный warning здесь на тему existential types, см.
    * [[http://scala-language.1934581.n4.nabble.com/Option-What-Option-td4647470.html]]
    * [[https://github.com/scala/scala/pull/4017]]
    */
  protected case class State(
                              onMainScrC          : ReactConnectProxy[OnMainScreenR.PropsVal],
                              rcvrsGeoC           : ReactConnectProxy[Pot[MGeoNodesResp]],
                              rcvrPopupC          : ReactConnectProxy[MRcvr],
                              geoMapPropsC        : ReactConnectProxy[MGeoMapPropsR],
                              geoAdvExistGjC      : ReactConnectProxy[Pot[js.Array[GjFeature]]],
                              geoAdvPopupC        : ReactConnectProxy[MExistGeoPopupS],
                              mRadOptC            : ReactConnectProxy[Option[MRad]],
                              radEnabledPropsC    : ReactConnectProxy[RadEnabledR.PropsVal],
                              priceDslOptC        : ReactConnectProxy[Option[IPriceDslTerm]],
                              mDocC               : ReactConnectProxy[MDocS]
                            )


  /** Класс для компонента формы. */
  protected class Backend($: BackendScope[Props, _]) {

    /** Рендер всея формы. */
    def render(p: Props, s: State): VdomElement = {
      // без <form>, т.к. форма теперь сущетсвует на уровне JS в состоянии diode.
      <.div(
        ^.`class` := Css.Lk.Adv.FORM_OUTER_DIV,

        // Рендер компонента документации.
        s.mDocC { DocR.apply },

        // Галочка бесплатного размещения для суперюзеров.
        p.wrap(_.adv4free) { a4fOptProx =>
          Adv4FreeR(a4fOptProx)
        },

        // Верхняя половина, левая колонка:
        <.div(
          ^.`class` := Css.Lk.Adv.LEFT_BAR,

          // Галочка активности георазмещения на карте.
          s.radEnabledPropsC( RadEnabledR.apply ),
          <.br,
          <.br,

          // Галочка размещения на главном экране
          s.onMainScrC( OnMainScreenR.apply ),

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

        // Если не удалось прочитать маркеры ресиверов с сервера, то отрендерить заметное сообщение об ошибке.
        s.rcvrsGeoC { x =>
          MapInitFailR( x.asInstanceOf[ModelProxy[Pot[_]]] )
        },

        // Рендер географической карты:
        {
          val mapChildren = List[VdomNode](
            // Рендерим основную плитку карты.
            ReactLeafletUtil.Tiles.OsmDefault,

            // Плагин для геолокации текущего юзера.
            LocateControlR(),

            // Рендер кружочков текущих размещений.
            s.geoAdvExistGjC( ExistAdvGeoShapesR.apply ),

            // Рендер попапа над кружочком георазмещения:
            s.geoAdvPopupC( ExistPopupR.apply ),

            // Запрешаем рендер красного круга пока не нарисованы все остальные. Так надо, чтобы он был поверх их всех.
            s.geoAdvExistGjC { potProxy =>
              val pot = potProxy.value
              // Георазмещение: рисуем настраиваемый круг для размещения в радиусе:
              // TODO Отрендерить попап ошибки, если failed.
              ReactCommonUtil.maybeEl( pot.isReady || pot.isFailed ) {
                s.mRadOptC( RadR.apply )
              }
            },

            // MarkerCluster для списка ресиверов, если таковой имеется...
            s.rcvrsGeoC( RcvrMarkersR.applyNoChildren ),

            // Рендер опционального попапа над ресивером.
            s.rcvrPopupC( RcvrPopupR.apply )
          )
          s.geoMapPropsC { mapProps =>
            LMapR(
              LGeoMapR.lmMapSProxy2lMapProps(
                mapProps
              )
            )( mapChildren: _* )
          }
        },

        <.br,

        // Рендерить табличку с данными по рассчёту текущей цены:
        s.priceDslOptC { ItemsPricesR.apply }

      )   // top div
    }     // render()

  }


  protected val component = ScalaComponent.builder[Props]("AdvGeoForm")
    .initialStateFromProps { propsProxy =>
      val mradOptZoomF = { r: MRoot => r.rad }
      val mapCssClass = Some( Css.Lk.Maps.MAP_CONTAINER )

      State(
        onMainScrC   = propsProxy.connect { mroot =>
          OnMainScreenR.PropsVal(
            mroot.other.onMainScreen
          )
        },
        rcvrsGeoC        = propsProxy.connect(_.rcvr.rcvrsGeo),
        rcvrPopupC       = propsProxy.connect(_.rcvr),
        geoMapPropsC     = propsProxy.connect { p =>
          val m = p.mmap
          MGeoMapPropsR(
            center        = m.center,
            zoom          = m.zoom,
            locationFound = m.locationFound,
            cssClass      = mapCssClass
          )
        },
        geoAdvExistGjC   = propsProxy.connect(_.geoAdv.geoJson),
        geoAdvPopupC     = propsProxy.connect(_.geoAdv.popup),
        // Для рендера подходит только radEnabled, а он у нас генерится заново каждый раз.
        mRadOptC         = propsProxy.connect(mradOptZoomF),
        radEnabledPropsC = RadEnabledR.radEnabledPropsConn(
          propsProxy.zoom(mradOptZoomF),
          renderHintAsText = false
        ),
        priceDslOptC     = propsProxy.connect( _.bill.price.respDslOpt ),
        mDocC            = propsProxy.connect(_.other.doc)
      )
    }
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)

}
