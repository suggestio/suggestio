package io.suggest.xadv.ext.js.runner.m

import io.suggest.xadv.ext.js.runner.c.{AppContextImpl, IAppContext}
import org.scalajs.dom.WebSocket

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.03.15 9:35
 * Description: Модель глобального состояния приложения.
 * Модель может стать частично или полностью изменяемой в будущем, в зависимости от потребностей энтерпрайза.
 * Назначение этой модели: инкапсулировать данные приложения, при этом не храня их на виду нигде.
 * Модель обычно живёт в контексте методов и функциях-замыканиях, порожденных этими методами, чтобы запретить
 * любой возможный доступ к данным извне (из других скриптов).
 */
case class MAppState(
  ws        : WebSocket,
  adapters  : List[IAdapter]
) {

  /** Расшаренный глобальный контекст приложения. */
  val appContext: IAppContext = new AppContextImpl

}

