package io.suggest

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.07.17 22:02
  */
package object wxm {

  /** Тип уникального идентификатора реквеста на сервер по WebSocket.
    * Уникальность должна быть обеспечена только в рамках одной сессии общения в некотором коротком интервале времени.
    *
    * На клиенте по идее достаточно просто Int (или Short) с инкрементом (т.к. js однопоточный и FSM по своей сути тоже).
    */
  type WxmMsgId_t = Int

}
