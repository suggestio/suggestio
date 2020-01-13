package util.img

import io.suggest.es.model.EsModel
import io.suggest.n2.edge.MPredicates
import javax.inject.Inject
import io.suggest.n2.node.{MNodeTypes, MNodes}
import models.im._
import models.im.make.MakeResult
import japgolly.univeq._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.02.18 15:36
  * Description: Утиль для img-maker'ов.
  */

final class ImgMakerUtil @Inject() (
                                     esModel                   : EsModel,
                                     mNodes                    : MNodes,
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
      mediaOpt <- mNodes.getByIdCache( dynImgId.mediaId )
    } yield {
      val szReal = (for {
        mediaNode <- mediaOpt
        if mediaNode.common.ntype ==* MNodeTypes.Media.Image
        fileEdge  <- mediaNode.edges
          .withPredicateIter( MPredicates.File )
          .nextOption()
        edgeMedia <- fileEdge.media
        whPx      <- edgeMedia.picture.whPx
      } yield whPx)
        // TODO А если нет размера вдруг?
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
