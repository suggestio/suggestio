package io.suggest.swfs.client.play

import java.io.File

import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.Matchers._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.test.FakeApplication
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.10.15 12:02
 * Description: Тесты для play-клиента seaweedfs: [[SwfsClientWs]].
 */
class SwfsClientWsSpec extends PlaySpec with OneAppPerSuite  {

  override implicit lazy val app: FakeApplication = {
    import Assign._
    new FakeApplication(
      path = new File("target/"),
      additionalConfiguration = Map(
        "play.crypto.secret"  -> "fafasdfasdf",
        MASTERS_CK            -> Seq("localhost:9333")
      )
    )
  }

  lazy val cl = app.injector.instanceOf( classOf[SwfsClientWs] )

  "assign()" must {

    "assign fid without additional params" in {
      val fut = cl.assign()
      whenReady(fut, timeout(Span(6, Seconds))) { asRes =>
        asRes.count   shouldBe  1
      }
    }

  }

}
