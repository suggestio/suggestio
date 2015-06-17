package io.suggest.sc.sjs.vm.foc.fsm.msg

import io.suggest.sc.sjs.m.msrv.foc.find.IMFocAds

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.06.15 14:51
 * Description: Сообщения для посылания в focused fsm.
 */
trait IFsmMessage

case object Next extends IFsmMessage
case object Prev extends IFsmMessage
case object Close extends IFsmMessage
case class GoTo(index: Int) extends IFsmMessage
case class FadsReceived(fads: IMFocAds) extends IFsmMessage