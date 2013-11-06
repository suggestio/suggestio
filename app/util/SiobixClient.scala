package util

import play.api.Play.current
import play.api.libs.concurrent.Akka.system
import akka.pattern.AskableActorSelection
import io.suggest.proto.bixo.CrawlersSupProto._
import scala.concurrent.Future
import scala.collection.immutable
import akka.util.Timeout
import concurrent.duration._
import io.suggest.proto.bixo.crawler._, MainProto.MajorRebuildReply_t
import akka.actor.ActorPath

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.10.13 18:40
 * Description: Статический Akka-клиент для связи с нодой siobix-кравлера.
 */
object SiobixClient extends Logs {

  import LOGGER._

  val URL_PREFIX = current.configuration.getString("siobix.akka.url.prefix").get
  val CRAWLERS_SUP_PATH = current.configuration.getString("siobix.akka.crawler.sup.path").get

  implicit val askTimeout = new Timeout(
    current.configuration.getInt("siobix.akka.bootstrap.ask_timeout_ms").getOrElse(2000) milliseconds
  )

  def remoteSelection(actorPath: String) = new AskableActorSelection(system.actorSelection(URL_PREFIX + actorPath))
  def getCrawlersSupSelector = remoteSelection(CRAWLERS_SUP_PATH)

  val MAIN_CRAWLER_PATH = CRAWLERS_SUP_PATH + "/" + MainProto.NAME
  def getMainCrawlerSelector = remoteSelection(MAIN_CRAWLER_PATH)

  /**
   * Отправить в кравлер сообщение о запросе бутстрапа домена
   * @param dkey Доменный ключ.
   * @param seedUrls Ссылки для начала обхода.
   * @return
   */
  def maybeBootstrapDkey(dkey:String, seedUrls: immutable.Seq[String]) = {
    val sel = getCrawlersSupSelector
    trace(s"maybeBootstrapDkey($dkey, $seedUrls): crawlersSup URL = " + CRAWLERS_SUP_PATH)
    (sel ? MaybeBootstrapDkey(dkey, seedUrls))
      .asInstanceOf[Future[MaybeBootstrapDkeyReply_t]]
  }

  /**
   * Запросить major rebuild поискового кластера, за которым следит main-кравлер.
   * @return Фьючерс с ответом.
   */
  def majorRebuildRequest = {
    val sel = getMainCrawlerSelector
    trace(s"majorRebuildRequest(): " + "main crawler selection = " + sel)
    (sel ? MajorRebuildMsg).asInstanceOf[Future[MajorRebuildReply_t]]
  }

}

// TODO Следует использовать один актора для диспетчеризации протокола.
//      Ибо ask() всегда порождает новый актора в /temp, а тут этого можно и нужно избежать.
