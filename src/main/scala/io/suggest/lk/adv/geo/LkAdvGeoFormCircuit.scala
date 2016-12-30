package io.suggest.lk.adv.geo

import diode.Effect
import diode.react.ReactConnector
import io.suggest.adv.geo.MFormS
import io.suggest.adv.geo.MFormS.pickler
import io.suggest.bin.ConvCodecs
import io.suggest.lk.adv.geo.a._
import io.suggest.lk.adv.geo.a.geo.adv.{GeoAdvExistInitAh, GeoAdvsPopupAh}
import io.suggest.lk.adv.geo.m.MRoot
import io.suggest.lk.adv.geo.r.LkAdvGeoApiImpl
import io.suggest.lk.adv.geo.r.mapf.AdvGeoMapCommonAh
import io.suggest.lk.adv.geo.r.oms.OnMainScreenAH
import io.suggest.lk.adv.geo.r.rcvr._
import io.suggest.lk.adv.r.Adv4FreeActionHandler
import io.suggest.lk.tags.edit.c.TagsEditAh
import io.suggest.lk.tags.edit.m.SetTagSearchQuery
import io.suggest.pick.PickleUtil
import io.suggest.sjs.common.spa.StateInp
import io.suggest.sjs.dt.period.r.DtpAh
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.sjs.common.msg.ErrorMsgs
import io.suggest.sjs.common.bin.EvoBase64JsUtil.EvoBase64JsDecoder

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
    val formRW  = zoomRW(_.form) { _.withForm(_) }

    val rcvrRW  = zoomRW(_.rcvr) { _.withRcvr(_) }

    val adIdZoom    = formRW.zoom(_.adId)
    val rcvrPopupRW = rcvrRW.zoomRW(_.popupResp) { _.withPopupResp(_) }

    val mapStateRW  = formRW.zoomRW(_.mapState) { _.withMapState(_) }

    // Собираем handler'ы

    val rcvrsMarkerPopupAh = {
      val p1 = new RcvrsMarkerPopupAH(
        api       = API,
        adIdProxy = adIdZoom,
        rcvrsRW   = rcvrPopupRW // zoomRW(_.rcvrPopup) { _.withRcvrPopup(_) }
      )
      val p3 = new RcvrMarkerPopupState(
        popStateRW = rcvrRW.zoomRW(_.popupState) { _.withPopupState(_) }
      )
      foldHandlers(p1, p3)
    }

    val rcvrInputsAh = new RcvrInputsAH(
      respPot   = rcvrPopupRW,
      rcvrMapRW = rcvrRW.zoomRW(_.rcvrsMap) { _.withRcvrMap(_) }
    )

    val tagsAh = new TagsEditAh(
      modelRW         = zoomRW { _.tags } { _.withTagsEditState(_) },
      api             = API,
      priceUpdateFx   = priceUpdateEffect
    )

    val onMainScreenAh = new OnMainScreenAH(
      modelRW = formRW.zoomRW(_.onMainScreen) { _.withOnMainScreen(_) }
    )

    val mapAh = new AdvGeoMapCommonAh(
      mapStateRW = mapStateRW
    )

    val datePeriodAh = new DtpAh(
      modelRW = zoomRW(_.datePeriod) { _.withDatePeriod(_) },
      priceUpdateFx = priceUpdateEffect
    )

    val adv4freeAh = new Adv4FreeActionHandler(
      modelRW = formRW.zoomMapRW(_.adv4free)(_.checked) { (m, checkedOpt) =>
        m.withAdv4Free(
          for (a4f0 <- m.adv4free; checked2 <- checkedOpt) yield {
            a4f0.withChecked(checked2)
          }
        )
      }
    )

    val geoAdvRW = zoomRW(_.geoAdv) { _.withCurrGeoAdvs(_) }

    val geoAdvsInitAh = new GeoAdvExistInitAh(
      api       = API,
      adIdProxy = adIdZoom,
      existAdvsRW   = geoAdvRW.zoomRW(_.existResp) { _.withExistResp(_) }
    )

    val geoAdvsPopupAh = new GeoAdvsPopupAh(
      api     = API,
      modelRW = geoAdvRW
    )

    val rcvrsMapInitAh = new RcvrMarkersInitAH(
      api       = API,
      adIdProxy = adIdZoom,
      modelRW   = rcvrRW.zoomRW(_.markers) { _.withMarkers(_) }
    )

    // Склеить все handler'ы.
    val h1 = composeHandlers(
      rcvrsMarkerPopupAh, rcvrInputsAh,
      geoAdvsPopupAh,
      tagsAh,
      onMainScreenAh,
      datePeriodAh,
      adv4freeAh,
      // init-вызовы в конце, т.к. они довольно редкие.
      geoAdvsInitAh,
      rcvrsMapInitAh
    )

    // Приклеить common-обработчики, которые вызываются параллельно со всеми остальными
    foldHandlers(h1, mapAh)
  }

  override protected def CIRCUIT_ERROR_CODE = ErrorMsgs.ADV_GEO_FORM_ERROR

  // Остатки конструктора могут вызываться только после выставления actionHandler'а.

  // Запустить получения списка кружочков текущих размещений.
  dispatch( CurrGeoAdvsInit )

  // Запустить инициализацию карты ресиверов после окончания инициализации circuit.
  dispatch( RcvrMarkersInit )

  // Если задано состояние rcvr-popup'а, то надо запустить в фоне запрос popup'а с сервера.
  for {
    rcvrPopupState <- zoom(_.rcvr.popupState).value
    pot = zoom(_.rcvr.popupResp).value
    if pot.isEmpty && !pot.isPending
  } {
    // Есть повод устроить запрос.
    val a = ReqRcvrPopup(
      nodeId    = rcvrPopupState.nodeId,
      geoPoint  = rcvrPopupState.latLng
    )
    dispatch(a)
  }


  // Если задано состояние поиска тегов, запустить запрос поиска тегов на сервер.
  {
    val tagSearchText = zoom(_.tags.props.query).value.text
    if (tagSearchText.nonEmpty) {
      val pot = zoom(_.tags).value.found
      if (pot.isEmpty && !pot.isPending) {
        dispatch( SetTagSearchQuery(tagSearchText) )
      }
    }
  }

}
