package io.suggest.event

import akka.event.{EventBus, SubchannelClassification}
import akka.util.{Timeout, Subclassification}
import akka.actor._
import scala.Some
import akka.actor.Terminated
import subscriber._
import akka.pattern.ask
import scala.concurrent.Future
import io.suggest.util.LogsAbstract

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.04.13 15:49
 * Description: Короткий порт sio_notifier.erl в скалу. Для бОльшей части функций используются шины и классификаторы akka.
 * SubchannelClassification позволяет раздавать сообщения неограниченному кругу получателей, работая по паттернам.
 *
 * В идеале, хорошо бы иметь древовидную рекурсивную структуру путей, а не текущую линейную карту событий - так эффективнее.
 * Примерная реализация статического SN-клиента:
 *
 * object SNClient extends SioNotifierStaticActorSelection {
 *   protected implicit val SN_ASC_TIMEOUT: Timeout = Timeout(5 seconds)
 *
 *   protected var snRef : ActorRef = null
 *
 *   def supPath = SiowebSup.actorPath
 *   protected def getSystem = Akka.system
 *
 *   def startLink(arf: ActorRefFactory) : ActorRef = arf.actorOf(Props[SioNotifier], name=actorName)
 * }
 */

// Статические вещи, относящиеся ко всем реализациям SioNotifier.
object SioNotifier {

  type Event = SioEventT
  type ClassifierToken = Option[Any]
  type Classifier = Seq[ClassifierToken]
  type Subscriber = SnSubscriberT

}


import SioNotifier._

/** Интерфейс для static-клиентов. Чтобы не писать комменты кучу раз и немного устаканить интерфейс взаимодействия. */
trait SioNotifierStaticClientI {

  /**
   * Запуск актора из супервизора.
   * @param arf ActorRefFactory, как правило контекст супервизора.
   * @return ActorRef запущенного сабжа.
   */
  def startLink(arf: ActorRefFactory): ActorRef

  /**
   * Подписать актора на событие
   * @param subscriber подписчик SubscriberT
   * @param classifier классификатор события
   */
  def subscribe(subscriber:Subscriber, classifier:Classifier)

  /**
   * Неблокирующая подпись на события с подтверждением от SioNotifier.
   * @param subscriber Подписчик SubscriberT.
   * @param classifier классификатор события.
   * @return Фьючерс с boolean внутри. true если всё ок.
   */
  def subscribeSync(subscriber:Subscriber, classifier:Classifier) : Future[Boolean]

  /**
   * Отправить событие в шину.
   * @param event событие
   */
  def publish(event:Event)

  /**
   * Отписать актора от событий.
   * @param subscriber описалово подписчика
   * @param classifier классификатор, такой же как был в subscribe
   */
  def unsubscribe(subscriber:Subscriber, classifier:Classifier)

  /**
   * Отписать актора от всех событий в карте шины.
   * @param subscriber подписавшийся
   */
  def unsubscribe(subscriber:Subscriber)

  /**
   * Клиент просит атомарно заменить одного подписчика на другого.
   * Проверка на наличие старого подписчика отсутствует, однако функция гарантирует, что после выполнения асинхр.операции
   * старого подписчика точно не будет, а новый уже будет привязан к шине.
   * @param subscriberOld старый подписчик, которого может и не быть на шине.
   * @param classifier классификатор для обоих
   * @param subscriberNew новый подписчик
   */
  def replaceSubscriber(subscriberOld:Subscriber, classifier:Classifier, subscriberNew:Subscriber)

  /**
   * ask-версия функции replaceSubscriber.
   * @param subscriberOld старый подписчик, которого может и не быть на шине.
   * @param classifier классификатор для обоих.
   * @param subscriberNew новый подписчик, который точно будет на шине.
   * @return Фьючерс с булевым. true, если всё нормально.
   */
  def replaceSubscriberSync(subscriberOld:Subscriber, classifier:Classifier, subscriberNew:Subscriber): Future[Boolean]
}


trait SioNotifierStaticActorSelection extends SioNotifierStaticClientI {

  val actorName: String = getClass.getSimpleName.replace("$", "")
  def supPath: ActorPath
  def actorPath = supPath / actorName

  implicit def SN_ASK_TIMEOUT: Timeout

  protected def getSystem: ActorSystem

  def actorSelection = getSystem.actorSelection(actorPath)


  def subscribe(subscriber:Subscriber, classifier:Classifier) {
    actorSelection ! SnSubscribe(subscriber, classifier)
  }

  def subscribeSync(subscriber:Subscriber, classifier:Classifier) : Future[Boolean] = {
    (actorSelection ? SnSubscribeSync(subscriber, classifier)).asInstanceOf[Future[Boolean]]
  }

  def publish(event:Event) {
    actorSelection ! event
  }

