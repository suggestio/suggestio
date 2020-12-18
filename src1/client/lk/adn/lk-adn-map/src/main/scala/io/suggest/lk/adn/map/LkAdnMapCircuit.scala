package io.suggest.lk.adn.map

import diode.data.Ready
import diode.react.ReactConnector
import io.suggest.adn.mapf.MLamFormInit
import io.suggest.adv.free.MAdv4Free
import io.suggest.lk.adn.map.a._
import io.suggest.lk.adn.map.m._
import io.suggest.lk.adn.map.u.LkAdnMapApiHttpImpl
import io.suggest.lk.adv.a.{Adv4FreeAh, PriceAh}
import io.suggest.lk.adv.m.{MPriceS, ResetPrice}
import io.suggest.maps.c.{MapCommonAh, RadAh, RcvrMarkersInitAh}
import io.suggest.maps.m.MMapS.MMapSFastEq4Map
import io.suggest.maps.m.{MAdvGeoS, MRad, MRadS, RcvrMarkersInit}
import io.suggest.maps.u.{AdvRcvrsMapApiHttpViaUrl, MapsUtil}
import io.suggest.msg.ErrorMsgs
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.log.CircuitLog
import io.suggest.sjs.dt.period.r.DtpAh
import io.suggest.spa.{CircuitUtil, StateInp}
import play.api.libs.json.Json

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.04.17 21:55
  * Description: Diode circuit для формы размещения целого узла на карте мира.
  */
final class LkAdnMapCircuit extends CircuitLog[MRoot] with ReactConnector[MRoot] {

  override protected def CIRCUIT_ERROR_CODE = ErrorMsgs.ADN_MAP_CIRCUIT_ERROR


  /** Собрать начальный инстанс MRoot. */
  override protected def initialModel: MRoot = {
    val stateInp = StateInp.find().get
    val stateJsonStr = stateInp.value.get
    val mFormInit = Json
      .parse( stateJsonStr )
      .as[MLamFormInit]

    MRoot(
      geo = MAdvGeoS(
        mmap = MapsUtil.initialMapStateFrom( mFormInit.form.mapProps ),
        rad = Some( MRad(
          circle = mFormInit.form.mapCursor,
          state = MRadS(
            radiusMarkerCoords = MapsUtil.radiusMarkerLatLng( mFormInit.form.mapCursor )
          ),
        )),
      ),
      conf = mFormInit.conf,

      adv4free = for {
        a4fProps <- mFormInit.adv4FreeProps
      } yield {
        MAdv4Free(
          static  = a4fProps,
          checked = mFormInit.form.adv4freeChecked contains[Boolean] true,
        )
      },
      price = MPriceS(
        resp = Ready( mFormInit.priceResp )
      ),
      datePeriod = mFormInit.form.datePeriod
    )
  }


  /** Собрать статический actionHandler. */
  override protected val actionHandler: HandlerFunction = {

    val httpApi = new LkAdnMapApiHttpImpl

    val confRO = zoom(_.conf)
    val formRO = zoom(_.toForm)

    val nodeIdProxy = confRO.zoom(_.nodeId)
    val geoRW = CircuitUtil.mkLensRootZoomRW( this, MRoot.geo )

    // Поддержка экшенов виджета цены с кнопкой сабмита.
    val priceAh = new PriceAh(
      modelRW = CircuitUtil.mkLensRootZoomRW( this, MRoot.price ),
      priceAskFutF = { () =>
        httpApi.getPriceSubmit( nodeIdProxy(), formRO() )
      },
      doSubmitF = { () =>
        httpApi.forNodeSubmit( nodeIdProxy(), formRO() )
      }
    )

    // Эффект пересчёта стоимости размещения с помощью сервера.
    val priceUpdateEffect = ResetPrice.toEffectPure

    // Обновлять состояние при изменении конфигурации карты.
    val mapCommonAh = new MapCommonAh(
      mmapRW = CircuitUtil.mkLensZoomRW( geoRW, MAdvGeoS.mmap ),
    )

    val radRW = CircuitUtil.mkLensZoomRW( geoRW, MAdvGeoS.rad )
    // Реакция на двиганье маркера на карте:
    val radAh = new RadAh(
      modelRW       = radRW,
      priceUpdateFx = priceUpdateEffect
    )

    val radPopupAh = new LamRadPopupAh(
      modelRW = CircuitUtil.mkLensZoomRW( geoRW, MAdvGeoS.radPopup )
    )

    // Поддержка реакции на adv4free:
    val adv4freeAh = new Adv4FreeAh(
      // Для оптимального подхвата Option[] используем zoomMap:
      modelRW = CircuitUtil.mkLensRootZoomRW( this, MRoot.adv4free ),
      priceUpdateFx = priceUpdateEffect
    )

    val existAdvsRW = CircuitUtil.mkLensZoomRW( geoRW, MAdvGeoS.existAdv )

    val currentGeoAh = new CurrentGeoAh(
      api         = httpApi,
      modelRW     = existAdvsRW.zoomRW(_.geoJson) { _.withGeoJson(_) },
      nodeIdProxy = nodeIdProxy
    )

    val curGeoPopupAh = new CurrentGeoPopupAh(
      api         = httpApi,
      modelRW     = existAdvsRW.zoomRW(_.popup) { _.withPopup(_) }
    )

    // Реакция на события виджета с датой:
    val datePeriodAh = new DtpAh(
      modelRW       = CircuitUtil.mkLensRootZoomRW( this, MRoot.datePeriod ),
      priceUpdateFx = priceUpdateEffect
    )

    val rcvrsRw = CircuitUtil.mkLensRootZoomRW( this, MRoot.rcvrs )

    // Карта покрытия с данными ресиверов:
    val rcvrsMapApi = new AdvRcvrsMapApiHttpViaUrl()
    val rcvrsInitAh = new RcvrMarkersInitAh(
      api     = rcvrsMapApi,
      modelRW = rcvrsRw.zoomRW(_.nodesResp) { _.withNodesResp(_) },
      argsRO  = confRO.zoom(_.rcvrsMap),
    )

    val rcvrMarkerPopupAh = new LamRcvrMarkerPopupAh(
      api     = httpApi,
      rcvrsRW = rcvrsRw
    )

    // Склеить все handler'ы последовательно.
    val conseqAh = composeHandlers(
      radAh,
      currentGeoAh,
      rcvrMarkerPopupAh,
      curGeoPopupAh,
      priceAh,
      datePeriodAh,
      radPopupAh,
      adv4freeAh,
      rcvrsInitAh
    )

    // Параллельно приделать mapCommonAh, который работает с абстрактными сигналами:
    foldHandlers( conseqAh, mapCommonAh )
  }


  // Конструктор: действия, выполняемые в фоне.
  Future {
    // Запустить в фоне инициализацию текущих размещений.
    dispatch(CurrGeoAdvsInit)

    // Запустить инициализацию географической карты ресиверов.
    dispatch(RcvrMarkersInit())
  }

}
