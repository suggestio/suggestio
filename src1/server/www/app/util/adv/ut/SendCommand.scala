package util.adv.ut

import akka.actor.Actor
import models.adv.WsMediatorRef
import models.adv.ext.act.ExtActorEnv
import models.adv.js.IWsCmd

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.04.15 13:21
 * Description: Утиль для отправки js-команд.
 */

/** Интерфейс для отправки одной команды по ws. */
trait ISendCommand {
  /** Отправка указанной js-команды по ws в браузер клиенту. */
  def sendCommand(cmd: IWsCmd): Unit
}


/** Реализация [[ISendCommand]] через промежуточный актор (актор-медиатор). */
trait MediatorSendCommand extends ISendCommand {

  def args: WsMediatorRef

  def sendCommand(cmd: IWsCmd): Unit = {
    args.wsMediatorRef ! cmd
  }

}


/** Готовое к использованию значение полей replyTo в jscmd-запросах. */
trait ReplyTo extends Actor with ExtActorEnv {
  override def replyTo = self.path.name
}

