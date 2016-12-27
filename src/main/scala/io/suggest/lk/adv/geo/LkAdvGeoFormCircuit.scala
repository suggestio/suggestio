package io.suggest.lk.adv.geo

import diode.Effect
import diode.react.ReactConnector
import io.suggest.adv.geo.MFormS
import io.suggest.adv.geo.MFormS.pickler
import io.suggest.bin.ConvCodecs
import io.suggest.lk.adv.geo.a.{LetsInitRcvrMarkers, ReqRcvrPopup, SetPrice}
import io.suggest.lk.adv.geo.m.MRoot
import io.suggest.lk.adv.geo.r.LkAdvGeoApiImpl
import io.suggest.lk.adv.geo.r.mapf.AdvGeoMapAH
import io.suggest.lk.adv.geo.r.oms.OnMainScreenAH
import io.suggest.lk.adv.geo.r.rcvr._
import io.suggest.lk.adv.r.Adv4FreeActionHandler
import io.suggest.lk.tags.edit.c.TagsEditAh
import io.suggest.lk.tags.edit.m.{MTagsEditData, SetTagSearchQuery}
import io.suggest.pick.PickleUtil
import io.suggest.sjs.common.spa.StateInp
import io.suggest.sjs.dt.period.r.DtpAh
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.sjs.common.msg.ErrorMsgs
import io.suggest.evothings.util.EvoBase64JsUtil.EvoBase64JsDecoder

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.12.16 16:45
  * Description: Diode circuit мудрёной формы георазмещения, которая для view используется react.
  */
object LkAdvGeoFormCircuit extends CircuitLog[MRoot] with ReactConnector[MRoot] {

  /** Сборка начальной корневой модели. */
  override protected def initialModel: MRoot = {
    // Десериализовывать из base64 из скрытого поля через boopickle + base64.
    val mrootOpt: Option[MRoot] = for {
      stateInp <- StateInp.find()
      base64   <- stateInp.value
    } yield {
      MRoot(
        form = PickleUtil.unpickleConv[String, ConvCodecs.Base64, MFormS](base64)
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

    // Единое RW-представление для props и state подсистемы редактора тегов.
    val tagsEditDataRW = zoomRW { mroot => MTagsEditData(mroot.form.tags, mroot.tagsEditState) } { (mroot, ted) =>
      mroot.copy(
        form          = mroot.form.withTags( ted.props ),
        tagsEditState = ted.state
      )
    }

    val tagsAh = new TagsEditAh(
      modelRW         = tagsEditDataRW,
      api             = API,
      priceUpdateFx   = priceUpdateEffect
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
      tagsAh,
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
  {
    val tagSearchText = zoom(_.form.tags.query).value.text
    if (tagSearchText.nonEmpty) {
      val pot = zoom(_.tagsEditState).value.found
      if (pot.isEmpty && !pot.isPending) {
        dispatch( SetTagSearchQuery(tagSearchText) )
      }
    }
  }

  override protected def CIRCUIT_ERROR_CODE = ErrorMsgs.ADV_GEO_FORM_ERROR

}
