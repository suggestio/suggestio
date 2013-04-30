package io.suggest.event

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import akka.actor.{ActorRef, ActorSystem}
import io.suggest.SioutilSup
import akka.actor.ActorDSL._
import SioNotifier._
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout
import scala.concurrent.Await
import io.suggest.util.Logs

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.04.13 19:10
 * Description: Тестирование набора SioNotifier происходит путем имитации работы нескольких акторов внутри некоторой
 * Actor System. В данном случае есть два актора, которые подписаны на те или иные сообщения.
 */

class SioNotifierTest extends FlatSpec with ShouldMatchers with Logs {

  // Определяем контекст тестов
  def fixture = {
    new {
      implicit val asys = ActorSystem.create("SioNotifierTestSys")
      val sup = SioutilSup.start_link(asys)

      // актор act0 принимает любые Event2
      val act0 = actor(new Act {
        protected var uid10Rcvd = 0
        protected var uid20Rcvd = 0

        whenStarting {
          subscribe(self, Event2.getClassifier() )
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
          unsubscribe(self)
        }
      })

      // актор act20 хочет видеть Event2 только с user_id = 20 и никакие иные
      val act20 = actor(new Act {
        protected var uid20Received = 0
        protected var uidOtherReceived = 0

        whenStarting {
          subscribe(self, Event2.getClassifier(user_id = Some(20)) )
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

    // Проверяем act0
    wasEvent(act0) should equal (0, 0)

    SioNotifier.publish(Event2(user_id=10, data="asd"))
    Thread.sleep(40)
    wasEvent(f.act0) should equal (1, 0)

    // Проверяем act20
    wasEvent(act20)  should equal (0, 0)
    SioNotifier.publish(Event2(user_id=20, data="bbb"))

    Thread.sleep(40)
    wasEvent(act20) should equal (1, 0)
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


  case class Event3(user_id:Int, timestamp:Long, msg:String) extends SioEventT {
    lazy val getClassifier: Classifier = Event3.getClassifier(user_id=Some(user_id))
  }

  object Event3 extends SioEventObjectHelperT {
    def getClassifier(user_id:Option[Int] = None) : Classifier = List(hd, user_id)
  }


  case class WasEvent()

}
