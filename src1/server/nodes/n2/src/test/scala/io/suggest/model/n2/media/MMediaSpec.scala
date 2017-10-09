package io.suggest.model.n2.media

import io.suggest.common.geom.d2.MSize2di
import io.suggest.model.MockedEsSn
import io.suggest.model.n2.media.storage.swfs.SwfsStorage
import io.suggest.swfs.client.proto.fid.Fid
import org.scalatest.Matchers._
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.09.15 22:51
 * Description: Тесты для модели [[MMedia]].
 */
class MMediaSpec extends PlaySpec with OneAppPerSuite with MockedEsSn {

  private lazy val mMedias = app.injector.instanceOf[MMedias]
  private lazy val MMediaImplicits = mMedias.Implicits

  private def t(mn: MMedia): Unit = {
    import MMediaImplicits.mockPlayDocRespEv
    mMedias.deserializeOne2(mn)  shouldBe  mn
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
      storage = SwfsStorage(
        Fid(
          volumeId = 22,
          fileId = "asdf4390tf34gfs?sd.sdtr4390w=124sf24f"
        )
      )
    )
  }

  "JSON" must {
    "handle minimal model" in {
      t(m1)
    }

    "handle full-filled model" in {
      t {
        m1.copy(
          picture = MPictureMeta(
            whPx = Some(MSize2di(
              width  = 640,
              height = 480
            ))
          ),
          id = Some("asdaffafr23?awf349025234=f3w4fewfgse98ug3jg"),
          versionOpt = Some(45L)
        )
      }
    }
  }

}
