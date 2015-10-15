package util.acl

import controllers.SioController
import io.suggest.di.{IEsClient, IExecutionContext}
import models.MAdnNodeGeo
import models.req.SioReqMd
import scala.concurrent.Future
import play.api.mvc.{Request, ActionBuilder, Result}
import util.acl.PersonWrapper.PwOpt_t

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.10.15 14:38
 * Description: Аддон для контроллера для поддержки смеси IsSuperuser + доступ к AdnGeo по id.
 */
trait IsSuperuserAdnGeo
  extends SioController
  with IExecutionContext
  with IEsClient
  with IsSuperuserUtilCtl
{

  /** Нужно админство и доступ к существующей географии узла по geo id. */
  trait IsSuperuserAdnGeoBase
    extends ActionBuilder[AdnGeoRequest]
    with IsSuperuserUtil
  {

    /** id гео-шейпа. */
    def geoId: String

    /** id родительского узла, т.к. в MAdnNodeGeo исторически царит parent-child. */
    def adnId: String

    override def invokeBlock[A](request: Request[A], block: (AdnGeoRequest[A]) => Future[Result]): Future[Result] = {
      val pwOpt = PersonWrapper.getFromRequest(request)
      if (PersonWrapper isSuperuser pwOpt) {
        val srmFut = SioReqMd.fromPwOpt(pwOpt)
        MAdnNodeGeo.get(geoId, adnId) flatMap {
          case Some(geo) =>
            srmFut flatMap { srm =>
              val req1 = AdnGeoRequest(geo, pwOpt, request, srm)
              block(req1)
            }

          case _ =>
            geoNotFound
        }
      } else {
        onUnauthFut(request, pwOpt)
      }
    }

    def geoNotFound: Future[Result] = {
      val res = NotFound(s"Geography $geoId not found for node $adnId.")
      Future successful res
    }
  }


  sealed abstract class IsSuperuserAdnGeoAbstract
    extends IsSuperuserAdnGeoBase
    with ExpireSession[AdnGeoRequest]


  /** ACL для su-экшенов, связанных с географией узлов. */
  case class IsSuperuserAdnGeo(override val geoId: String, override val adnId: String)
    extends IsSuperuserAdnGeoAbstract

  /** ACL для исполнения su-экшенов, связанных с администрирования географии узлов,
    * и содержащих в ответе формы, защищенные от CSRF. */
  case class IsSuperuserAdnGeoGet(override val geoId: String, override val adnId: String)
    extends IsSuperuserAdnGeoAbstract
    with CsrfGet[AdnGeoRequest]

  /** ACL для POST'а от суперюзера географии с защитой от CSRF. */
  case class IsSuperuserAdnGeoPost(override val geoId: String, override val adnId: String)
    extends IsSuperuserAdnGeoAbstract
    with CsrfPost[AdnGeoRequest]

}


case class AdnGeoRequest[A](
  adnGeo    : MAdnNodeGeo,
  pwOpt     : PwOpt_t,
  request   : Request[A],
  sioReqMd  : SioReqMd
)
  extends AbstractRequestWithPwOpt(request)

