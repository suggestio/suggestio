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

  def URL_PREFIX = current.configuration.getString("siobix.akka.url.prefix").get
  def CRAWLERS_SUP_LP = current.configuration.getString("siobix.akka.crawler.sup.path").get

  implicit val askTimeout = new Timeout(
    current.configuration.getInt("siobix.akka.bootstrap.ask_timeout_ms").getOrElse(2000) milliseconds
  )

  val CRAWLERS_SUP_URL = URL_PREFIX + CRAWLERS_SUP_LP
  def getCrawlersSupSelector = new AskableActorSelection(system actorSelection CRAWLERS_SUP_URL)

  val MAIN_CRAWLER_URL = URL_PREFIX + CRAWLERS_SUP_LP + "/" + MainProto.NAME
  def getMainCrawlerSelector = new AskableActorSelection(system actorSelection MAIN_CRAWLER_URL)

  /**
   * Отправить в кравлер сообщение о запросе бутстрапа домена
   * @param dkey Доменный ключ.
   * @param seedUrls Ссылки для начала обхода.
   * @return
   */
  def maybeBootstrapDkey(dkey:String, seedUrls: immutable.Seq[String]) = {
    val sel = getCrawlersSupSelector
    trace(s"maybeBootstrapDkey($dkey, $seedUrls): crawlersSup URL = " + CRAWLERS_SUP_URL)
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
