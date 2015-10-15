package util.acl

import controllers.SioController
import io.suggest.di.{IEsClient, IExecutionContext}
import models.im.MGallery
import models.req.SioReqMd
import scala.concurrent.Future
import play.api.mvc.{Request, ActionBuilder, Result}
import util.acl.PersonWrapper.PwOpt_t

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.10.15 14:31
 * Description: Аддон для контроллеров для IsSuperuser + доступ к галереи по id.
 */

trait IsSuperuserGallery
  extends SioController
  with IExecutionContext
  with IEsClient
  with IsSuperuserUtilCtl
{

  /** Логика проверки живёт в этом трейте. */
  trait IsSuperuserGalleryBase
    extends ActionBuilder[GalleryRequest]
    with IsSuperuserUtil
  {

    /** id запрашиваемой галлереи. */
    def galId: String

    override def invokeBlock[A](request: Request[A], block: (GalleryRequest[A]) => Future[Result]): Future[Result] = {
      val pwOpt = PersonWrapper.getFromRequest(request)
      if (PersonWrapper isSuperuser pwOpt) {
        val srmFut = SioReqMd.fromPwOpt(pwOpt)
        MGallery.getById(galId) flatMap {
          case Some(gallery) =>
            srmFut flatMap { srm =>
              val req1 = GalleryRequest(gallery, pwOpt, request, srm)
              block(req1)
            }

          case None =>
            galNotFound
        }
      } else {
        supOnUnauthFut(request, pwOpt)
      }
    }

    def galNotFound: Future[Result] = {
      val res = NotFound(s"Gallery $galId not found.")
      Future successful res
    }
  }


  case class IsSuperuserGallery(override val galId: String)
    extends IsSuperuserGalleryBase
    with ExpireSession[GalleryRequest]

}


/** Реквест запроса к галерее. */
case class GalleryRequest[A](
  gallery   : MGallery,
  pwOpt     : PwOpt_t,
  request   : Request[A],
  sioReqMd  : SioReqMd
)
  extends AbstractRequestWithPwOpt(request)
