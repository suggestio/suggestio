package io.suggest.swfs.client.play

import java.io.File

import io.suggest.streams.StreamsUtil
import io.suggest.swfs.client.proto.delete.DeleteRequest
import io.suggest.swfs.client.proto.get.GetRequest
import io.suggest.swfs.client.proto.lookup.LookupRequest
import io.suggest.swfs.client.proto.put.PutRequest
import org.apache.commons.io.FileUtils
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.Matchers._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatestplus.play.PlaySpec
import play.api.{Application, Configuration}
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.ExecutionContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.10.15 12:02
 * Description: Тесты для play-клиента seaweedfs: [[SwfsClientWs]].
 */
class SwfsClientWsSpec extends PlaySpec with GuiceOneAppPerSuite {

  override implicit lazy val app: Application = {
    GuiceApplicationBuilder(
      configuration = Configuration(
        SwfsClientWs.MASTERS_CK     -> Seq("localhost:9333")
      )
    )
      .build()
  }


  private lazy val cl = app.injector.instanceOf[SwfsClientWs]
  private lazy val streamsUtil = app.injector.instanceOf[StreamsUtil]
  implicit private lazy val ec = app.injector.instanceOf[ExecutionContext]


  "SeaWeedFS client" when {

    val _timeout = timeout(Span(2, Seconds))

    // ASSIGN
    info("Assigning new fid...")
    val asResp = whenReady(cl.assign(), _timeout) { asRes =>

      "assign response received" must {
        "containt ONE count" in {
          asRes.count shouldBe 1
        }
        "contain non-empty fid" in {
          assert(asRes.fid.length > 4, asRes.fid)
        }
        "contain looking-valid (positive) volume id" in {
          assert(asRes.fidParsed.volumeId >= 0, asRes.fidParsed)
        }
        "contain non-empty file id" in {
          assert(asRes.fidParsed.fileId.length > 4, asRes.fidParsed)
        }
      }
      asRes
    }

    val fileUrl = "http://" + asResp.url + "/" + asResp.fid
    info("Assigned new fid: " + fileUrl)

    val getReq = GetRequest(
      volUrl = asResp.url,
      fid    = asResp.fid
    )

    "HEAD isExists for not-yet-existing file" must {
      whenReady(cl.isExist(getReq), _timeout) { isExist =>
        "return false" in {
          assert(!isExist, getReq)
        }
      }
    }

    // PUT
    // Подготовить (создать) тестовый файл, TODO закинуть в него тестовые данные.
    val f = File.createTempFile("swfs-client-test", ".txt")
    f.deleteOnExit()

    val fileData = "test test test test test test test test test"
    FileUtils.write(f, fileData)
    val ct = "text/plain"

    "PUT new file" must {
      val putReq = PutRequest(
        volUrl        = asResp.url,
        fid           = asResp.fid,
        file          = f,
        contentType   = ct,
        origFileName  = None
      )

      info("PUT " + fileUrl)
      whenReady(cl.put(putReq), _timeout) { putResp =>
        // put resp возвращает compressed size.
        "contain reasonable occupied storage size" in {
          putResp.occupiedSize should be <= f.length
        }
      }
    }


    "HEAD isExists for uploaded file" must {
      whenReady( cl.isExist(getReq), _timeout) { isExists =>
        "return true" in {
          assert(isExists, getReq)
        }
      }
    }


    "GET lookup for volume, related to saved file" must {
      val lcArgs = LookupRequest(
        volumeId = asResp.fidParsed.volumeId
      )
      whenReady(cl.lookup(lcArgs), _timeout) { lcResp =>
        "have positive lookup result" in {
          assert(lcResp.isRight, lcResp)
        }
        val r = lcResp.right.get
        "contain requested volumeId" in {
          r.volumeId  shouldBe  lcArgs.volumeId
        }
        "contain exactly one shard location" in {
          r.locations.size  shouldBe  1
        }
        val vloc = r.locations.head
        "contain assigned volume URL" in {
          vloc.url  shouldBe  asResp.url
        }
        "contain assigned public volume URL" in {
          vloc.publicUrl  shouldBe  asResp.publicUrl
        }
      }
    }


    "GET saved & existing file" must {
      info("GET " + fileUrl)
      whenReady(cl.get(getReq), _timeout) { getResp =>
        "contain non-empty response" in {
          assert(getResp.nonEmpty, getResp)
        }
        val gr = getResp.get
        val fcontentFut = streamsUtil.mergeByteStrings( gr.data )
          .map { bs => new String( bs.toArray ) }
        "have expected resp.body lenght" in {
          gr.sizeB shouldBe f.length()
        }
        "contain valid Content-Type" in {
          gr.contentType shouldBe ct
        }
        whenReady(fcontentFut, _timeout) { str =>
          "contain previously-stored file content" in {
            str shouldBe fileData
          }
        }
      }
    }


    "GET inexisting" must {
      // GET 404
      val getReq2 = GetRequest(
        volUrl  = asResp.url,
        fid     = "123" + asResp.fid
      )
      info("GET " + getReq2.toUrl)
      whenReady(cl.get(getReq2), _timeout) { getResp =>
        "return empty response" in {
          assert(getResp.isEmpty, getResp)
        }
      }
    }


    val deleteReq = DeleteRequest(
      volUrl = asResp.url,
      fid    = asResp.fid
    )
    "DELETE existing" must {
      info("DELETE " + fileUrl)
      whenReady(cl.delete(deleteReq), _timeout) { delResp =>
        "have non-empty response" in {
          assert(delResp.nonEmpty, delResp)
        }
        "contain isExisted = true flag" in {
          val dr = delResp.get
          assert(dr.isExisted, dr)
        }
      }
    }


    "DELETE of already deleted" must {
      info("DELETE " + fileUrl)
      whenReady(cl.delete(deleteReq), _timeout) { del2Resp =>
        "return empty response" in {
          assert(del2Resp.isEmpty, del2Resp)
        }
      }
    }


    "HEAD isExist() for deleted file" must {
      whenReady( cl.isExist(getReq), _timeout ) { isExists =>
        "return false again" in {
          assert(!isExists, getReq)
        }
      }
    }


    "DELETE of never-existed file" must {
      val delReq404 = deleteReq.copy(
        fid = "1234" + deleteReq.fid
      )
      info("DELETE inexisting " + delReq404.toUrl)
      whenReady(cl.delete(delReq404), _timeout) { del3Resp =>
        "return empty response" in {
          assert(del3Resp.isEmpty, del3Resp)
        }
      }
    }


    "GET lookup inexisting volume" must {
      val lcReq = LookupRequest(
        volumeId = 56894590
      )
      whenReady(cl.lookup(lcReq), _timeout) { lcResp =>
        "return not-found response" in {
          assert(lcResp.isLeft, lcResp)
        }
        val l = lcResp.left.get
        "contain requested volumeId" in {
          l.volumeId  shouldBe  lcReq.volumeId
        }
        "contain non-empty error message field" in {
          l.error.length should be >= 4
        }
      }
    }


    "HEAD isExist() for unknown volume id" must {
      val isExReq = getReq.copy(
        fid = "123123" + getReq.fid
      )
      whenReady( cl.isExist(isExReq), _timeout ) { isExist =>
        "return false" in {
          assert(!isExist, isExReq)
        }
      }
    }

  }

}
