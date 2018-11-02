package io.suggest.lk.adn.map

import diode.Effect
import diode.data.Ready
import diode.react.ReactConnector
import io.suggest.adn.mapf.MLamFormInit
import io.suggest.adv.free.MAdv4Free
import io.suggest.bin.ConvCodecs
import io.suggest.lk.adn.map.a._
import io.suggest.lk.adn.map.m.IRadOpts.IRadOptsFastEq
import io.suggest.lk.adn.map.m.MLamRad.MLamRadFastEq
import io.suggest.lk.adn.map.m._
import io.suggest.lk.adn.map.u.LkAdnMapApiHttpImpl
import io.suggest.lk.adv.a.{Adv4FreeAh, PriceAh}
import io.suggest.lk.adv.m.{MPriceS, ResetPrice}
import io.suggest.maps.c.{MapCommonAh, RcvrMarkersInitAh}
import io.suggest.maps.m.MMapS.MMapSFastEq4Map
import io.suggest.maps.m.{MRadS, RcvrMarkersInit}
import io.suggest.maps.u.MapsUtil
import io.suggest.msg.ErrorMsgs
import io.suggest.pick.Base64JsUtil.SjsBase64JsDecoder
import io.suggest.pick.PickleUtil
import io.suggest.routes.AdvRcvrsMapApiHttpViaUrl
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.sjs.dt.period.r.DtpAh
import io.suggest.spa.StateInp

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.04.17 21:55
  * Description: Diode circuit для формы размещения целого узла на карте мира.
  */
class LkAdnMapCircuit extends CircuitLog[MRoot] with ReactConnector[MRoot] {

  override protected def CIRCUIT_ERROR_CODE = ErrorMsgs.ADN_MAP_CIRCUIT_ERROR


  /** Собрать начальный инстанс MRoot. */
  override protected def initialModel: MRoot = {
    val stateInp = StateInp.find().get
    val base64 = stateInp.value.get
    val mFormInit = PickleUtil.unpickleConv[String, ConvCodecs.Base64, MLamFormInit](base64)

    MRoot(
      mmap = MapsUtil.initialMapStateFrom( mFormInit.form.mapProps ),
      conf = mFormInit.conf,
      rad = MLamRad(
        circle = mFormInit.form.mapCursor,
        state = MRadS(
          radiusMarkerCoords = MapsUtil.radiusMarkerLatLng( mFormInit.form.mapCursor )
        )
      ),
      adv4free = for {
        a4fProps <- mFormInit.adv4FreeProps
      } yield {
        MAdv4Free(
          static  = a4fProps,
          checked = mFormInit.form.adv4freeChecked.contains(true)
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

    // Поддержка экшенов виджета цены с кнопкой сабмита.
    val priceAh = new PriceAh(
      modelRW = zoomRW(_.price) { _.withPrice(_) },
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
      mmapRW = zoomRW(_.mmap) { _.withMap(_) }
    )

    val radOptsRW = zoomRW [IRadOpts[MRoot]] (identity) { _.withRadOpts(_) }
    // Реакция на двиганье маркера на карте:
    val radAh = new LamRadAh(
      modelRW       = radOptsRW,
      priceUpdateFx = priceUpdateEffect
    )

    val radRw = radOptsRW.zoomRW(_.rad) { _.withRad(_) }

    val radPopupAh = new LamRadPopupAh(
      modelRW = radRw.zoomRW(_.popup) { _.withPopup(_) }
    )

    // Поддержка реакции на adv4free:
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

    val currentRW = zoomRW(_.current) { _.withCurrent(_) }

    val currentGeoAh = new CurrentGeoAh(
      api         = httpApi,
      modelRW     = currentRW.zoomRW(_.geoJson) { _.withGeoJson(_) },
      nodeIdProxy = nodeIdProxy
    )

    val curGeoPopupAh = new CurrentGeoPopupAh(
      api         = httpApi,
      modelRW     = currentRW.zoomRW(_.popup) { _.withPopup(_) }
    )

    // Реакция на события виджета с датой:
    val datePeriodAh = new DtpAh(
      modelRW       = zoomRW(_.datePeriod) { _.withDatePeriod(_) },
      priceUpdateFx = priceUpdateEffect
    )

    val rcvrsRw = zoomRW(_.rcvrs) { _.withRcvrs(_) }

    // Карта покрытия с данными ресиверов:
    val rcvrsMapApi = new AdvRcvrsMapApiHttpViaUrl( confRO.value.rcvrsMapUrl )
    val rcvrsInitAh = new RcvrMarkersInitAh(
      api     = rcvrsMapApi,
      modelRW = rcvrsRw.zoomRW(_.nodesResp) { _.withNodesResp(_) }
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
    dispatch(RcvrMarkersInit)
  }

}
