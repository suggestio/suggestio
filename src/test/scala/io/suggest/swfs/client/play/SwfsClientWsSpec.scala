package io.suggest.swfs.client.play

import java.io.File

import io.suggest.swfs.client.proto.put.PutRequest
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.Matchers._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.iteratee.Enumerator
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
    new FakeApplication(
      path = new File("target/"),
      additionalConfiguration = Map(
        Assign.MASTERS_CK     -> Seq("localhost:9333")
      )
    )
  }


  private lazy val cl = app.injector.instanceOf( classOf[SwfsClientWs] )


  "swfs client" must {

    "assign, put, read, delete, etc" in {
      // ASSIGN
      whenReady(cl.assign(), timeout(Span(2, Seconds))) { asRes =>
        asRes.count   shouldBe  1
        assert(asRes.fid.length > 4, asRes.fid)
        assert(asRes.fidParsed.volumeId >= 0, asRes.fidParsed)
        assert(asRes.fidParsed.fileId.length > 4, asRes.fidParsed)

        // PUT
        val sample1 = "test0 test0 test0 test0 test0"
        val sample2 = "test0 test0 test0 test0 test0"
        val putReq = PutRequest(
          volUrl = asRes.url,
          fid    = asRes.fid,
          data   = Enumerator(sample1.getBytes, sample2.getBytes)
        )

        whenReady(cl.put(putReq), timeout(Span(2, Seconds))) { putResp =>
          putResp.size  shouldBe  (sample1.length + sample2.length)

          // TODO read, delete
        }
      }
    }

  }

}
