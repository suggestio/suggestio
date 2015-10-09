package io.suggest.swfs.client.play

import java.io.File

import io.suggest.swfs.client.proto.delete.DeleteRequest
import io.suggest.swfs.client.proto.put.PutRequest
import io.suggest.util.MacroLogsImpl
import org.apache.commons.io.FileUtils
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
class SwfsClientWsSpec extends PlaySpec with OneAppPerSuite {

  override implicit lazy val app: FakeApplication = {
    new FakeApplication(
      path = new File("target/"),
      additionalConfiguration = Map(
        Assign.MASTERS_CK     -> Seq("localhost:9333")
      )
    )
  }


  private lazy val cl = app.injector.instanceOf( classOf[SwfsClientWs] )


  "SeaWeedFS client" must {

    // ASSIGN
    info("Assigning new fid...")
    whenReady(cl.assign(), timeout(Span(2, Seconds))) { asRes =>
      val fileUrl = "http://" + asRes.url + "/" + asRes.fid
      info("Assigned new fid: " + fileUrl)

      "handle assign" in {
        asRes.count   shouldBe  1
        assert(asRes.fid.length > 4, asRes.fid)
        assert(asRes.fidParsed.volumeId >= 0, asRes.fidParsed)
        assert(asRes.fidParsed.fileId.length > 4, asRes.fidParsed)
      }

      // PUT
      // Подготовить (создать) тестовый файл, TODO закинуть в него тестовые данные.
      val f = File.createTempFile("swfs-client-test", ".txt")
      f.deleteOnExit()

      val fileData = "test test test test test test test test test"
      FileUtils.write(f, fileData)

      val putReq = PutRequest(
        volUrl        = asRes.url,
        fid           = asRes.fid,
        file          = f,
        contentType   = "text/plain",
        origFileName  = None
      )

      info("PUTting to " + fileUrl)
      whenReady(cl.put(putReq), timeout(Span(2, Seconds))) { putResp =>
        // put resp возвращает compressed size.
        "handle put" in {
          putResp.occupiedSize should be <= f.length
        }

        // TODO read

        // DELETE
        val deleteReq = DeleteRequest(
          volUrl = asRes.url,
          fid    = asRes.fid
        )

        info("DELETEing " + fileUrl)
        val delTimeout = timeout(Span(2, Seconds))
        whenReady(cl.delete(deleteReq), delTimeout) { delResp =>
          "handle normal delete -> success" in {
            assert(delResp.isExisted, delResp)
          }

          // reDELETE => 404
          info("reDELETEing " + fileUrl)
          whenReady(cl.delete(deleteReq), delTimeout) { del2Resp =>
            "handle re-delete -> 404" in {
              assert(!del2Resp.isExisted, del2Resp)
            }
          }
        }

        // DELETE 404
        val delReq404 = deleteReq.copy(
          fid = "1234" + deleteReq.fid
        )
        info("DELETEing inexisting " + delReq404.toUrl)
        whenReady(cl.delete(delReq404), delTimeout) { del3Resp =>
          "handle delete never-existed file -> 404" in {
            assert(!del3Resp.isExisted, del3Resp)
          }
        }
      }

    }

  }

}
