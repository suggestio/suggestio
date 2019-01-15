package util.img

import io.suggest.es.model.EsModel
import javax.inject.Inject
import io.suggest.model.n2.media.MMedias
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
                                esModel                   : EsModel,
                                mMedias                   : MMedias,
                                implicit private val ec   : ExecutionContext
                              ) {

  import esModel.api._

  /** Вернуть оригинал картинки в качестве результата make.
    *
    * @param dynImgId
    * @return
    */
  def returnImg(dynImgId: MDynImgId): Future[MakeResult] = {
    for {
      mediaOpt <- mMedias.getByIdCache( dynImgId.mediaId )
    } yield {
      val szReal = mediaOpt
        .get
        .picture.whPx
        .get

      MakeResult(
        szCss       = szReal,
        szReal      = szReal,
        dynCallArgs = MImg3(dynImgId),
        isWide      = false,
        isFake      = true
      )
    }
  }

}
