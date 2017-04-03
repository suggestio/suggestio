package io.suggest.lk.adv.geo

import diode.Effect
import diode.data.Ready
import diode.react.ReactConnector
import io.suggest.adv.free.MAdv4Free
import io.suggest.adv.geo.{MFormInit, MFormS}
import io.suggest.bill.MGetPriceResp
import io.suggest.bin.ConvCodecs
import io.suggest.lk.adv.a.{Adv4FreeAh, PriceAh}
import io.suggest.lk.adv.geo.a.geo.exist.{GeoAdvExistInitAh, GeoAdvsPopupAh}
import io.suggest.lk.adv.geo.a.geo.rad.RadAh
import io.suggest.lk.adv.geo.a.mapf.MapCommonAh
import io.suggest.lk.adv.geo.a.pop.NodeInfoPopupAh
import io.suggest.lk.adv.geo.a.rcvr.{RcvrInputsAh, RcvrMarkersInitAh, RcvrsMarkerPopupAh}
import io.suggest.lk.adv.geo.m._
import io.suggest.lk.adv.geo.r.LkAdvGeoApiImpl
import io.suggest.lk.adv.geo.r.oms.OnMainScreenAH
import io.suggest.lk.adv.geo.u.LkAdvGeoFormUtil
import io.suggest.lk.adv.m.{MPriceS, ResetPrice}
import io.suggest.lk.tags.edit.c.TagsEditAh
import io.suggest.lk.tags.edit.m.{MTagsEditState, SetTagSearchQuery}
import io.suggest.pick.PickleUtil
import io.suggest.sjs.common.spa.StateInp
import io.suggest.sjs.dt.period.r.DtpAh
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.sjs.common.msg.ErrorMsgs
import io.suggest.sjs.common.bin.EvoBase64JsUtil.EvoBase64JsDecoder
import MMap.MMapFastEq
import MOther.MOtherFastEq
// TODO import MAdv4Free....FastEq
import MTagsEditState.MTagsEditStateFastEq
import MRcvr.MRcvrFastEq
import MRad.MRadFastEq
import MGeoAdvs.MGeoAdvsFastEq
// TODO import MAdvPeriod...FastEq
import MPopupsS.MPopupsFastEq
import MNodeInfoPopupS.MNodeInfoPopupFastEq
import io.suggest.sjs.common.spa.OptFastEq.Wrapped

import scala.concurrent.Future


/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.12.16 16:45
  * Description: Diode circuit мудрёной формы георазмещения, которая для view используется react.
  */
object LkAdvGeoFormCircuit extends CircuitLog[MRoot] with ReactConnector[MRoot] {

  val API = new LkAdvGeoApiImpl

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
        mmap = MMap(
          props = mFormInit.form.mapProps
        ),
        other = MOther(
          adId = mFormInit.adId,
          price = MPriceS(
            resp = Ready( mFormInit.advPricing )
          )
        ),
        adv4free = for (a4fProps <- mFormInit.adv4FreeProps) yield {
          MAdv4Free(
            static  = a4fProps,
            checked = mFormInit.form.adv4freeChecked.contains(true)   // getOrElse(false)
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
              radiusMarkerCoords = LkAdvGeoFormUtil.radiusMarkerLatLng(radCircle)
            )
          )
        },
        datePeriod = mFormInit.form.datePeriod
      )
    }

    mrootOpt.get
  }


  private val otherRW  = zoomRW(_.other) { _.withOther(_) }
  private val adIdRW = otherRW.zoom(_.adId)
  val mFormDataRO = zoom(_.toFormData)

  private def _apiF2fut[T]( apiF: (String, MFormS) => Future[T] ): Future[T] = {
    val adId = adIdRW.value
    val mFormS = mFormDataRO.value
    apiF(adId, mFormS)
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
    val priceUpdateEffect = Effect.action( ResetPrice )

    val rcvrRW  = zoomRW(_.rcvr) { _.withRcvr(_) }
    val rcvrPopupRW = rcvrRW.zoomRW(_.popupResp) { _.withPopupResp(_) }

    val mmapRW  = zoomRW(_.mmap) { _.withMapState(_) }

    // Собираем handler'ы

    val rcvrsMarkerPopupAh = new RcvrsMarkerPopupAh(
      api       = API,
      adIdProxy = adIdRW,
      rcvrsRW   = rcvrRW
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

    val onMainScreenAh = new OnMainScreenAH(
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
      adIdProxy     = adIdRW,
      existAdvsRW   = geoAdvRW.zoomRW(_.existResp) { _.withExistResp(_) }
    )

    val geoAdvsPopupAh = new GeoAdvsPopupAh(
      api     = API,
      modelRW = geoAdvRW
    )

    val rcvrsMapInitAh = new RcvrMarkersInitAh(
      api       = API,
      adIdProxy = adIdRW,
      modelRW   = rcvrRW.zoomRW(_.markers) { _.withMarkers(_) }
    )

    val radAh = new RadAh(
      modelRW       = zoomRW(_.rad) { _.withRad(_) },
      priceUpdateFx = priceUpdateEffect
    )

    val priceAh = new PriceAh(
      modelRW       = otherRW.zoomRW(_.price) { _.withPriceS(_) },
      priceAskFutF  = priceAskFut,
      doSubmitF     = submitFormFut
    )

    val mPopupsRW = zoomRW(_.popups) { _.withPopups(_) }

    val nodeInfoPopupAh = new NodeInfoPopupAh(
      api     = API,
      modelRW = mPopupsRW.zoomRW(_.nodeInfo) { _.withNodeInfo(_) }
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


  // post-constructor
  {
    // Если задано состояние rcvr-popup'а, то надо запустить в фоне запрос popup'а с сервера.
    val pot = zoom(_.rcvr.popupResp).value
    if (pot.isEmpty && !pot.isPending) {
      for (rcvrPopupState <- zoom(_.rcvr.popupState).value) yield {
        // Есть повод устроить запрос.
        val a = ReqRcvrPopup(
          nodeId    = rcvrPopupState.nodeId,
          geoPoint  = rcvrPopupState.latLng
        )
        dispatch(a)
      }
    }

    // Если задано состояние поиска тегов, запустить запрос поиска тегов на сервер.
    val tagSearchText = zoom(_.tags.props.query).value.text
    if (tagSearchText.nonEmpty) {
      val pot = zoom(_.tags).value.found
      if (pot.isEmpty && !pot.isPending) {
        dispatch( SetTagSearchQuery(tagSearchText) )
      }
    }

  }

}
