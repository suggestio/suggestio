package io.suggest.lk.adv.geo

import diode.data.Ready
import diode.react.ReactConnector
import io.suggest.adv.free.MAdv4Free
import io.suggest.adv.geo.{AdvGeoConstants, MFormInit, MFormS}
import io.suggest.bill.MGetPriceResp
import io.suggest.lk.adv.a.{Adv4FreeAh, PriceAh, RcvrsMarkerPopupAh}
import io.suggest.lk.adv.geo.a.geo.exist.{GeoAdvExistInitAh, GeoAdvsPopupAh}
import io.suggest.lk.adv.geo.a.pop.NodeInfoPopupAh
import io.suggest.lk.adv.geo.a.rcvr.RcvrInputsAh
import io.suggest.lk.adv.geo.m._
import io.suggest.lk.adv.geo.r.LkAdvGeoHttpApiImpl
import io.suggest.lk.adv.m.{IRcvrPopupProps, MPriceS, ResetPrice}
import io.suggest.lk.tags.edit.c.TagsEditAh
import io.suggest.lk.tags.edit.m.{MTagsEditState, SetTagSearchQuery}
import io.suggest.sjs.dt.period.r.DtpAh
import io.suggest.log.CircuitLog
import MOther.MOtherFastEq
import io.suggest.lk.adv.geo.a.DocAh
import io.suggest.lk.adv.geo.a.oms.OnMainScreenAh
import io.suggest.maps.c.{MapCommonAh, RadAh, RadPopupAh, RcvrMarkersInitAh}
import io.suggest.maps.m._
import io.suggest.maps.u.{AdvRcvrsMapApiHttpViaUrl, MapsUtil}
import io.suggest.msg.ErrorMsgs
import io.suggest.spa.{CircuitUtil, StateInp}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import play.api.libs.json.Json
// TODO import MAdv4Free....FastEq
import MTagsEditState.MTagsEditStateFastEq
import MRcvr.MRcvrFastEq
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
final class LkAdvGeoFormCircuit extends CircuitLog[MRoot] with ReactConnector[MRoot] {

  /** Сборка начальной корневой модели. */
  override protected def initialModel: MRoot = {
    val mrootOpt: Option[MRoot] = for {
      stateInp <- StateInp.find()
      stateJsonStr <- stateInp.value
    } yield {
      val mFormInit = Json
        .parse( stateJsonStr )
        .as[MFormInit]
      // Собираем начальный инстанс MRoot на основе данных, переданных с сервера...
      MRoot(
        geo = MAdvGeoS(
          mmap = MapsUtil.initialMapStateFrom( mFormInit.form.mapProps ),
          rad  = for (radCircle <- mFormInit.form.radCircle) yield {
            MRad(
              circle = radCircle,
              state  = MRadS(
                radiusMarkerCoords = MapsUtil.radiusMarkerLatLng(radCircle)
              ),
              enabled = mFormInit.radEnabled,
            )
          },
        ),
        other = MOther(
          adId     = mFormInit.adId,
          rcvrsMap = mFormInit.rcvrsMap
        ),
        adv = MAdvS(
          free = for (a4fProps <- mFormInit.adv4FreeProps) yield {
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
          datePeriod = mFormInit.form.datePeriod,
          bill = MBillS(
            price = MPriceS(
              resp = Ready( mFormInit.advPricing )
            )
          ),
        ),
      )
    }

    mrootOpt.get
  }


  private val otherRW = CircuitUtil.mkLensRootZoomRW( this, MRoot.other )( MOther.MOtherFastEq )

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
    _apiF2fut( API.forAdSubmit )
  }


