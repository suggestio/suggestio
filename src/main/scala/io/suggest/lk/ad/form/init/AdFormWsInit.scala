package io.suggest.lk.ad.form.init

import io.suggest.sjs.common.controller.IInit
import io.suggest.ad.form.AdFormConstants.WS_ID_INPUT_ID
import org.scalajs.jquery.jQuery

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.08.15 17:16
 * Description: Инициализация websocket для связи с сервером по простому каналу.
 */
trait AdFormWsInit extends IInit {

  /** Запуск инициализации текущего модуля. */
  abstract override def init(): Unit = {
    super.init()
    _initWs()
  }


  /** Инициализация websocket. */
  private def _initWs(): Unit = {
    // Аккуратно извлечь wsId для генерации ws-ссылки. Если есть.
    val jq = jQuery(WS_ID_INPUT_ID)
    Option( jq )
      .flatMap { jqSel => Option( jqSel.`val`() ) }
      .map { _.toString.trim }
      .filter { !_.isEmpty }
      .foreach { wsId =>
        _initWsForId(wsId)
        jq.remove()
      }
  }

  /** Непосредственная инициализация ws. */
  private def _initWsForId(wsId: String): Unit = {
    ???   // TODO JS валяется в adFormBaseTpl.
  }

}
