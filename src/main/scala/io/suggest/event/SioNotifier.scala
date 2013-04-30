package io.suggest.event

import akka.event.ActorEventBus
import akka.event.SubchannelClassification
import akka.util.Subclassification
import akka.actor.{Terminated, Actor, ActorRef}
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

  val ANY = new AnyClassifier
  protected val bus = new SioNotifierBus

  val SN_WATCHER_NAME = "sn_watcher"

  /**
   * Узнать ref watcher-а у супервизора.
   * @return
   */
  def actorRefFuture = SioutilSup.getChild(SN_WATCHER_NAME)

  /**
   * Подписать актора на событие
   * @param actor актор
   * @param classifier классификатор события
   */
  def subscribe(actor:ActorRef, classifier:Classifier) {
    bus.subscribe(actor, classifier)
    actorRefFuture.onSuccess {
      case Some(watcherRef) => watcherRef ! WatchActor(actor)
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
   * @param actor ActorRef
   * @param classifier классификатор, такой же как был в subscribe
   */
  def unsubscribe(actor:ActorRef, classifier:Classifier) {
    unwatch(actor)
    bus.unsubscribe(actor, classifier)
  }

  /**
   * Отписать актора от всех событий в карте шины.
   * @param actor актор
   */
  def unsubscribe(actor:ActorRef) {
    unwatch(actor)
    bus.unsubscribe(actor)
  }


  /**
   * Отписать актора от наблюдения со стороны ActorWatcher.
   * @param actor актор
   */
  protected def unwatch(actor:ActorRef) {
    actorRefFuture.onSuccess {
      case Some(watcherRef) =>
        watcherRef ! UnwatchActor(actor)
    }
  }

  def generateClassifier(clazz:Class[_], tokens : ClassifierToken*) : Classifier = {
    Some(clazz.getName) :: tokens.toList
  }
}


class SioNotifierBus extends ActorEventBus with SubchannelClassification with Logs {

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
    subscriber ! event
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
      SioNotifier.unsubscribe(actorRef)
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


final class AnyClassifier {
  override def equals(obj: Any): Boolean = super.equals(obj) || obj.isInstanceOf[AnyClassifier]
  override def hashCode(): Int = 83457820
  override def toString: String = "*"
}

final case class WatchActor(actorRef:ActorRef)
final case class UnwatchActor(actorRef:ActorRef)