  /** Обработчики экшенов объединяются прямо здесь: */
  override protected val actionHandler: HandlerFunction = {

    // Эффект пересчёта стоимости размещения с помощью сервера.
    val priceUpdateEffect = ResetPrice.toEffectPure

    val advRW = CircuitUtil.mkLensRootZoomRW( this, MRoot.adv )
    val rcvrRW = CircuitUtil.mkLensZoomRW( advRW, MAdvS.rcvr )
    val rcvrPopupRW = rcvrRW.zoomRW(_.popupResp) { _.withPopupResp(_) }

    val geoRW = CircuitUtil.mkLensRootZoomRW( this, MRoot.geo )
    val mmapRW = CircuitUtil.mkLensZoomRW( geoRW, MAdvGeoS.mmap )

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
      modelRW         = CircuitUtil.mkLensZoomRW( advRW, MAdvS.tags ),
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
      modelRW = CircuitUtil.mkLensZoomRW( advRW, MAdvS.datePeriod ),
      priceUpdateFx = priceUpdateEffect
    )

    val adv4freeAh = new Adv4FreeAh(
      // Для оптимального подхвата Option[] используем zoomMap:
      modelRW = CircuitUtil.mkLensZoomRW( advRW, MAdvS.free ),
      priceUpdateFx = priceUpdateEffect
    )

    val existAdvRW = CircuitUtil.mkLensZoomRW( geoRW, MAdvGeoS.existAdv )( MExistGeoS.MExistGeoSFastEq )

    val geoAdvsInitAh = new GeoAdvExistInitAh(
      api           = API,
      existAdvsRW   = existAdvRW.zoomRW(_.geoJson) { _.withGeoJson(_) }
    )

    val geoAdvsPopupAh = new GeoAdvsPopupAh(
      api     = API,
      modelRW = existAdvRW.zoomRW(_.popup) { _.withPopup(_) }
    )

    val advRcvrsMapApi = new AdvRcvrsMapApiHttpViaUrl()
    val rcvrsMapInitAh = new RcvrMarkersInitAh(
      api       = advRcvrsMapApi,
      modelRW   = rcvrRW.zoomRW(_.rcvrsGeo) { _.withRcvrsGeo(_) },
      argsRO    = otherRW.zoom(_.rcvrsMap),
    )

    val radAh = new RadAh(
      modelRW       = CircuitUtil.mkLensZoomRW( geoRW, MAdvGeoS.rad ),
      priceUpdateFx = priceUpdateEffect,
      radiusMinMax  = AdvGeoConstants.Radius,
    )
    val radPopupAh = new RadPopupAh(
      modelRW = CircuitUtil.mkLensZoomRW( geoRW, MAdvGeoS.radPopup ),
    )

    val billRW = CircuitUtil.mkLensZoomRW( advRW, MAdvS.bill )

    val priceAh = new PriceAh(
      modelRW       = billRW.zoomRW(_.price) { _.withPrice(_) },
      priceAskFutF  = priceAskFut,
      doSubmitF     = submitFormFut
    )

    val mPopupsRW = CircuitUtil.mkLensRootZoomRW( this, MRoot.popups )

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
      // lk-попапы (вне leaflet):
      nodeInfoPopupAh,
      radPopupAh,
      adv4freeAh,
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
    dispatch(RcvrMarkersInit())
  }

  // Если задано состояние rcvr-popup'а, то надо запустить в фоне запрос popup'а с сервера.
  Future {
    val pot = zoom(_.adv.rcvr.popupResp).value
    if (pot.isEmpty && !pot.isPending) {
      for (rcvrPopupState <- zoom(_.adv.rcvr.popupState).value) yield {
        // Есть повод устроить запрос.
        val a = OpenMapRcvr(
          nodeId    = rcvrPopupState.nodeId,
          geoPoint  = rcvrPopupState.geoPoint
        )
        dispatch(a)
      }
    }
  }

  // Если задано состояние поиска тегов, запустить запрос поиска тегов на сервер.
  Future {
    val tagSearchText = zoom(_.adv.tags.props.query).value.text
    if (tagSearchText.nonEmpty) {
      val pot = zoom(_.adv.tags).value.found
      if (pot.isEmpty && !pot.isPending) {
        dispatch( SetTagSearchQuery(tagSearchText) )
      }
    }
  }

  //addProcessor( io.suggest.spa.LoggingAllActionsProcessor[MRoot] )

}
