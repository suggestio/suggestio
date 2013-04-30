package io.suggest.event

import akka.event.{EventBus, ActorEventBus, SubchannelClassification}
import akka.util.Subclassification
import akka.actor.{ActorPath, Terminated, Actor, ActorRef}
import io.suggest.SioutilSup
import scala.concurrent.ExecutionContext.Implicits.global
import io.suggest.util.Logs

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
  type Subscriber = SubscriberT

  protected val bus = new SioNotifierBus

  val SN_WATCHER_NAME = "sn_watcher"

  /**
   * Узнать ref watcher-а у супервизора.
   * @return
   */
  def watcherActorRefFuture = SioutilSup.getChild(SN_WATCHER_NAME)

  /**
   * Подписать актора на событие
   * @param subscriber подписчик SubscriberT
   * @param classifier классификатор события
   */
  def subscribe(subscriber:Subscriber, classifier:Classifier) {
    bus.subscribe(subscriber, classifier)
    subscriber.getActor.map { actor =>
      watcherActorRefFuture.onSuccess {
        case Some(watcherRef) => watcherRef ! WatchActor(actor)
      }
    }
  }

  /**
   * Отправить событие в шину.
   * @param event событие
   */
  def publish(event:Event) {
    bus.publish(event)
  }

  /**
   * Отписать актора от событий.
   * @param subscriber описалово подписчика
   * @param classifier классификатор, такой же как был в subscribe
   */
  def unsubscribe(subscriber:Subscriber, classifier:Classifier) {
    unwatch(subscriber)
    bus.unsubscribe(subscriber, classifier)
  }

  /**
   * Отписать актора от всех событий в карте шины.
   * @param subscriber подписавшийся
   */
  def unsubscribe(subscriber:Subscriber) {
    unwatch(subscriber)
    bus.unsubscribe(subscriber)
  }


  /**
   * Отписать актора от наблюдения со стороны ActorWatcher.
   * @param subscriber подписавшийся
   */
  protected def unwatch(subscriber:Subscriber) {
    subscriber.getActor.map { actor =>
      watcherActorRefFuture.onSuccess {
        case Some(watcherRef) =>
          watcherRef ! UnwatchActor(actor)
      }
    }
  }

}


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
   * @param event
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


// Нужно наблюдать за подписчиками, т.к. они могут внезапно отвалится, а подписки останутся.
class SioNotifierWatcher extends Actor {

  def receive = {
    case WatchActor(actorRef) =>
      context.watch(actorRef)

    case UnwatchActor(actorRef) =>
      context.unwatch(actorRef)

    case Terminated(actorRef) =>
      SioNotifier.unsubscribe(ActorRefSubscriber(actorRef))
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


final case class WatchActor(actorRef:ActorRef)
final case class UnwatchActor(actorRef:ActorRef)


/**
 * Подписчик на шине. Он имеет метод publish(event), скрывающий его внутреннюю структуру (актора, метод и тд)
 */
trait SubscriberT {
  def publish(event:SioNotifier.Event)
  def getActor : Option[ActorRef]
  def toStringTail : String
  override def toString: String = "sn_subscriber:" + toStringTail
}


/**
 * Актор подписывается на сообщения.
 * @param actorRef реф актора.
 */
case class ActorRefSubscriber(actorRef:ActorRef) extends SubscriberT {
  def publish(event: SioNotifier.Event) {
    actorRef ! event
  }

  def getActor = Some(actorRef)

  override def hashCode(): Int = actorRef.hashCode() + 10
  override def equals(obj: Any): Boolean = super.equals(obj) || {
    obj match {
      case ActorRefSubscriber(_actor) => actorRef == _actor
      case _ => false
    }
  }
  def toStringTail: String = actorRef.toString()
}

/**
 * Актор задан через путь. Связаваться с супервизором, чтоб он отрезовлвил путь.
 * @param actorPath путь до актора
 */
case class ActorPathSubscriber(actorPath:ActorPath) extends SubscriberT {
  def publish(event: SioNotifier.Event) {
    SioutilSup.resolveActorPath(actorPath) onSuccess { case actor =>
      actor ! event
    }
  }

  def getActor = None

  override def hashCode(): Int = actorPath.hashCode() + 31
  override def equals(obj: Any): Boolean = super.equals(obj) || {
    obj match {
      case ActorPathSubscriber(_actorPath) => actorPath == _actorPath
      case _ => false
    }
  }
  def toStringTail: String = actorPath.toString
}
