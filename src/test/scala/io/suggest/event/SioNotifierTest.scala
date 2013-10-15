package io.suggest.event

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import akka.actor._
import akka.actor.ActorDSL._
import SioNotifier._
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout
import scala.concurrent.Await
import io.suggest.util.Logs
import io.suggest.event.subscriber.SnActorRefSubscriber
import scala.Some

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.04.13 19:10
 * Description: Тестирование набора SioNotifier происходит путем имитации работы нескольких акторов внутри некоторой
 * Actor System. В данном случае есть два актора, которые подписаны на те или иные сообщения.
 */

/* TODO Тест выполняется нормально, но вылетает сообщение о недоставке:
  [SioNotifierTestSys-akka.actor.default-dispatcher-6] [akka://SioNotifierTestSys/deadLetters] Message
  [io.suggest.event.SioNotifier$SnSubscribe] from Actor[akka://SioNotifierTestSys/deadLetters] to
  Actor[akka://SioNotifierTestSys/deadLetters] was not delivered. [1] dead letters encountered. This logging can be
  turned off or adjusted with configuration settings 'akka.log-dead-letters' and 'akka.log-dead-letters-during-shutdown'.
  */

class SioNotifierTest extends FlatSpec with ShouldMatchers with Logs {

  // Определяем контекст тестов
  def fixture = new {
    implicit val asys = ActorSystem.create("SioNotifierTestSys")
    val snClient = new SioNotifierStaticActorSelection {
      def supPath: ActorPath = (asys / actorName).parent
      implicit def SN_ASK_TIMEOUT: Timeout = 1 seconds
      protected def getSystem: ActorSystem = asys

      def startLink(arf: ActorRefFactory): ActorRef = {
        asys.actorOf(Props(new SioNotifier with Logs), name=actorName)
      }
    }
    import snClient._
    snClient.startLink(asys)

    // актор act0 принимает любые Event2
    val act0 = actor(new Act {
      protected var uid10Rcvd = 0
      protected var uid20Rcvd = 0
      protected def self_s = SnActorRefSubscriber(self)

      whenStarting {
        subscribe(self_s, Event2.getClassifier() )
      }

      become {
        case Event2(user_id, data) =>
          if (user_id == 10)
            uid10Rcvd = uid10Rcvd + 1
          else if (user_id == 20)
            uid20Rcvd = uid20Rcvd + 1

        case WasEvent() =>
          sender ! (uid10Rcvd, uid20Rcvd)
          uid10Rcvd = 0
          uid20Rcvd = 0
      }

      whenStopping {
        unsubscribe(self_s)
      }
    })

    // актор act20 хочет видеть Event2 только с user_id = 20 и никакие иные
    val act20 = actor(new Act {
      protected var uid20Received = 0
      protected var uidOtherReceived = 0
      protected def self_s = SnActorRefSubscriber(self)

      whenStarting {
        subscribe(self_s, Event2.getClassifier(user_id = Some(20)) )
      }

      become {
        case Event2(user_id, data) if user_id == 20 =>
          uid20Received = uid20Received + 1

        case _:Event2 =>
          uidOtherReceived = uidOtherReceived + 1

        case WasEvent() =>
          sender ! (uid20Received, uidOtherReceived)
          uid20Received = 0
          uidOtherReceived = 0
      }

      // unsubscribe "забываем" сделать
    })
  }

  val we = WasEvent()
  private val timeoutSec = 2.seconds
  private implicit val timeout = Timeout(timeoutSec)

  object ActorsClient {
    def wasEvent(to:ActorRef) = {
      Await.result(to ? we, timeoutSec).asInstanceOf[(Int,Int)]
    }
  }


  // Непосредственно, тесты
  "SN bus" should "transfer events to actors" in {
    import ActorsClient._
    val f = fixture
    import f._
    import snClient._

    // Просто проверяем act0
    wasEvent(act0)  should equal (0, 0)

    publish(Event2(user_id=10, data="asd"))
    Thread.sleep(40)
    wasEvent(act0)  should equal (1, 0)
    wasEvent(act20) should equal (0, 0)

    // Проверяем act20
    publish(Event2(user_id=20, data="bbb"))
    Thread.sleep(40)
    wasEvent(act0)  should equal (0, 1)
    wasEvent(act20) should equal (1, 0)

    // Проверяем unsubscribe
    unsubscribe(SnActorRefSubscriber(act0))
    publish(Event2(user_id=10, data="asd"))
    Thread.sleep(40)
    wasEvent(act0)  should equal (0, 0)
  }



  // Нужно очищать контекст по завершении тестов.
  override protected def withFixture(test: NoArgTest) {
    try {
      super.withFixture(test)

    } finally {
      // Остановить всех акторов после тестов
      fixture.asys.shutdown()
    }
  }


  case class Event2(user_id:Int, data:String) extends SioEventT {
    lazy val getClassifier: Classifier = Event2.getClassifier(user_id=Some(user_id), data=Some(data))
  }

  object Event2 extends SioEventObjectHelperT {
    def getClassifier(user_id:Option[Int] = None, data:Option[String] = None) : Classifier = List(hd, user_id, data)
  }


  case class WasEvent()

}
