package io.suggest.swfs.client.play

import java.io.File

import io.suggest.itee.IteeUtil
import io.suggest.swfs.client.proto.delete.DeleteRequest
import io.suggest.swfs.client.proto.get.GetRequest
import io.suggest.swfs.client.proto.put.PutRequest
import org.apache.commons.io.FileUtils
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

    val _timeout = timeout(Span(2, Seconds))

    // ASSIGN
    info("Assigning new fid...")
    whenReady(cl.assign(), _timeout) { asRes =>
      val fileUrl = "http://" + asRes.url + "/" + asRes.fid
      info("Assigned new fid: " + fileUrl)

      "handle assign response" in {
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
      val ct = "text/plain"

      "PUT file" in {
        val putReq = PutRequest(
          volUrl        = asRes.url,
          fid           = asRes.fid,
          file          = f,
          contentType   = ct,
          origFileName  = None
        )

        info("PUT " + fileUrl)
        whenReady(cl.put(putReq), _timeout) { putResp =>
          // put resp возвращает compressed size.
          putResp.occupiedSize should be <= f.length
        }
      }


      "GET existing file" in {
        val getReq = GetRequest(
          volUrl = asRes.url,
          fid    = asRes.fid
        )
        info("GET " + fileUrl)
        whenReady(cl.get(getReq), _timeout) { getResp =>
          assert(getResp.nonEmpty, getResp)
          val gr = getResp.get
          val fcontentFut = IteeUtil.dumpBlobs( gr.enumerator )
            .map { new String(_) }
          gr.size         shouldBe  f.length()
          gr.contentType  shouldBe  ct
          whenReady(fcontentFut, _timeout) { str =>
            str shouldBe fileData
          }
        }
      }


      "GET inexisting -> 404" in {
        // GET 404
        val getReq2 = GetRequest(
          volUrl  = asRes.url,
          fid     = "123" + asRes.fid
        )
        info("GET " + getReq2.toUrl)
        whenReady(cl.get(getReq2), _timeout) { getResp =>
          assert(getResp.isEmpty, getResp)
        }
      }


      val deleteReq = DeleteRequest(
        volUrl = asRes.url,
        fid    = asRes.fid
      )
      "DELETE existing -> 200 OK" in {
        info("DELETE " + fileUrl)
        whenReady(cl.delete(deleteReq), _timeout) { delResp =>
          assert(delResp.nonEmpty, delResp)
          val dr = delResp.get
          assert(dr.isExisted, dr)
        }
      }


      "DELETE of already deleted -> 404/None" in {
        info("DELETE " + fileUrl)
        whenReady(cl.delete(deleteReq), _timeout) { del2Resp =>
          assert(del2Resp.isEmpty, del2Resp)
        }
      }


      // DELETE 404
      "DELETE of never-existed file -> 404/None" in {
        val delReq404 = deleteReq.copy(
          fid = "1234" + deleteReq.fid
        )
        info("DELETE inexisting " + delReq404.toUrl)
        whenReady(cl.delete(delReq404), _timeout) { del3Resp =>
          assert(del3Resp.isEmpty, del3Resp)
        }
      }

    }   // assign ready

  }

}
