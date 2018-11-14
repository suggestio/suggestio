package io.suggest.lk.adv.geo

import diode.data.Ready
import diode.react.ReactConnector
import io.suggest.adv.free.MAdv4Free
import io.suggest.adv.geo.{MFormInit, MFormS}
import io.suggest.bill.MGetPriceResp
import io.suggest.bin.ConvCodecs
import io.suggest.lk.adv.a.{Adv4FreeAh, PriceAh, RcvrsMarkerPopupAh}
import io.suggest.lk.adv.geo.a.geo.exist.{GeoAdvExistInitAh, GeoAdvsPopupAh}
import io.suggest.lk.adv.geo.a.pop.NodeInfoPopupAh
import io.suggest.lk.adv.geo.a.rcvr.RcvrInputsAh
import io.suggest.lk.adv.geo.m._
import io.suggest.lk.adv.geo.r.LkAdvGeoHttpApiImpl
import io.suggest.lk.adv.m.{IRcvrPopupProps, MPriceS, ResetPrice}
import io.suggest.lk.tags.edit.c.TagsEditAh
import io.suggest.lk.tags.edit.m.{MTagsEditState, SetTagSearchQuery}
import io.suggest.pick.PickleUtil
import io.suggest.sjs.dt.period.r.DtpAh
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.pick.Base64JsUtil.SjsBase64JsDecoder
import MOther.MOtherFastEq
import io.suggest.lk.adv.geo.a.DocAh
import io.suggest.lk.adv.geo.a.oms.OnMainScreenAh
import io.suggest.maps.c.{MapCommonAh, RadAh, RcvrMarkersInitAh}
import io.suggest.maps.m._
import io.suggest.maps.u.{AdvRcvrsMapApiHttpViaUrl, IAdvRcvrsMapApi, MapsUtil}
import io.suggest.msg.ErrorMsgs
import io.suggest.spa.StateInp
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
// TODO import MAdv4Free....FastEq
import MTagsEditState.MTagsEditStateFastEq
import MRcvr.MRcvrFastEq
import MRad.MRadFastEq
import io.suggest.maps.m.MExistGeoS.MExistGeoSFastEq
// TODO import MAdvPeriod...FastEq
import MPopupsS.MPopupsFastEq
import MNodeInfoPopupS.MNodeInfoPopupFastEq
import io.suggest.spa.OptFastEq.Wrapped
import io.suggest.common.empty.OptionUtil.BoolOptOps

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
      val mFormInit = PickleUtil.unpickleConv[String, ConvCodecs.Base64, MFormInit](base64)
      // Собираем начальный инстанс MRoot на основе данных, переданных с сервера...
      MRoot(
        mmap = MapsUtil.initialMapStateFrom( mFormInit.form.mapProps ),
        other = MOther(
          adId     = mFormInit.adId,
          rcvrsMap = mFormInit.rcvrsMap
        ),
        adv4free = for (a4fProps <- mFormInit.adv4FreeProps) yield {
          MAdv4Free(
            static  = a4fProps,
            checked = mFormInit.form.adv4freeChecked.getOrElseFalse
          )
        },
        tags = MTagsEditState(
          props = mFormInit.form.tagsEdit
        ),
        rcvr = MRcvr(
          rcvrsMap = mFormInit.form.rcvrsMap
        ),
        rad  = for (radCircle <- mFormInit.form.radCircle) yield {
          MRad(
            circle = radCircle,
            state  = MRadS(
              radiusMarkerCoords = MapsUtil.radiusMarkerLatLng(radCircle)
            )
          )
        },
        datePeriod = mFormInit.form.datePeriod,
        bill = MBillS(
          price = MPriceS(
            resp = Ready( mFormInit.advPricing )
          )
        )
      )
    }

    mrootOpt.get
  }


  private val otherRW  = zoomRW(_.other) { _.withOther(_) }

  private val API = new LkAdvGeoHttpApiImpl(otherRW)

  private val mFormDataRO = zoom(_.toFormData)

  private def _apiF2fut[T]( apiF: MFormS => Future[T] ): Future[T] = {
    val mFormS = mFormDataRO.value
    apiF( mFormS )
  }

  /** Функция запуска запроса на сервер для перерасчёта ценника. */
  private def priceAskFut(): Future[MGetPriceResp] = {
    _apiF2fut( API.getPrice )
  }

  private def submitFormFut(): Future[String] = {
    _apiF2fut( API.formSubmit )
  }


  /** Обработчики экшенов объединяются прямо здесь: */
  override protected val actionHandler: HandlerFunction = {

    // Эффект пересчёта стоимости размещения с помощью сервера.
    val priceUpdateEffect = ResetPrice.toEffectPure

    val rcvrRW  = zoomRW(_.rcvr) { _.withRcvr(_) }
    val rcvrPopupRW = rcvrRW.zoomRW(_.popupResp) { _.withPopupResp(_) }

    val mmapRW  = zoomRW(_.mmap) { _.withMapState(_) }

    // Собираем handler'ы

    val rcvrsMarkerPopupAh = new RcvrsMarkerPopupAh(
      api       = API,
      rcvrsRW   = rcvrRW.zoomRW[IRcvrPopupProps](identity) { _.withIrpp(_) }
    )

    val rcvrInputsAh = new RcvrInputsAh(
      respPot       = rcvrPopupRW,
      rcvrMapRW     = rcvrRW.zoomRW(_.rcvrsMap) { _.withRcvrMap(_) },
      priceUpdateFx = priceUpdateEffect
    )

    val tagsAh = new TagsEditAh(
      modelRW         = zoomRW(_.tags) { _.withTagsEditState(_) },
      api             = API,
      priceUpdateFx   = priceUpdateEffect
    )

    val onMainScreenAh = new OnMainScreenAh(
      priceUpdateFx   = priceUpdateEffect,
      modelRW = otherRW.zoomRW(_.onMainScreen) { _.withOnMainScreen(_) }
    )

    val mapAh = new MapCommonAh(
      mmapRW = mmapRW
    )

    val datePeriodAh = new DtpAh(
      modelRW = zoomRW(_.datePeriod) { _.withDatePeriod(_) },
      priceUpdateFx = priceUpdateEffect
    )

    val adv4freeAh = new Adv4FreeAh(
      // Для оптимального подхвата Option[] используем zoomMap:
      modelRW = zoomMapRW(_.adv4free)(_.checked) { (m, checkedOpt) =>
        m.withAdv4Free(
          for (a4f0 <- m.adv4free; checked2 <- checkedOpt) yield {
            a4f0.withChecked(checked2)
          }
        )
      },
      priceUpdateFx = priceUpdateEffect
    )

    val geoAdvRW = zoomRW(_.geoAdv) { _.withCurrGeoAdvs(_) }

    val geoAdvsInitAh = new GeoAdvExistInitAh(
      api           = API,
      existAdvsRW   = geoAdvRW.zoomRW(_.geoJson) { _.withGeoJson(_) }
    )

    val geoAdvsPopupAh = new GeoAdvsPopupAh(
      api     = API,
      modelRW = geoAdvRW.zoomRW(_.popup) { _.withPopup(_) }
    )

    val advRcvrsMapApi = new AdvRcvrsMapApiHttpViaUrl(
      route = { () =>
        IAdvRcvrsMapApi.rcvrsMapRouteFromArgs( otherRW.value.rcvrsMap )
      }
    )
    val rcvrsMapInitAh = new RcvrMarkersInitAh(
      api       = advRcvrsMapApi,
      modelRW   = rcvrRW.zoomRW(_.rcvrsGeo) { _.withRcvrsGeo(_) }
    )

    val radAh = new RadAh(
      modelRW       = zoomRW(_.rad) { _.withRad(_) },
      priceUpdateFx = priceUpdateEffect
    )

    val billRW = zoomRW(_.bill) { _.withBill(_) }

    val priceAh = new PriceAh(
      modelRW       = billRW.zoomRW(_.price) { _.withPrice(_) },
      priceAskFutF  = priceAskFut,
      doSubmitF     = submitFormFut
    )

    val mPopupsRW = zoomRW(_.popups) { _.withPopups(_) }

    val nodeInfoPopupAh = new NodeInfoPopupAh(
      api     = API,
      modelRW = mPopupsRW.zoomRW(_.nodeInfo) { _.withNodeInfo(_) }
    )

    val docAh = new DocAh(
      modelRW = otherRW.zoomRW(_.doc) { _.withDoc(_) }
    )

    // Склеить все handler'ы.
    val h1 = composeHandlers(
      // Основные элементы формы, в т.ч. leaflet-попапы:
      radAh,
      priceAh,
      rcvrsMarkerPopupAh, rcvrInputsAh,
      geoAdvsPopupAh,
      tagsAh,
      onMainScreenAh,
      datePeriodAh,
      adv4freeAh,
      // lk-попапы (вне leaflet):
      nodeInfoPopupAh,
      // init-вызовы в конце, т.к. они довольно редкие.
      geoAdvsInitAh,
      rcvrsMapInitAh,
      // Просто очень редкие вызовы:
      docAh
    )

    // Приклеить common-обработчики, которые вызываются параллельно со всеми остальными
    foldHandlers(h1, mapAh)
  }

  override protected def CIRCUIT_ERROR_CODE = ErrorMsgs.ADV_GEO_FORM_ERROR

  // Остатки конструктора могут вызываться только после выставления actionHandler'а.

  // Запустить получения списка кружочков текущих размещений.
  Future {
    dispatch( CurrGeoAdvsInit )
  }

  // Запустить инициализацию карты ресиверов после окончания инициализации circuit.
  Future {
    dispatch(RcvrMarkersInit)
  }

  // Если задано состояние rcvr-popup'а, то надо запустить в фоне запрос popup'а с сервера.
  Future {
    val pot = zoom(_.rcvr.popupResp).value
    if (pot.isEmpty && !pot.isPending) {
      for (rcvrPopupState <- zoom(_.rcvr.popupState).value) yield {
        // Есть повод устроить запрос.
        val a = OpenMapRcvr(
          nodeId    = rcvrPopupState.nodeId,
          geoPoint  = rcvrPopupState.latLng
        )
        dispatch(a)
      }
    }
  }

  // Если задано состояние поиска тегов, запустить запрос поиска тегов на сервер.
  Future {
    val tagSearchText = zoom(_.tags.props.query).value.text
    if (tagSearchText.nonEmpty) {
      val pot = zoom(_.tags).value.found
      if (pot.isEmpty && !pot.isPending) {
        dispatch( SetTagSearchQuery(tagSearchText) )
      }
    }
  }

}
