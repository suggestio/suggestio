package io.suggest.lk.adn.map

import diode.data.Ready
import diode.react.ReactConnector
import io.suggest.adn.mapf.MLamFormInit
import io.suggest.adv.free.MAdv4Free
import io.suggest.bin.ConvCodecs
import io.suggest.lk.adn.map.a.{LamOptsAh, LamRadAh}
import io.suggest.lk.adn.map.m._
import io.suggest.lk.adn.map.u.LkAdnMapApiHttpImpl
import io.suggest.lk.adv.a.{Adv4FreeAh, PriceAh}
import io.suggest.lk.adv.m.{MPriceS, ResetPrice}
import io.suggest.maps.c.MapCommonAh
import io.suggest.maps.m.{MMapS, MRadS}
import io.suggest.maps.u.MapsUtil
import io.suggest.pick.PickleUtil
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.sjs.common.msg.ErrorMsgs
import io.suggest.sjs.common.spa.StateInp
import io.suggest.sjs.dt.period.r.DtpAh
import io.suggest.sjs.common.bin.Base64JsUtil.SjsBase64JsDecoder

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
      mmap = MMapS(
        props = mFormInit.form.mapProps
      ),
      conf = MLamConf(
        nodeId = mFormInit.nodeId
      ),
      rad = MLamRad(
        circle = mFormInit.form.mapCursor,
        state = MRadS(
          radiusMarkerCoords = MapsUtil.radiusMarkerLatLng( mFormInit.form.mapCursor )
        )
      ),
      opts = mFormInit.form.opts,
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

    // Поддержка экшенов виджета цены с кнопкой сабмита.
    val priceAh = new PriceAh(
      modelRW = zoomRW(_.price) { _.withPrice(_) },
      priceAskFutF = { () =>
        httpApi.getPriceSubmit( confRO().nodeId, formRO() )
      },
      doSubmitF = { () =>
        httpApi.forNodeSubmit( confRO().nodeId, formRO() )
      }
    )

    // Эффект пересчёта стоимости размещения с помощью сервера.
    val priceUpdateEffect = ResetPrice.effect

    // Обновлять состояние при изменении конфигурации карты.
    val mapCommonAh = new MapCommonAh(
      mmapRW = zoomRW(_.mmap) { _.withMap(_) }
    )

    // Реакция на двиганье маркера на карте:
    val radAh = new LamRadAh(
      modelRW       = zoomRW [IRadOpts[MRoot]] (identity) { _.withRadOpts(_) },
      priceUpdateFx = priceUpdateEffect
    )

    // Контроллер галочек опций.
    val optsAh = new LamOptsAh(
      modelRW       = zoomRW(_.opts) { _.withOpts(_) },
      priceUpdateFx = priceUpdateEffect
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

    // Реакция на события виджета с датой:
    val datePeriodAh = new DtpAh(
      modelRW       = zoomRW(_.datePeriod) { _.withDatePeriod(_) },
      priceUpdateFx = priceUpdateEffect
    )

    // Склеить все handler'ы последовательно.
    val conseqAh = composeHandlers(
      radAh,
      priceAh,
      datePeriodAh,
      optsAh,
      adv4freeAh
    )

    // Параллельно приделать mapCommonAh, который работает с абстрактными сигналами:
    foldHandlers( conseqAh, mapCommonAh )
  }

}