  def unsubscribe(subscriber:Subscriber, classifier:Classifier) {
    actorSelection ! SnUnsubscribe(subscriber, classifier)
  }

  def unsubscribe(subscriber:Subscriber) {
    actorSelection ! SnUnsubscribeAll(subscriber)
  }

  def replaceSubscriber(subscriberOld:Subscriber, classifier:Classifier, subscriberNew:Subscriber) {
    actorSelection ! SnReplaceSubscriber(subscriberOld, classifier, subscriberNew)
  }

  def replaceSubscriberSync(subscriberOld:Subscriber, classifier:Classifier, subscriberNew:Subscriber) = {
    (actorSelection ? SnReplaceSubscriberSync(subscriberOld, classifier, subscriberNew)).asInstanceOf[Future[Boolean]]
  }

}


/** Бывает, что необходимо задействовать статическую подписку на события для текущего объекта. (Допустим, дергать
  * какой-либо статический метод объекта при событии). Это подобие #sn_info{} в прошлом suggest.io. */
trait SNStaticSubscriber {
  def snMap: List[(Classifier, Seq[Subscriber])]
}

/** Пустая реализация трейта [[SNStaticSubscriber]]. Нужна при использовании stackable-trait'ов, реализующих snMap(). */
trait SNStaticSubscriberDummy extends SNStaticSubscriber {
  override def snMap: List[(Classifier, Seq[Subscriber])] = Nil
}


/** Когда нужно выполнить подписание/отписание всех статических подписчиков, можно подмешать этот код и
  * оформить соотв. вызов в preStart() */
trait SNStaticSubscriptionManager extends SioNotifierStaticClientI {

  /** Карта подписей: на какое событие каких подписчиков повесить. */
  protected def getStaticSubscribers: Seq[SNStaticSubscriber]

  protected def staticSubscribeAllSync() = applyForeachSC(subscribe)
  protected def staticUnsubscribeAllSync() = applyForeachSC(unsubscribe)    // TODO а надо ли оно вообще?

  protected def applyForeachSC(f: (Subscriber, Classifier) => Unit) {
    getStaticSubscribers foreach { snss =>
      applyOneSC(snss, f)
    }
  }

  def subscribeStatic(snss: SNStaticSubscriber) {
    applyOneSC(snss, subscribe)
  }
  def unsubscribeStatic(snss: SNStaticSubscriber) {
    applyOneSC(snss, unsubscribe)
  }

  protected def applyOneSC(snss: SNStaticSubscriber, f: (Subscriber, Classifier) => Unit) {
    snss.snMap foreach { case (c, sss) =>
      sss foreach { ss => f(ss, c) }
    }
  }
}


/** Реализация механизма SioNotifier в виде актора. */
trait SioNotifier extends Actor with LogsAbstract {

  // Шина сообщений. Делает все дела.
  protected val bus = new SioNotifierBus

  def receive = {
    // Пришло событие. Отправить его в шину.
    case event:Event =>
      handleEvent(event)

    // Кто-то подписывается на сообщения по классификатору.
    case SnSubscribe(subscriber, classifier) =>
      subscribeOne(subscriber, classifier)

    case SnSubscribeSync(subscriber, classifier) =>
      subscribeOne(subscriber, classifier)
      sender ! true

    // Кто-то хочет заменить одного подписчика другим атомарно.
    case SnReplaceSubscriber(subscriberOld, classifier, subscriberNew) =>
      replaceSubscriber(subscriberOld, classifier, subscriberNew)

    case SnReplaceSubscriberSync(subscriberOld, classifier, subscriberNew) =>
      replaceSubscriber(subscriberOld, classifier, subscriberNew)
      sender ! true

    // Кто-то отписывается от сообщений по классификатору.
    case SnUnsubscribe(subscriber, classifier) =>
      unsubscribeOne(subscriber, classifier)

    // Кто-то отписывается от всех сообщений.
    case SnUnsubscribeAll(subscriber) =>
      unsubscribeAll(subscriber)

    // Актор, подписанный на сообщения, помер. Нужно выкинуть его из шины.
    case Terminated(actorRef) =>
      subscriberTerminated(actorRef)
  }


  // Функции обработки вынесены сюда для возможности их простого переопределения в классах-потомках.

  protected def handleEvent(event: Event) {
    trace(s"handleEvent($event)")
    bus.publish(event)
  }

  protected def watchSubscriber(subscriber:Subscriber) {
    lazy val logPrefix = s"watchSubscriber($subscriber): "
    subscriber.getActor match {
      case Some(ref) =>
        context.watch(ref)
        trace(logPrefix + "success")

      case None =>
        trace(logPrefix + "requested subscriber is NOT an actor. Ignoring.")
    }
  }

