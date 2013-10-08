package util

import play.api.Play.current
import play.api.libs.concurrent.Akka.system
import akka.actor.{Actor, Props, ActorRefFactory, ActorRef}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.10.13 18:40
 * Description: Статический Akka-клиент для связи с нодой siobix-кравлера.
 */
object SiobixClient {

  private var clientRef: ActorRef = null

  def URL_PREFIX = current.configuration.getString("siobix.akka.url.prefix").get
  def CRAWLERS_SUP_LP = current.configuration.getString("siobix.akka.path.crawler.sup").get

  def CRAWLERS_SUP_URL = URL_PREFIX + CRAWLERS_SUP_LP
  def CRAWLERS_SUP_PATH = system actorSelection CRAWLERS_SUP_URL

  def startLink(arf: ActorRefFactory): ActorRef = {
    val ref = arf.actorOf(Props[SiobixClient])
    clientRef = ref
    ref
  }

}

// Для отправки надо указать адресата. Этот актор - получатель ответов от кравлеров.
class SiobixClient extends Actor {
  def receive: Actor.Receive = {
    case _ => ???
  }
}
