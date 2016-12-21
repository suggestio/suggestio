package io.suggest.lk.adv.geo

import diode.{Circuit, Effect}
import diode.react.ReactConnector
import evothings.EvothingsUtil
import io.suggest.adv.geo.MFormS
import io.suggest.lk.adv.geo.a.{LetsInitRcvrMarkers, ReqRcvrPopup, SetPrice}
import io.suggest.lk.adv.geo.m.MRoot
import io.suggest.lk.adv.geo.r.LkAdvGeoApiImpl
import io.suggest.lk.adv.geo.r.mapf.AdvGeoMapAH
import io.suggest.lk.adv.geo.r.oms.OnMainScreenAH
import io.suggest.lk.adv.geo.r.rcvr._
import io.suggest.lk.adv.r.Adv4FreeActionHandler
import io.suggest.lk.tags.edit.r.TagsEditActionHandler
import io.suggest.sjs.common.spa.StateInp
import io.suggest.sjs.dt.period.r.DtpAh
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.concurrent.Future
import scala.scalajs.js.typedarray.TypedArrayBuffer

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.12.16 16:45
  * Description: Diode circuit мудрёной формы георазмещения, которая для view используется react.
  */
object LkAdvGeoFormCircuit extends Circuit[MRoot] with ReactConnector[MRoot] {

  /** Сборка начальной корневой модели. */
  override protected def initialModel: MRoot = {
    // Десериализовывать из base64 из скрытого поля через boopickle + base64.
    val mrootOpt: Option[MRoot] = for {
      stateInp <- StateInp.find()
      base64   <- stateInp.value
    } yield {
      MRoot(
        form = {
          val arr = EvothingsUtil.base64DecToArr(base64)
          val buf = TypedArrayBuffer.wrap(arr.buffer)
          MFormS.unPickler.fromBytes( buf )
        }
      )
    }

    mrootOpt.get
  }

  /** Эффект пересчёта стоимости размещения с помощью сервера. */
  private val priceUpdateEffect: Effect = {
    Effect {
      // TODO Реализовать запрос к серверу и получения ответа с ценником.
      Future.successful( SetPrice("TODO") )
    }
  }

  val API = new LkAdvGeoApiImpl

  /** Обработчики экшенов объединяются прямо здесь: */
  override protected val actionHandler: HandlerFunction = {

    // RW-доступ к полю с формой.
    val formZoomRW  = zoomRW(_.form) { _.withForm(_) }

    val adIdZoom    = formZoomRW.zoom(_.adId)
    val rcvrPopupRW = zoomRW(_.rcvrPopup) { _.withRcvrPopup(_) }

    val mapStateRW  = formZoomRW.zoomRW(_.mapState) { _.withMapState(_) }

    // Собираем handler'ы

    val rcvrsMarkerPopupAh = {
      val p1 = new RcvrsMarkerPopupAH(
        api       = API,
        adIdProxy = adIdZoom,
        rcvrsRW   = rcvrPopupRW // zoomRW(_.rcvrPopup) { _.withRcvrPopup(_) }
      )
      val p2 = new RcvrMarkerOnMapAH(
        mapStateRW = mapStateRW
      )
      val p3 = new RcvrMarkerPopupState(
        popStateRW = formZoomRW.zoomRW(_.rcvrPopup) { _.withRcvrPopup(_) }
      )
      foldHandlers(p1, p2, p3)
    }

    val rcvrInputsAh = new RcvrInputsAH(
      respPot   = rcvrPopupRW,
      rcvrMapRW = formZoomRW.zoomRW(_.rcvrsMap) { _.withRcvrMap(_) }
    )

    val tagsEditAh = new TagsEditActionHandler(
      modelRW       = formZoomRW.zoomRW(_.tags) { _.withTags(_) },
      priceUpdateFx = priceUpdateEffect
    )

    val onMainScreenAh = new OnMainScreenAH(
      modelRW = formZoomRW.zoomRW(_.onMainScreen) { _.withOnMainScreen(_) }
    )

    val mapAh = new AdvGeoMapAH(
      mapStateRW = mapStateRW
    )

    val datePeriodAh = new DtpAh(
      modelRW = formZoomRW.zoomRW(_.datePeriod) { _.withDatePeriod(_) },
      priceUpdateFx = priceUpdateEffect
    )

    val adv4freeAh = new Adv4FreeActionHandler(
      modelRW = formZoomRW.zoomMapRW(_.adv4free)(_.checked) { (m, checkedOpt) =>
        m.withAdv4Free(
          for (a4f0 <- m.adv4free; checked2 <- checkedOpt) yield {
            a4f0.withChecked(checked2)
          }
        )
      }
    )

    val rcvrsMapInitAh = new RcvrMarkersInitAH(
      api       = API,
      adIdProxy = adIdZoom,
      modelRW   = zoomRW(_.rcvrMarkers) { _.withRcvrMarkers(_) }
    )

    // Склеить все handler'ы.
    composeHandlers(
      rcvrsMarkerPopupAh, rcvrInputsAh,
      tagsEditAh,
      onMainScreenAh,
      mapAh,
      datePeriodAh,
      adv4freeAh,
      // init-вызовы в конце, т.к. они довольно редкие.
      rcvrsMapInitAh
    )
  }


  // Запустить инициализацию карты ресиверов после окончания инициализации circuit.
  dispatch( LetsInitRcvrMarkers )

  // Если задано состояние rcvr-popup'а, то надо запустить в фоне запрос popup'а с сервера.
  for {
    rcvrPopupState <- zoom(_.form.rcvrPopup).value
    pot = zoom(_.rcvrPopup).value
    if pot.isEmpty && !pot.isPending
  } {
    // Есть повод устроить запрос.
    val a = ReqRcvrPopup(
      nodeId    = rcvrPopupState.nodeId,
      geoPoint  = rcvrPopupState.latLng
    )
    dispatch(a)
  }


  // TODO Если задано состояние поиска тегов, запустить запрос поиска тегов на сервер.
  /*{
    val tagSearch = zoom(_.form.tags.query).value
    if (tagSearch.text.nonEmpty) {
      val pot = zoom(_.tagsFound).value
      if (pot.isEmpty && !pot.isPending) {
        dispatch( TagsFoundR )
      }
    }
  }*/

}
