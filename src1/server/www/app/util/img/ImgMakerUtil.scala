package util.img

import javax.inject.Inject

import io.suggest.model.n2.media.MMediasCache
import models.im._
import models.im.make.MakeResult

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.02.18 15:36
  * Description: Утиль для img-maker'ов.
  */

class ImgMakerUtil @Inject() (
                                mMediasCache              : MMediasCache,
                                implicit private val ec   : ExecutionContext
                              ) {

  /** Вернуть оригинал картинки в качестве результата make.
    *
    * @param dynImgId
    * @return
    */
  def returnImg(dynImgId: MDynImgId): Future[MakeResult] = {
    for {
      mediaOpt <- mMediasCache.getById( dynImgId.mediaId )
    } yield {
      val szReal = mediaOpt
        .get
        .picture.whPx
        .get

      MakeResult(
        szCss       = szReal,
        szReal      = szReal,
        dynCallArgs = MImg3(dynImgId),
        isWide      = false
      )
    }
  }

}
