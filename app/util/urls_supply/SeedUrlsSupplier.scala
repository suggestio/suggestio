package util.urls_supply

import io.suggest.proto.bixo.TalkativeActor
import io.suggest.model.JsonDfsBackend.JsonMap_t
import akka.actor._
import scala.collection.mutable
import play.api.Play.current
import scala.concurrent.duration._
import util.AkkaSiobixClient.getMainCrawlerSelector
import io.suggest.util.LogsImpl
import util.SiowebSup
import play.api.libs.concurrent.Akka
import io.suggest.proto.bixo.crawler.ReferrersBulk
import play.api.libs.concurrent.Execution.Implicits._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.12.13 18:11
 * Description: Пакетный поставщик ссылок для main-кравлера.
 * Сюда попадают ссылки из referrers, например.
 * Для облегчения тестирования, есть hi-prio ссылки, которые нужно отправить в кравлер как можно скорее.
 *
 * Актор накапливает ссылки у себя в контексте, и затем пачкой отправляет их в siobix main crawler.
 * Fsm выражен состояниями: "ожидание первой ссылки" и "накопление пакета".
 */

object SeedUrlsSupplier {
  /** Стремится к этому размеру отправляемого пакета (кол-во url). */
  val TARGET_PACKET_SIZE: Int = current.configuration.getInt("urls.supply.packet.size.target") getOrElse 30

  /** Максимальное время накопления одного пакета в памяти. Даже если в пакете лишь одна ссылка, он будет отправлен в кравлер. */
  val FORCE_FLUSH_EVERY: FiniteDuration = (current.configuration.getInt("urls.supply.flush.timeout_sec") getOrElse 20) seconds

  val actorName = getClass.getSimpleName.replace("$", "")

  val actorPath = SiowebSup.actorPath / actorName

  def actorSelection = Akka.system.actorSelection(actorPath)

  /**
   * Запуск актора в контексте другого актора. Всегда вызывается из [[util.SiowebSup]].
   * @param arf Интерфейс актора-супервизора.
   * @return ActorRef нового актора.
   */
  def startLink(arf: ActorRefFactory): ActorRef = {
    arf.actorOf(Props[SeedUrlsSupplier], name = actorName)
  }

  /** Стандартный приём реферрера и отправка в очередь. */
  def sendReferrer(url: String) {
    actorSelection ! ReferrerMsg(url)
  }

  /** Для отладки и возможно каких-либо ещё нужд, есть ускоренная отправка ref'ов в кравлер.
    * Актор немедленно завершает текущий пакет и отправляет его в кравлер. */
  def sendReferrerNow(url: String) {
    actorSelection ! HiPrioReferrerMsg(url)
  }

}


import SeedUrlsSupplier._

class SeedUrlsSupplier extends TalkativeActor {

  private val LOGGER = new LogsImpl(getClass)
  import LOGGER._


  override def preStart() = {
    super.preStart()
    // Самоконтроль. Проверяем, что static client будет правильно слать сообщения.
    if (self.path != actorPath) {
      throw new IllegalStateException(s"Invalid self actor path: expected = $actorPath , but real = ${self.path}")
    }
  }

  /** Динамический буфер ссылок, поставленных для отправки в кравлер. */
  protected var fsmState: FsmState = WaitingForFirstUrlState

  protected def allStatesReceiver: Actor.Receive = super.receive

  def taGetStatusReport: JsonMap_t = Map(
    "TODO" -> "TODO"
  )


  def become(nextState: FsmState) {
    trace(s"become(): ${fsmState.name} -> ${nextState.name}")
    fsmState = nextState
    context.become(nextState.receiver, discardOld = true)
  }


  def sendReferrers(urls: List[String]) {
    getMainCrawlerSelector ! ReferrersBulk(urls)
  }

  /* ================================================= FSM states ================================================= */

  trait FsmState {
    def superReceiver = allStatesReceiver
    def receiverPart: Actor.Receive
    def receiver: Actor.Receive = receiverPart orElse superReceiver
    def name = getClass.getSimpleName
    val logPrefix = s"[$name] "
  }


  /** Ещё ничего не накапливается в состоянии. Ждём первой ссылки. */
  protected case object WaitingForFirstUrlState extends FsmState {
    def receiverPart: Actor.Receive = {
      case rm: ReferrerMsg =>
        val nextState = AccumulatingPacketState(rm.url)
        become(nextState)

      case rm: HiPrioReferrerMsg =>
        sendReferrers(List(rm.url))
    }
  }

  /** Запуск таймера, который пришлёт в актор сообщение о необходимости флуша. */
  protected def startForceFlushTimer: Cancellable = {
    context.system.scheduler.scheduleOnce(FORCE_FLUSH_EVERY) {
      self ! PacketFlushMsg
    }
  }

  /**
   * Состояние накопление пакета.
   * @param urlsBuf Буффер ссылок в виде множества.
   * @param forceFlushTimer Таймер, который определяет максимальное время сборки пакета ссылок.
   */
  protected case class AccumulatingPacketState(
    urlsBuf: mutable.HashSet[String]  = new mutable.HashSet(),
    forceFlushTimer: Cancellable      = startForceFlushTimer
  ) extends FsmState {
    def receiverPart: Actor.Receive = {
      case rm: ReferrerMsg =>
        urlsBuf += rm.url
        // Определение размера хэш-множества - это мгновенная операция.
        if (urlsBuf.size >= TARGET_PACKET_SIZE) {
          forceFlushTimer.cancel()
          doFlush()
        }

      case rm: HiPrioReferrerMsg =>
        urlsBuf += rm.url
        forceFlushTimer.cancel()
        doFlush()

      case PacketFlushMsg =>
        doFlush()
    }

    def doFlush() {
      sendReferrers(urlsBuf.toList)
      become(WaitingForFirstUrlState)
    }
  }


  protected object AccumulatingPacketState {
    /**
     * Сборщик состояния AccumulatingPacketState на основе первой ссылки.
     * @param url Ссылка.
     * @return Новое состояние с иниализированным множеством ссылок.
     */
    def apply(url: String): AccumulatingPacketState = {
      val urlsBuf = mutable.HashSet[String](url)
      AccumulatingPacketState(urlsBuf)
    }
  }


  /** Сообщение актору о том, что пора отправить неполный пакет кравлеру. */
  protected case object PacketFlushMsg

}


case class ReferrerMsg(url: String)
case class HiPrioReferrerMsg(url: String)

