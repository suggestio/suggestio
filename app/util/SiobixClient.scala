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
import play.api.Logger
import io.suggest.event._
import io.suggest.proto.bixo.CrawlersSupProto.MaybeBootstrapDkey

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.10.13 18:40
 * Description: Статический Akka-клиент для связи с нодой siobix-кравлера.
 */
object SiobixClient extends SiobixClientWrapperT with SNStaticSubscriber {

  lazy val URL_PREFIX = current.configuration.getString("siobix.akka.url.prefix").get

  /** Сгенерить селектор относительно akka-корня siobix.
    * @param actorPath Полный путь до siobix-актора.
    * @return Селектор, который необязательно верен или существует.
    */
  def remoteSelection(actorPath: String) = {
    val url = URL_PREFIX + actorPath
    system.actorSelection(url)
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
    val CLIENT_AKKA = "AKKA"
    val CLIENT_FAKE_POS = "FAKE+"
    current.configuration
      .getString(confKey, Some(Set(CLIENT_AKKA, CLIENT_FAKE_POS)))
      .map(_.toUpperCase)
      .flatMap {
        case CLIENT_AKKA => None

        case CLIENT_FAKE_POS =>
          val c = new FakePositiveSiobixClient
          Logger(getClass).warn(s"siobixClientImpl: Using FAKE ${c.getClass.getSimpleName} as siobix client!!!")
          Some(c)
      }
      .getOrElse { new AkkaSiobixClient }
  }
  
  siobixClientImpl.install()

  // Костыль, чтобы akka-клиент загрузился в память и проинициализировался по-скорее.
  override def snMap: Seq[(SioNotifier.Classifier, Seq[SioNotifier.Subscriber])] = Nil
}


/** Базовый интерфейс клиента. */
sealed trait SiobixClientT {
  /** Вызывается, когда клиент входит в работу. Обычно сразу по завершению конструктора.
    * Здесь должны делаться действия, вызывающие сайд-эффекты, необходимые для работы клиента. */
  def install() {}
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

  def getCrawlersSupAskSelector = remoteAskSelection(CRAWLERS_SUP_ABSPATH)
  def getMainCrawlerAskSelector = remoteAskSelection(MAIN_CRAWLER_ABSPATH)
  def getMainCrawlerSelector    = remoteSelection(MAIN_CRAWLER_ABSPATH)

}

/** Клиент к реальному siobix, работающий через Akka.
  * Часть событий приходит из SioNotifier, поэтому тут заодно и слушалка действий, и static-подписчик в одном флаконе. */
sealed class AkkaSiobixClient extends SiobixClientT with PlayMacroLogsImpl {

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
    trace(s"maybeBootstrapDkey($dkey, $seedUrls): crawlersSup URL = " + sel)
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

