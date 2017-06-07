package io.suggest.lk.adv.geo.r

import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.bill.price.dsl.IPriceDslTerm
import io.suggest.css.Css
import io.suggest.lk.adv.geo.m._
import io.suggest.lk.adv.geo.r.oms.OnMainScreenR
import io.suggest.lk.adv.geo.r.rcvr.RcvrPopupR
import io.suggest.lk.adv.r.{Adv4FreeR, ItemsPricesR}
import io.suggest.lk.tags.edit.r.TagsEditR
import io.suggest.maps.m.{MExistGeoPopupS, MExistGeoS, MMapS, MRad}
import io.suggest.maps.r.rad.{RadEnabledR, RadR}
import io.suggest.maps.r._
import io.suggest.react.ReactCommonUtil.Implicits.reactElOpt2reactEl
import io.suggest.sjs.common.geo.json.GjFeature
import io.suggest.sjs.dt.period.r._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import react.leaflet.control.LocateControlR
import io.suggest.sjs.common.spa.OptFastEq.Wrapped
import MExistGeoPopupS.MGeoCurPopupSFastEq
import io.suggest.maps.nodes.MGeoNodesResp

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
  import MMapS.MMapSFastEq
  import MExistGeoS.MExistGeoSFastEq
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
                              mmapC               : ReactConnectProxy[MMapS],
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
    def render(p: Props, s: State): ReactElement = {
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
        s.mmapC { mapProps =>
          LGeoMapR(mapProps)(

            // Рендерим основную плитку карты.
            ReactLeafletUtil.Tiles.OsmDefault,

            // Плагин для геолокации текущего юзера.
            LocateControlR()(),

            // Рендер кружочков текущих размещений.
            s.geoAdvExistGjC( ExistAdvGeoShapesR.apply ),
            // Рендер попапа над кружочком георазмещения:
            s.geoAdvPopupC( ExistPopupR.apply ),

            // Запрешаем рендер красного круга пока не нарисованы все остальные. Так надо, чтобы он был поверх их всех.
            s.geoAdvExistGjC { potProx =>
              // Георазмещение: рисуем настраиваемый круг для размещения в радиусе:
              for (_ <- potProx().toOption) yield {
                s.mRadOptC( RadR.apply )
              }
            },

            // MarkerCluster для списка ресиверов, если таковой имеется...
            s.rcvrsGeoC( RcvrMarkersR.apply ),
            // Рендер опционального попапа над ресивером.
            s.rcvrPopupC( RcvrPopupR.apply )

          )
        },

        <.br,

        // Рендерить табличку с данными по рассчёту текущей цены:
        s.priceDslOptC { ItemsPricesR.apply }

      )   // top div
    }     // render()

  }


  protected val component = ReactComponentB[Props]("AdvGeoForm")
    .initialState_P { p =>
      val mradOptZoomF = { r: MRoot => r.rad }
      State(
        onMainScrC   = p.connect { mroot =>
          OnMainScreenR.PropsVal(
            mroot.other.onMainScreen
          )
        },
        rcvrsGeoC        = p.connect(_.rcvr.rcvrsGeo),
        rcvrPopupC       = p.connect(_.rcvr),
        mmapC            = p.connect(_.mmap),
        geoAdvExistGjC   = p.connect(_.geoAdv.geoJson),
        geoAdvPopupC     = p.connect(_.geoAdv.popup),
        // Для рендера подходит только radEnabled, а он у нас генерится заново каждый раз.
        mRadOptC         = p.connect(mradOptZoomF),
        radEnabledPropsC = RadEnabledR.radEnabledPropsConn(
          p.zoom(mradOptZoomF),
          renderHintAsText = false
        ),
        priceDslOptC     = p.connect( _.bill.price.respDslOpt ),
        mDocC            = p.connect(_.other.doc)
      )
    }
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)

}
