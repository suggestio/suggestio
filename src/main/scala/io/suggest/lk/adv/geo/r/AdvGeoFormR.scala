package io.suggest.lk.adv.geo.r

import io.suggest.adv.geo.AdvGeoConstants
import io.suggest.adv.geo.AdvGeoConstants.AdnNodes.Req
import io.suggest.common.maps.leaflet.LeafletConstants
import io.suggest.css.Css
import io.suggest.lk.adv.geo.m.{IRcvrPopupResp, MMapGjResp, MarkerNodeId}
import io.suggest.lk.adv.geo.tags.m._
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
import io.suggest.sjs.common.model.SjsRoute
import io.suggest.sjs.common.msg.ErrorMsgs
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import react.leaflet.lmap.LMapR
import react.leaflet.popup.PopupR
import io.suggest.sjs.leaflet.L
import io.suggest.sjs.leaflet.event.LocationEvent
import io.suggest.sjs.leaflet.map.{LatLng, Zoom_t}
import io.suggest.sjs.leaflet.marker.{Marker, MarkerEvent}
import react.leaflet.control.LocateControlR
import react.leaflet.layer.TileLayerR
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

  /** Модель пропертисов, приходящая свыше из инициализатора. */
  case class Props(
    adId            : String,
    adv4free        : Option[IAdv4FreeProps]
  ) {

    /** Данные для финального сабмита формы. */
    val submit = SjsRoute.fromJsRouteUsingXhrOpts {
      jsRoutes.controllers.LkAdvGeo.forAdSubmit( adId )
    }

    /** Данные для POST-реквеста с формой с целью запроса стоимости размещения. */
    val price = SjsRoute.fromJsRouteUsingXhrOpts {
      jsRoutes.controllers.LkAdvGeo.getPriceSubmit( adId )
    }

  }


  /** Модель состояния.
    * @param mapCenter Координаты центра карты.
    * @param mapZoom Состояние зума карты.
    * @param rcvrMarkers Маркеры точек-ресиверов для карты ресиверов, полученные с сервера.
    *                    None -- дефолтовое состояние. Почти сразу же заменяется на
    *                    Left(Future[Resp]) -- Запущен XHR за списком маркеров.
    *                    Right([Markers]) -- массив маркеров, передаваемых в MarkerCluster при рендере.
    * @param locationFound false -- значит callback нужен для геолокации.
    *                      true -- геолокация уже была, действия уже были приняты.
    */
  case class State(
    //mapCenter     : LatLng,
    //mapZoom       : Zoom_t,
    onMainScreen  : Boolean = true,
    rcvrMarkers   : Option[Either[Future[_], js.Array[Marker]]] = None,
    locationFound : Boolean = false,
    rcvrPopup     : Option[RcvrPopupState] = None,
    rcvrsMap      : Map[RcvrKey, Boolean] = Map.empty
    //datesPeriodInfo : Option[IDatesPeriodInfo] = None
  ) {
    def withOnMainScreen(oms2: Boolean) = copy(onMainScreen = oms2)
    def withRcvrMarkers(rm: Option[Either[Future[_], js.Array[Marker]]]) = copy(rcvrMarkers = rm)
    def withRcvrPopup(pr: Option[RcvrPopupState]) = copy(rcvrPopup = pr)
    def withRcvrsMap(rm: Map[RcvrKey, Boolean]) = copy(rcvrsMap = rm)
  }


  /** Состояние попапа над ресивером на карте. */
  case class RcvrPopupState(
    nodeId: String,
    latLng: LatLng,
    resp  : Either[Future[_], IRcvrPopupResp]
  )

  /** Ключ в карте текущих ресиверов. */
  case class RcvrKey(from: String, to: String, groupId: Option[String]) {
    override def toString = from + "." + to + "." + groupId.getOrElse("")
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

    /** Реакция на изменение флага узла-ресивера в попапе узла. */
    def rcvrCheckboxChanged(rk: RcvrKey)(e: ReactEventI): Callback = {
      val checked = e.target.checked
      $.modState { s0 =>
        // Состояние попапа уже обязано быть готовым, т.к. данный сигнал другого состояние не подразумевает.
        val ps = s0.rcvrPopup.get
          .resp.right.get
        // Найти узел с текущим id среди всех узлов.
        val checkedOnServerOpt = ps.groups.iterator
          .flatMap(_.nodes)
          .find(_.nodeId == rk.to)
          // Содержит ли описание узла с сервера текущее значение чекбокса? Если да, то значит значение галочки вернулось на исходное.
          .map(_.checked)

        s0.withRcvrsMap(
          if ( checkedOnServerOpt.contains(checked) ) {
            s0.rcvrsMap - rk
          } else {
            s0.rcvrsMap + (rk -> checked)
          }
        )
      }
    }


    /** Рендер всея формы. */
    def render(props: Props, state: State) = {
      <.div(
        ^.`class` := Css.Lk.Adv.FORM_OUTER_DIV,

        // Рендер самой формы...
        <.form(
          ^.method := props.submit.method,
          ^.action := props.submit.url,

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
          ),

          // Отрендерить hidden-input'ы для ресиверов (галочки в попапах на геокарте).
          state.rcvrsMap.nonEmpty ?= <.div(
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
          )

        ),

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
            PopupR(position = r.latLng, key = "p")(
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
    .initialState( State() )
    .renderBackend[Backend]
    .componentDidMount( _.backend.componentDidMount )
    .build

  def apply(props: Props) = component(props)

}