  protected def unwatchSubscriber(subscriber:Subscriber) {
    lazy val logPrefix = s"unwatchSubscriber($subscriber): "
    subscriber.getActor match {
      case Some(ref) =>
        context.unwatch(ref)
        trace(logPrefix + "success")

      case None =>
        trace(logPrefix + "subscriber is NOT an actor. Ignoring.")
    }
  }

  protected def subscribeOne(subscriber:Subscriber, classifier:Classifier) {
    val busHasChanges = bus.subscribe(subscriber, classifier)
    debug(s"subscribeOne($subscriber, $classifier) => $busHasChanges")
    watchSubscriber(subscriber)
  }

  protected def unsubscribeOne(subscriber:Subscriber, classifier:Classifier) {
    val busHasChanges = bus.unsubscribe(subscriber, classifier)
    debug(s"unsubscribeOne($subscriber, $classifier) => $busHasChanges")
    unwatchSubscriber(subscriber)
  }

  protected def unsubscribeAll(subscriber: Subscriber) {
    debug(s"unsubscribeAll($subscriber)")
    bus.unsubscribe(subscriber)
    unwatchSubscriber(subscriber)
  }

  protected def replaceSubscriber(subscriberOld:Subscriber, classifier:Classifier, subscriberNew:Subscriber) {
    trace(s"replaceSubscriber(classifier = $classifier): $subscriberOld -> $subscriberNew")
    unsubscribeOne(subscriberOld, classifier)
    subscribeOne(subscriberNew, classifier)
  }

  protected def subscriberTerminated(actorRef: ActorRef) {
    trace(s"subscriberTerminated($actorRef)")
    bus.unsubscribe(SnActorRefSubscriber(actorRef))
  }


  // Реализация шины событий в рамках akka.
  class SioNotifierBus extends EventBus with SubchannelClassification {

    type Subscriber = SioNotifier.Subscriber
    type Event = SioNotifier.Event
    override type Classifier = SioNotifier.Classifier

    protected val _subclassification = new MySubClassification

    /**
     * Ориентировочный размер предполагаемого множества классификаторов.
     * @return
     */
    protected def mapSize: Int = 16


    /**
     * Выдать класс субклассификатора.
     * @return
     */
    protected implicit def subclassification: Subclassification[Classifier] = _subclassification


    /**
     * Вернуть классификатор для события
     * @param event событие, подлежащщее классификации.
     * @return
     */
    protected def classify(event: Event): Classifier = event.getClassifier


    /**
     * Рассылка событий адресатам
     * @param event событие
     * @param subscriber подписчик
     */
    protected def publish(event: Event, subscriber: Subscriber) {
      subscriber.publish(event)
    }


    // Подкласс, который занимается определением рассовой схожести двух классификаторов.
    class MySubClassification extends Subclassification[Classifier] {
      def isEqual(x: Classifier, y: Classifier): Boolean = {
        x == y
      }

      /**
       * True if x is subclass or equal of y.
       * @param x subclass?
       * @param y superclass
       * @return
       */
      def isSubclass(x: Classifier, y: Classifier): Boolean = {
        val xh :: xt = x
        val yh :: yt = y
        if (yh.isEmpty || xh == yh) {
          // Начала списков совпадают или являются wildcard'ами. Теперь надо обработать хвосты.
          val xte = xt.isEmpty
          val yte = yt.isEmpty
          if (xte && yte)         // Оба путя пройдены. Значит всё ок.
            true
          else if (xte || yte)    // один из списков закончился, а другой -- нет. Дальше сравнивать смысла нет, ибо это какое-то другое событие
            false
          else                    // оба хвоста содержат элементы. Продолжаем движение
            isSubclass(xt, yt)

        } else
        // xh и yh не удовлетворяют друг-другу. Значит дальше можно не проверять - событие не подходит.
          false
      }

    }
  }

}


trait SioEventT {
  def getClassifier : SioNotifier.Classifier
}

trait SioEventObjectHelperT {
  // hd - константа, отражающая голову классификатора, т.е. сравнивается перед последующим списком переменных.
  // Использование этого кортежа укорачивает логику объекта события на пару строчек.
  val hd = Some(getClass.getSimpleName)
}


// Классы-сообщения.
case class SnSubscribe(subscriber:Subscriber, classifier:Classifier)
case class SnSubscribeSync(subscriber:Subscriber, classifier:Classifier)
case class SnUnsubscribe(subscriber:Subscriber, classifier:Classifier)
case class SnUnsubscribeAll(subscriber:Subscriber)
case class SnReplaceSubscriber(subscriberOld:Subscriber, classifier:Classifier, subscriberNew:Subscriber)
case class SnReplaceSubscriberSync(subscriberOld:Subscriber, classifier:Classifier, subscriberNew:Subscriber)

