package util.acl

import play.api.mvc._
import scala.concurrent.Future
import util.PlayMacroLogsImpl
import scala.Some
import play.api.mvc.Result
import controllers.Application.http404Fut
import util.acl.PersonWrapper.PwOpt_t
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models._
import util.SiowebEsUtil.client
import play.api.db.DB
import play.api.Play.current

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.13 13:48
 * Description: Суперпользователи сервиса имеют все необходимые права, в т.ч. для доступа в /sys/.
 */
trait IsSuperuserAbstract extends ActionBuilder[AbstractRequestWithPwOpt] with PlayMacroLogsImpl {
  import LOGGER._
  
  override def invokeBlock[A](request: Request[A], block: (AbstractRequestWithPwOpt[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    pwOpt match {
      case Some(pw) if pw.isSuperuser =>
        val sioReqMdFut = SioReqMd.fromPwOpt(pwOpt)
        trace(s"for user ${pw.personId} :: ${request.method} ${request.path}")
        sioReqMdFut flatMap { srm =>
          block(RequestWithPwOpt(pwOpt, request, srm))
        }

      case _ => onUnauthFut(request, pwOpt)
    }
  }

  def onUnauthFut(request: RequestHeader, pwOpt: PwOpt_t): Future[Result] = {
    import request._
    warn(s"$method $path <- BLOCKED access to hidden/priveleged place from $remoteAddress user=$pwOpt")
    onUnauthResult(request, pwOpt)
  }

  def onUnauthResult(request: RequestHeader, pwOpt: PwOpt_t): Future[Result]
}
object IsSuperuser extends IsSuperuserAbstract with ExpireSession[AbstractRequestWithPwOpt] {
  override def onUnauthResult(request: RequestHeader, pwOpt: PwOpt_t): Future[Result] = {
    IsAuth.onUnauth(request)
  }
}
object IsSuperuserOr404 extends IsSuperuserAbstract with ExpireSession[AbstractRequestWithPwOpt] {
  override def onUnauthResult(request: RequestHeader, pwOpt: PwOpt_t): Future[Result] = {
    http404Fut(request)
  }
}



object IsSuperuserAdnNode {
  def nodeNotFound(adnId: String) = Results.NotFound("Adn node not found: " + adnId)
}
/** Часто нужно админить узлы рекламной сети. Тут комбинация IsSuperuser + IsAdnAdmin. */
trait IsSuperuserAdnNodeBase extends ActionBuilder[AbstractRequestForAdnNode] {
  import IsSuperuserAdnNode._

  def adnId: String
  override def invokeBlock[A](request: Request[A], block: (AbstractRequestForAdnNode[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    if (PersonWrapper.isSuperuser(pwOpt)) {
      val sioReqMdFut = SioReqMd.fromPwOpt(pwOpt)
      MAdnNodeCache.getById(adnId) flatMap {
        case Some(adnNode) =>
          sioReqMdFut flatMap { srm =>
            block(RequestForAdnNodeAdm(adnNode, isMyNode = true, request, pwOpt, srm))
          }
        case None =>
          Future successful nodeNotFound(adnId)
      }
    } else {
      IsSuperuser.onUnauthFut(request, pwOpt)
    }
  }
}
/**
 * Часто нужно админить узлы рекламной сети. Тут комбинация IsSuperuser + IsAdnAdmin.
 * @param adnId
 */
case class IsSuperuserAdnNode(adnId: String)
  extends IsSuperuserAdnNodeBase
  with ExpireSession[AbstractRequestForAdnNode]



case class FeeTariffRequest[A](
  tariff: MBillTariffFee,
  contract: MBillContract,
  pwOpt: PwOpt_t,
  request: Request[A],
  sioReqMd: SioReqMd
) extends AbstractRequestWithPwOpt[A](request)

trait IsSuperuserFeeTariffContractBase extends ActionBuilder[FeeTariffRequest] {
  def tariffId: Int
  override def invokeBlock[A](request: Request[A], block: (FeeTariffRequest[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    if (PersonWrapper.isSuperuser(pwOpt)) {
      val sioReqMdFut = SioReqMd.fromPwOpt(pwOpt)
      val (tariff, contract) = DB.withConnection { implicit c =>
        val _tariff = MBillTariffFee.getById(tariffId).get
        val _contract = MBillContract.getById(_tariff.contractId).get
        _tariff -> _contract
      }
      sioReqMdFut flatMap { srm =>
        val req1 = FeeTariffRequest(tariff, contract, pwOpt, request, srm)
        block(req1)
      }
    } else {
      IsSuperuser.onUnauthFut(request, pwOpt)
    }
  }
}
case class IsSuperuserFeeTariffContract(tariffId: Int)
  extends IsSuperuserFeeTariffContractBase
  with ExpireSession[FeeTariffRequest]



case class StatTariffRequest[A](
  tariff: MBillTariffStat,
  contract: MBillContract,
  pwOpt: PwOpt_t,
  request: Request[A],
  sioReqMd: SioReqMd
) extends AbstractRequestWithPwOpt[A](request)

trait IsSuperuserStatTariffContractAbstract extends ActionBuilder[StatTariffRequest] {
  def tariffId: Int
  override def invokeBlock[A](request: Request[A], block: (StatTariffRequest[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    if (PersonWrapper.isSuperuser(pwOpt)) {
      val sioReqMdFut = SioReqMd.fromPwOpt(pwOpt)
      val (tariff, contract) = DB.withConnection { implicit c =>
        val _tariff = MBillTariffStat.getById(tariffId).get
        val _contract = MBillContract.getById(_tariff.contractId).get
        _tariff -> _contract
      }
      sioReqMdFut flatMap { srm =>
        val req1 = StatTariffRequest(tariff, contract, pwOpt, request, srm)
        block(req1)
      }
    } else {
      IsSuperuser.onUnauthFut(request, pwOpt)
    }
  }
}
case class IsSuperuserStatTariffContract(tariffId: Int)
  extends IsSuperuserStatTariffContractAbstract
  with ExpireSession[StatTariffRequest]



case class ContractRequest[A](
  contract: MBillContract,
  pwOpt: PwOpt_t,
  request: Request[A],
  sioReqMd: SioReqMd
) extends AbstractRequestWithPwOpt[A](request)

trait IsSuperuserContractBase extends ActionBuilder[ContractRequest] {
  def contractId: Int
  override def invokeBlock[A](request: Request[A], block: (ContractRequest[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    if (PersonWrapper.isSuperuser(pwOpt)) {
      val sioReqMdFut = SioReqMd.fromPwOpt(pwOpt)
      val contract = DB.withConnection { implicit c =>
        MBillContract.getById(contractId).get
      }
      sioReqMdFut flatMap { srm =>
        val req1 = ContractRequest(contract, pwOpt, request, srm)
        block(req1)
      }
    } else {
      IsSuperuser.onUnauthFut(request, pwOpt)
    }
  }
}
case class IsSuperuserContract(contractId: Int)
  extends IsSuperuserContractBase
  with ExpireSession[ContractRequest]



case class CompanyRequest[A](
  company: MCompany,
  pwOpt: PwOpt_t,
  request: Request[A],
  sioReqMd: SioReqMd
) extends AbstractRequestWithPwOpt[A](request)

object IsSuperuserCompany {
  def companyNotFound(companyId: String) = Results.NotFound("Company not found: " + companyId)
}
trait IsSuperuserCompanyBase extends ActionBuilder[CompanyRequest] {
  import IsSuperuserCompany._
  def companyId: String
  override def invokeBlock[A](request: Request[A], block: (CompanyRequest[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    if (PersonWrapper.isSuperuser(pwOpt)) {
      MCompany.getById(companyId) flatMap {
        case Some(company) =>
          SioReqMd.fromPwOpt(pwOpt) flatMap { srm =>
            val req1 = CompanyRequest(company, pwOpt, request, srm)
            block(req1)
          }

        case None =>
          Future successful companyNotFound(companyId)
      }
    } else {
      IsSuperuser.onUnauthFut(request, pwOpt)
    }
  }

}
case class IsSuperuserCompany(companyId: String)
  extends IsSuperuserCompanyBase
  with ExpireSession[CompanyRequest]



// ActionBuilder и его детали для экшенов, работающих с экземпляром MInviteRequest.
case class MirRequest[A](
  mir: MInviteRequest,
  pwOpt: PwOpt_t,
  request: Request[A],
  sioReqMd: SioReqMd
) extends AbstractRequestWithPwOpt(request)
object IsSuperuserMir {
  def mirNotFound(mirId: String) = Results.NotFound("Invite request not found: " + mirId)
}
trait IsSuperuserMirBase extends ActionBuilder[MirRequest] {
  import IsSuperuserMir._
  def mirId: String

  def isMirStateOk(mir: MInviteRequest) = true
  def mirStateInvalidMsg = s"MIR[$mirId] has impossible state for this action."
  def mirStateInvalid = Results.ExpectationFailed(mirStateInvalidMsg)

  override def invokeBlock[A](request: Request[A], block: (MirRequest[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    if (PersonWrapper isSuperuser pwOpt) {
      MInviteRequest.getById(mirId) flatMap {
        case Some(mir) =>
          if (isMirStateOk(mir)) {
            SioReqMd.fromPwOpt(pwOpt) flatMap { srm =>
              val req1 = MirRequest(mir, pwOpt, request, srm)
              block(req1)
            }
          } else {
            Future successful mirStateInvalid
          }

        case None =>
          Future successful mirNotFound(mirId)
      }
    } else {
      IsSuperuser.onUnauthFut(request, pwOpt)
    }
  }
}
case class IsSuperuserMir(mirId: String) extends IsSuperuserMirBase with ExpireSession[MirRequest]

