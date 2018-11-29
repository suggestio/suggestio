package io.suggest.sc.sw.m

import io.suggest.spa.DAction
import org.scalajs.dom.experimental.serviceworkers.{FetchEvent, ServiceWorkerMessageEvent}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.11.18 21:26
  * Description: Экшены FSM ServiceWorker'а выдачи.
  */
sealed trait ScSwAction extends DAction

/** Экшен прихода сообщения.  */
case class HandleMessage(event: ServiceWorkerMessageEvent ) extends ScSwAction

/** Fetch-событие. */
case class HandleFetch(event: FetchEvent) extends ScSwAction
