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
import io.suggest.util.LogsImpl
import play.api.Logger

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.10.13 18:40
 * Description: Статический Akka-клиент для связи с нодой siobix-кравлера.
 */
object SiobixClient extends SiobixClientWrapperT {

  val URL_PREFIX = current.configuration.getString("siobix.akka.url.prefix").get

  /** Сгенерить селектор относительно akka-корня siobix.
    * @param actorPath Полный путь до siobix-актора.
    * @return Селектор, который необязательно верен или существует.
    */
  def remoteSelection(actorPath: String) = {
    system.actorSelection(URL_PREFIX + actorPath)
  }

  def remoteAskSelection(actorPath: String) = {
    val sel = remoteSelection(actorPath)
    new AskableActorSelection(sel)
  }


  implicit val askTimeout = new Timeout(
    current.configuration.getInt("siobix.akka.bootstrap.ask_timeout_ms").getOrElse(2000) milliseconds
  )

  /** Используемый клиент для siobix. */
  protected val siobixClientImpl: SiobixClientT = {
    val confKey = "siobix.client"
    current.configuration.getString(confKey)
      .map(_.toUpperCase)
      .flatMap {
        case "AKKA"  => None

        case "FAKE+" =>
          val c = new FakePositiveSiobixClient
          Logger(getClass).warn(s"siobixClientImpl: Using FAKE ${c.getClass.getSimpleName} as siobix client!!!")
          Some(c)

        case other =>
          throw new IllegalArgumentException(s"Invalid client type in application.conf in '$confKey' = $other")
      }
      .getOrElse { new AkkaSiobixClient }
  }
}


/** Базовый интерфейс клиента. */
sealed trait SiobixClientT {
  def maybeBootstrapDkey(dkey:String, seedUrls: immutable.Seq[String]): Future[MaybeBootstrapDkeyReply_t]
  def majorRebuildRequest: Future[MajorRebuildReply_t]
}


/** Трейт враппера клиентов. */
sealed trait SiobixClientWrapperT extends SiobixClientT {
  protected def siobixClientImpl: SiobixClientT

  def maybeBootstrapDkey(dkey:String, seedUrls: immutable.Seq[String]) = {
    siobixClientImpl.maybeBootstrapDkey(dkey, seedUrls)
  }

  def majorRebuildRequest = siobixClientImpl.majorRebuildRequest
}


import SiobixClient._


object AkkaSiobixClient {

  def SIOBIX_SUP_PATH   = current.configuration.getString("siobix.akka.sup.path") getOrElse "sup"
  def CRAWLERS_SUP_PATH = current.configuration.getString("siobix.akka.crawler.sup.path").get
  val CRAWLERS_SUP_ABSPATH = "/user/" + CRAWLERS_SUP_PATH
  def MAIN_CRAWLER_PATH = CRAWLERS_SUP_PATH + "/" + MainProto.NAME
  val MAIN_CRAWLER_ABSPATH = "/user/" + MAIN_CRAWLER_PATH

  def getCrawlersSupAskSelector = remoteAskSelection(CRAWLERS_SUP_PATH)
  def getMainCrawlerAskSelector = remoteAskSelection(MAIN_CRAWLER_ABSPATH)
  def getMainCrawlerSelector    = remoteSelection(MAIN_CRAWLER_ABSPATH)
  
}

/** Клиент к реальному siobix, работающий через Akka. */
sealed class AkkaSiobixClient extends SiobixClientT {

  private val LOGGER = new LogsImpl(getClass)
  import LOGGER._
  import SiobixClient.askTimeout
  import AkkaSiobixClient._

  /**
   * Отправить в кравлер сообщение о запросе бутстрапа домена
   * @param dkey Доменный ключ.
   * @param seedUrls Ссылки для начала обхода.
   * @return
   */
  def maybeBootstrapDkey(dkey:String, seedUrls: immutable.Seq[String]) = {
    val sel = getCrawlersSupAskSelector
    trace(s"maybeBootstrapDkey($dkey, $seedUrls): crawlersSup URL = " + CRAWLERS_SUP_ABSPATH)
    (sel ? MaybeBootstrapDkey(dkey, seedUrls))
      .asInstanceOf[Future[MaybeBootstrapDkeyReply_t]]
  }

  /**
   * Запросить major rebuild поискового кластера, за которым следит main-кравлер.
   * @return Фьючерс с ответом.
   */
  def majorRebuildRequest = {
    val sel = getMainCrawlerAskSelector
    trace(s"majorRebuildRequest(): " + "main crawler selection = " + sel)
    (sel ? MajorRebuildMsg).asInstanceOf[Future[MajorRebuildReply_t]]
  }

}


/** Фейковый клиент. Всегда отвечает, что всё ок. */
sealed class FakePositiveSiobixClient extends SiobixClientT {
  def maybeBootstrapDkey(dkey: String, seedUrls: immutable.Seq[String]): Future[MaybeBootstrapDkeyReply_t] = {
    Future successful None
  }

  def majorRebuildRequest: Future[MajorRebuildReply_t] = {
    Future successful Right("Fake client - OK")
  }

}

