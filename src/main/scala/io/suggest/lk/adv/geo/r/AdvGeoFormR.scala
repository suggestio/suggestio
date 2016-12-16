package io.suggest.lk.adv.geo.r

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.adv.geo.AdvGeoConstants.AdnNodes.Req
import io.suggest.adv.geo.{MAdv4FreeS, RcvrKey}
import io.suggest.common.maps.leaflet.LeafletConstants
import io.suggest.css.Css
import io.suggest.lk.adv.geo.m.{IRcvrPopupResp, MMapGjResp, MRoot, MarkerNodeId}
import io.suggest.lk.adv.geo.r.oms.OnMainScreenR
import io.suggest.lk.adv.geo.u.LkAdvGeoFormUtil
import io.suggest.lk.adv.r.Adv4FreeR
import io.suggest.lk.router.jsRoutes
import io.suggest.lk.tags.edit.r.TagsEditR
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.model.SjsRoute
import io.suggest.sjs.common.msg.ErrorMsgs
import io.suggest.sjs.common.xhr.Xhr
import io.suggest.sjs.dt.period.r._
import io.suggest.sjs.leaflet.L
import io.suggest.sjs.leaflet.event.LocationEvent
import io.suggest.sjs.leaflet.map.LatLng
import io.suggest.sjs.leaflet.marker.{Marker, MarkerEvent}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import react.leaflet.control.LocateControlR
import react.leaflet.layer.TileLayerR
import react.leaflet.lmap.LMapR
import react.leaflet.marker.MarkerClusterGroupR

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSON
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

  type Props = ModelProxy[MRoot]


  case class State(
                  adv4freeConn    : ReactConnectProxy[Option[MAdv4FreeS]],
                  onMainScrConn   : ReactConnectProxy[Boolean]
                  )


  /** Класс для компонента формы. */
  protected class Backend($: BackendScope[Props, _]) {

    def datePeriodChanged(): Unit = {
      println("datePeriodChanged()")
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


    /** Реакция на клик по маркеру. */
    def onMarkerClicked(layerEvent: MarkerEvent): Unit = {
      val marker = layerEvent.layer
      // TODO Почему-то тут не срабатывает implicit convertion... Приходится явно заворачивать
      val nodeId = MarkerNodeId(marker).nodeId.get
      val latLng = marker.getLatLng()

      val cb = for {
        props <- $.props

        r <- {
          val route = jsRoutes.controllers.LkAdvGeo.rcvrMapPopup(
            adId    = props.adId,
            nodeId  = nodeId
          )
          val fut = for (resp <- Xhr.requestJson(route)) yield {
            val r = resp.asInstanceOf[IRcvrPopupResp]
            _markerPopupRespReceived(r)
            r
          }

          $.modState { s0 =>
            val ps = RcvrPopupState(
              nodeId = nodeId,
              latLng = latLng,
              resp   = Left(fut)
            )
            s0.withRcvrPopup( Some(ps) )
          }
        }
      } yield {
        r
      }

      cb.runNow()
    }

    /** Реакция на ответ */
    def _markerPopupRespReceived(resp: IRcvrPopupResp): Unit = {
      val cb = $.modState { s0 =>
        s0.rcvrPopup.fold[State] {
          LOG.warn( ErrorMsgs.UNEXPECTED_RCVR_POPUP_SRV_RESP, msg = JSON.stringify(resp) )
          s0
        } { rps =>
          val rps2 = rps.copy(
            resp = Right(resp)
          )
          s0.withRcvrPopup( Some(rps2) )
        }
      }
      cb.runNow()
    }


    /** Рендер всея формы. */
    def render(p: Props, s: State) = {
      <.div(
        ^.`class` := Css.Lk.Adv.FORM_OUTER_DIV,

        s.adv4freeConn( Adv4FreeR.apply ),

        // Рендер самой формы (без <form>, т.к. форма теперь сущетсвует на уровне JS в состояние diode)...

          // Верхняя половина, левая колонка:
          <.div(
            ^.`class` := Css.Lk.Adv.LEFT_BAR,

            // Галочка размещения на главном экране
            s.onMainScrConn( OnMainScreenR.apply ),

            <.br,
            <.br,

            // Подсистема выбора тегов:
            p.wrap(m => TagsEditR.PropsVal(m.tagsFound, m.form.tags) )( TagsEditR.apply )
          ),

          // Верхняя половина, правая колонка:
          DtpCont(
            DtpOptions(
              DtpOptions.Props(
                onChange = datePeriodChanged
              )
            )
          ),

          // Отрендерить hidden-input'ы для ресиверов (галочки в попапах на геокарте).
          // TODO Кажется hidden-поля не нужны благодаря поддержки сериализации состояния всей формы.
          /*state.rcvrsMap.nonEmpty ?= <.div(
            ^.`class` := "hidden",
            for {
              ((rk, enabled), i) <- state.rcvrsMap.iterator.zipWithIndex
            } yield {
              <.div(
                ^.key       := rk.toString,
                _hiddenInput(i, fn = Req.FROM_FN, v = rk.from),
                _hiddenInput(i, fn = Req.TO_FN,   v = rk.to),
                for (groupId <- rk.groupId) yield {
                  _hiddenInput(i, fn = Req.GROUP_ID_FN, v = groupId)
                },
                _hiddenInput(i, fn = Req.VALUE_FN, v = enabled.toString)
              )
            }
          )*/


        // Тут немного пустоты нужно...
        <.br,
        <.br,

        // Карта должна рендерится сюда:
        LMapR(
          center    = state.rcvrPopup
            .map(_.latLng)
            .getOrElse {
              // TODO Брать откуда-то из состояния, например.
              L.latLng(20, 20)
            },
          zoom      = 10,
          className = Css.Lk.Adv.Geo.MAP_CONTAINER,
          useFlyTo  = true,
          onLocationFound = {
            if (state.locationFound)
              js.undefined
            else {
              // TODO Нужен Callback тут вместо голой функции?
              onLocationFound _
            }
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
          // TODO Тут у нас хрень: scalajs-react имеет кучу косяков, в т.ч. не умеет вложенные опциональные компоненты.
          // Зато умеет списки, что вызывает необходимость задания поля props.key.
          state.rcvrMarkers
            .flatMap(_.right.toOption)
            .iterator
            .map { markers =>
              MarkerClusterGroupR(
                markers = markers,
                onMarkerClick = onMarkerClicked _,
                key = "s"   // Чисто для подавления react-warning'а из-за iterator.
              )()
            }
            .toSeq,

          // Опять проблема с Option'ами. Чиним через iterator + key.
          for {
            r    <- state.rcvrPopup.iterator
            resp <- r.resp.right.toOption.iterator
          } yield {
            <.div() // TODO Попап ниже уже обёрнут в diode-react-компонент.
            /*PopupR(position = r.latLng, key = "p")(
              <.div(
                for (g <- resp.groups.iterator) yield {
                  // Значение key не суть важно, просто оно должно быть здесь.
                  val groupId = g.groupId.getOrElse("0")
                  <.div(
                    ^.key := groupId,
                    for (gname <- g.name.toOption) yield {
                      <.h3(gname)
                    },
                    for (n <- g.nodes.iterator) yield {
                      val rcvrKey = RcvrKey(from = r.nodeId, to = n.nodeId, groupId = g.groupId.toOption)
                      val name = n.nameOpt.getOrElse[String]("???")
                      <.div(
                        ^.key := rcvrKey.toString,
                        ^.`class` := Css.Lk.LK_FIELD,
                        // TODO Произвести дедубликацию кода, т.к. обе ветки очень похожи.
                        n.intervalOpt.fold {
                          // Нет текущего размещения. Рендерить активный рабочий чекбокс.
                          val checked = state.rcvrsMap.getOrElse(rcvrKey, n.checked)

                          <.label(
                            ^.classSet1(
                              Css.Lk.LK_FIELD_NAME,
                              Css.Colors.RED    -> (!checked && !n.isCreate),
                              Css.Colors.GREEN  -> (checked  && n.isCreate)
                            ),
                            <.input(
                              ^.`type`    := "checkbox",
                              ^.checked   := checked,
                              ^.onChange ==> rcvrCheckboxChanged(rcvrKey)
                            ),
                            <.span,
                            name
                          )
                        } { ivl =>
                          // Есть какая-то инфа по текущему размещению на данном узле.
                          <.label(
                            ^.classSet1(
                              Css.Lk.LK_FIELD_NAME,
                              Css.Colors.GREEN  -> true
                            ),
                            <.input(
                              ^.`type`    := "checkbox",
                              ^.checked   := true,
                              ^.disabled  := true
                            ),
                            <.span,
                            name,
                            " ",
                            <.span(
                              ^.title := ivl.start.dow,
                              Messages("from._date"), ivl.start.date
                            ),
                            " ",
                            <.span(
                              ^.title := ivl.end.dow,
                              Messages("till._date"), ivl.end.date
                            )
                          )
                        }
                      )
                    }
                  )
                }
              ) // Popup div
            )
            */
          }

        ) // LMap
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


    private val _hiddenInputBase = {
      <.input(
        ^.`type`    := "hidden",
        ^.readOnly  := true
      )
    }

    private def _hiddenInput(i: Int, fn: String, v: String) = {
      _hiddenInputBase(
        ^.name  := Req.RCVR_FN + "[" + i + "]." + fn,
        ^.value := v
      )
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
    .initialState_P { p =>
      State(
        adv4freeConn  = p.connect(_.form.adv4free),
        onMainScrConn = p.connect(_.form.onMainScreen)
      )
    }
    .renderBackend[Backend]
    .componentDidMount( _.backend.componentDidMount )
    .build

  def apply(props: Props) = component(props)

}
