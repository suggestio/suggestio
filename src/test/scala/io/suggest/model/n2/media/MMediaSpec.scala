package io.suggest.model.n2.media

import java.util.UUID

import io.suggest.model.n2.media.storage.CassandraStorage
import io.suggest.model.n2.media.storage.swfs.{SwfsStorage_, SwfsStorage}
import io.suggest.swfs.client.proto.fid.Fid
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.09.15 22:51
 * Description: Тесты для модели [[MMedia]].
 */
class MMediaSpec extends PlaySpec with OneAppPerSuite {

  lazy val mMedia = app.injector.instanceOf[MMedia_]
  lazy val swfsStorage = app.injector.instanceOf[SwfsStorage_]

  private def t(mn: MMedia): Unit = {
    mMedia.deserializeOne2(mn)  shouldBe  mn
  }

  private val m1 = {
    MMedia(
      nodeId = "asdarfg9345tkf34gf3g",
      file = MFileMeta(
        mime  = "application/jpeg",
        sizeB = 234525,
        isOriginal = true,
        sha1  = None
      ),
      id = None,
      storage = CassandraStorage(
        rowKey = UUID.randomUUID(),
        qOpt   = Some("asd/asdasda94tieg-e5ge")
      ),
      companion = mMedia
    )
  }

  "JSON" must {
    "handle minimal model" in {
      t(m1)
    }

    "handle full-filled model" in {
      t {
        m1.copy(
          picture = Some(MPictureMeta(
            width = 640,
            height = 480
          )),
          id = Some("asdaffafr23?awf349025234=f3w4fewfgse98ug3jg"),
          versionOpt = Some(45L),
          storage = swfsStorage(
            Fid(
              volumeId = 22,
              fileId = "asdf4390tf34gfs?sd.sdtr4390w=124sf24f"
            )
          )
        )
      }
    }
  }

}
