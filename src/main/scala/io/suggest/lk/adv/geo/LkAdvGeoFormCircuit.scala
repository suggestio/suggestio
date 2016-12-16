package io.suggest.lk.adv.geo

import diode.Circuit
import diode.react.ReactConnector
import evothings.EvothingsUtil
import io.suggest.adv.geo.MFormS
import io.suggest.lk.adv.geo.m.MRoot
import io.suggest.lk.adv.geo.r.oms.OnMainScreenActionHandler
import io.suggest.lk.adv.geo.r.pop.rcvr.RcvrPopupActionHandler
import io.suggest.lk.adv.r.Adv4FreeActionHandler
import io.suggest.lk.tags.edit.r.TagsEditActionHandler
import io.suggest.sjs.common.spa.StateInp

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
        // TODO Запустить в фоне экшен проверки необходимости запуска ранних эффектов запроса ресиверов с сервера.
        // Например, если задано root.form.rcvrState, а root.rcvrResp == Pot.Empty, но нужно стартануть эффект получения данных с сервера.
      )
    }

    mrootOpt.get
  }


  /** RW-доступ к полю с формой. */
  private val formZoomRW = zoomRW(_.form) { _.withForm(_) }

  /** Обработчики экшенов объединяются прямо здесь: */
  override protected val actionHandler: HandlerFunction = {

    val rcvrPopupAh = new RcvrPopupActionHandler(
      respPot = zoom(_.rcvrPopup),
      rcvrMapRW = formZoomRW.zoomRW(_.rcvrsMap) { _.withRcvrMap(_) }
    )

    val tagsEditAh = new TagsEditActionHandler(
      modelRW = formZoomRW.zoomRW(_.tags) { _.withTags(_) }
    )

    val onMainScreenAh = new OnMainScreenActionHandler(
      modelRW = formZoomRW.zoomRW(_.onMainScreen) { _.withOnMainScreen(_) }
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

    composeHandlers(rcvrPopupAh, tagsEditAh, onMainScreenAh, adv4freeAh)
  }

}
