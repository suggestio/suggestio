package io.suggest.lk.adv.geo.r

import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.bill.price.dsl.PriceDsl
import io.suggest.css.Css
import io.suggest.geo.json.GjFeature
import io.suggest.lk.adv.geo.m._
import io.suggest.lk.adv.geo.r.oms.OnMainScreenR
import io.suggest.lk.adv.geo.r.rcvr.RcvrPopupR
import io.suggest.lk.adv.r.{Adv4FreeR, ItemsPricesR}
import io.suggest.lk.tags.edit.r.TagsEditR
import io.suggest.maps.m.{MAdvGeoS, MExistGeoPopupS, MGeoMapPropsR, MRad}
import io.suggest.maps.r.rad.{RadEnabledR, RadR}
import io.suggest.maps.r._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.sjs.dt.period.r._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import react.leaflet.control.LocateControlR
import io.suggest.maps.nodes.MGeoNodesResp
import io.suggest.react.ReactCommonUtil
import io.suggest.spa.FastEqUtil
import react.leaflet.lmap.LMapR
import scalaz.Tree
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._

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
final class AdvGeoFormR(
                         rcvrPopupR: RcvrPopupR,
                         tagsEditR: TagsEditR,
                         docR: DocR,
                         val onMainScreenR: OnMainScreenR,
                       ) {

  // Без пинка, FastEq не подцеплялись к работе и вызывали лишней re-render внутри коннекшенов.
  import MRcvr.MRcvrFastEq
  import MGeoMapPropsR.MGeoMapPropsRFastEq
  import MExistGeoPopupS.MGeoCurPopupSFastEq
  import io.suggest.lk.tags.edit.m.MTagsEditState.MTagsEditStateFastEq


  type Props = ModelProxy[MRoot]


  /** Состояние содержит коннекшены от корневой модели до некоторых обновляемых компонентов.
    *
    * scalac 2.11.8 выдаёт ошибочный warning здесь на тему existential types, см.
    * [[http://scala-language.1934581.n4.nabble.com/Option-What-Option-td4647470.html]]
    * [[https://github.com/scala/scala/pull/4017]]
    */
  protected case class State(
                              onMainScrC          : ReactConnectProxy[onMainScreenR.PropsVal],
                              rcvrsGeoC           : ReactConnectProxy[Pot[MGeoNodesResp]],
                              rcvrPopupC          : ReactConnectProxy[MRcvr],
                              geoMapPropsC        : ReactConnectProxy[MGeoMapPropsR],
                              geoAdvExistGjC      : ReactConnectProxy[Pot[js.Array[GjFeature]]],
                              geoAdvPopupC        : ReactConnectProxy[MExistGeoPopupS],
                              mRadOptC            : ReactConnectProxy[MAdvGeoS],
                              radEnabledPropsC    : ReactConnectProxy[RadEnabledR.PropsVal],
                              priceDslOptC        : ReactConnectProxy[Option[Tree[PriceDsl]]],
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
        s.mDocC { docR.component.apply },

        // Галочка бесплатного размещения для суперюзеров.
        p.wrap(_.adv.free)( Adv4FreeR.component.apply ),

        // Верхняя половина, левая колонка:
        <.div(
          ^.`class` := Css.Lk.Adv.LEFT_BAR,

          // Галочка активности георазмещения на карте.
          s.radEnabledPropsC( RadEnabledR.component.apply ),
          <.br,
          <.br,

          // Галочка размещения на главном экране
          s.onMainScrC( onMainScreenR.component.apply ),

          <.br,
          <.br,

          // Компонент подсистемы выбора тегов:
          p.wrap( _.adv.tags )( tagsEditR.component.apply )
        ),

        // Верхняя половина, правая колонка:
        p.wrap(_.adv.datePeriod)( DatePeriodR.component.apply ),

        // Тут немного пустоты нужно...
        <.br,
        <.br,

        // Если не удалось прочитать маркеры ресиверов с сервера, то отрендерить заметное сообщение об ошибке.
        s.rcvrsGeoC { x =>
          MapInitFailR.component( x.asInstanceOf[ModelProxy[Pot[_]]] )
        },

        // Рендер географической карты:
        {
          val mapChildren = List[VdomNode](
            // Рендерим основную плитку карты.
            ReactLeafletUtil.Tiles.OsmDefault,

            // Плагин для геолокации текущего юзера.
            LocateControlR(),

            // Рендер кружочков текущих размещений.
            s.geoAdvExistGjC( ExistAdvGeoShapesR.component.apply ),

            // Рендер попапа над кружочком георазмещения:
            s.geoAdvPopupC( ExistPopupR.component.apply ),

            // Запрешаем рендер красного круга пока не нарисованы все остальные. Так надо, чтобы он был поверх их всех.
            {
              lazy val inner = s.mRadOptC( RadR.component.apply )
              s.geoAdvExistGjC { potProxy =>
                val pot = potProxy.value
                // Георазмещение: рисуем настраиваемый круг для размещения в радиусе:
                // TODO Отрендерить попап ошибки, если failed.
                ReactCommonUtil.maybeEl( pot.isReady || pot.isFailed )(inner)
              }
            },

            // MarkerCluster для списка ресиверов, если таковой имеется...
            s.rcvrsGeoC( RcvrMarkersR.component(_)() ),

            // Рендер опционального попапа над ресивером.
            s.rcvrPopupC( rcvrPopupR.component.apply )
          )
          val lgmCtx = LGeoMapR.LgmCtx.mk( $ )
          s.geoMapPropsC { mapProps =>
            LMapR.component(
              LGeoMapR.lmMapSProxy2lMapProps( mapProps, lgmCtx )
            )( mapChildren: _* )
          }
        },

        <.br,

        // Рендерить табличку с данными по рассчёту текущей цены:
        s.priceDslOptC { ItemsPricesR.component.apply },

      )   // top div
    }     // render()

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      val mradOptZoomF = { r: MRoot => r.geo.rad }
      val mapCssClass = Some( Css.Lk.Maps.MAP_CONTAINER )

      State(
        onMainScrC   = propsProxy.connect { mroot =>
          onMainScreenR.PropsVal(
            mroot.other.onMainScreen
          )
        },
        rcvrsGeoC        = propsProxy.connect(_.adv.rcvr.rcvrsGeo),
        rcvrPopupC       = propsProxy.connect(_.adv.rcvr),
        geoMapPropsC     = propsProxy.connect { p =>
          MGeoMapPropsR(
            mapS          = p.geo.mmap,
            cssClass      = mapCssClass,
          )
        },
        geoAdvExistGjC   = propsProxy.connect(_.geo.existAdv.geoJson),
        geoAdvPopupC     = propsProxy.connect(_.geo.existAdv.popup),
        // Для рендера подходит только radEnabled, а он у нас генерится заново каждый раз.
        mRadOptC         = propsProxy.connect(_.geo)( FastEqUtil[MAdvGeoS] { (a, b) =>
          (a.rad ===* b.rad) &&
          (a.radPopup ==* b.radPopup)
        }),
        radEnabledPropsC = RadEnabledR.radEnabledPropsConn(
          propsProxy.zoom(mradOptZoomF),
          renderHintAsText = false,
        ),
        priceDslOptC     = propsProxy.connect( _.adv.bill.price.respDslOpt ),
        mDocC            = propsProxy.connect(_.other.doc)
      )
    }
    .renderBackend[Backend]
    .build

}
