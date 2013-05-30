package io.suggest.event

import akka.event.{EventBus, SubchannelClassification}
import akka.util.Subclassification
import akka.actor._
import io.suggest.SioutilSup
import scala.Some
import akka.actor.Terminated
import subscriber._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.04.13 15:49
 * Description: Короткий порт sio_notifier.erl в скалу. Для бОльшей части функций используются шины и классификаторы akka.
 * SubchannelClassification позволяет раздавать сообщения неограниченному кругу получателей, работая по паттернам.
 *
 * В идеале, хорошо бы иметь древовидную рекурсивную структуру путей, а не текущую линейную карту событий - так эффективнее.
 */

object SioNotifier {

  type Event = SioEventT
  type ClassifierToken = Option[Any]
  type Classifier = List[ClassifierToken]
  type Subscriber = SnSubscriberT

  private var snRef : ActorRef = null

  val SN_NAME = "sn"

  def startLink(arf: ActorRefFactory) : ActorRef = {
    snRef = arf.actorOf(Props[SioNotifier], name=SN_NAME)
    snRef
  }

  /**
   * Узнать ref watcher-а у супервизора.
   * @return
   */
  def watcherActorRefFuture = SioutilSup.getChild(SN_NAME)

  /**
   * Подписать актора на событие
   * @param subscriber подписчик SubscriberT
   * @param classifier классификатор события
   */
  def subscribe(subscriber:Subscriber, classifier:Classifier) {
    snRef ! SnSubscribe(subscriber, classifier)
  }

  /**
   * Отправить событие в шину.
   * @param event событие
   */
  def publish(event:Event) {
    snRef ! event
  }

  /**
   * Отписать актора от событий.
   * @param subscriber описалово подписчика
   * @param classifier классификатор, такой же как был в subscribe
   */
  def unsubscribe(subscriber:Subscriber, classifier:Classifier) {
    snRef ! SnUnsubscribe(subscriber, classifier)
  }

  /**
   * Отписать актора от всех событий в карте шины.
   * @param subscriber подписавшийся
   */
  def unsubscribe(subscriber:Subscriber) {
    snRef ! SnUnsubscribeAll(subscriber)
  }

}


// Нужно наблюдать за подписчиками, т.к. они могут внезапно отвалится, а подписки останутся.
class SioNotifier extends Actor {

  import SioNotifier.{Subscriber, Event}

  // Шина сообщений. Делает все дела.
  protected val bus = new SioNotifierBus

  def receive = {
    // Пришло событие. Отправить его в шину.
    case event:Event =>
      bus.publish(event)

    // Кто-то подписывается на сообщения по классификатору.
    case SnSubscribe(subscriber, classifier) =>
      bus.subscribe(subscriber, classifier)
      watchSubscriber(subscriber)

    // Кто-то отписывается от сообщений по классификатору.
    case SnUnsubscribe(subscriber, classifier) =>
      bus.unsubscribe(subscriber, classifier)
      umwatchSubscriber(subscriber)

    // Кто-то отписывается от всех сообщений.
    case SnUnsubscribeAll(subscriber) =>
      bus.unsubscribe(subscriber)
      umwatchSubscriber(subscriber)

    // Актор, подписанный на сообщения, помер. Нужно выкинуть его из шины.
    case Terminated(actorRef) =>
      bus.unsubscribe(SnActorRefSubscriber(actorRef))
  }


  protected def watchSubscriber(subscriber:Subscriber) {
    subscriber.getActor.foreach(context.watch)
  }

  protected def umwatchSubscriber(subscriber:Subscriber) {
    subscriber.getActor.foreach(context.unwatch)
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

protected final case class SnSubscribe(subscriber:SioNotifier.Subscriber, classifier:SioNotifier.Classifier)
protected final case class SnUnsubscribe(subscriber:SioNotifier.Subscriber, classifier:SioNotifier.Classifier)
protected final case class SnUnsubscribeAll(subscriber:SioNotifier.Subscriber)


