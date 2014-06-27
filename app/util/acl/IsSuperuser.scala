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
    http404Fut(request)
  }

}
object IsSuperuser extends IsSuperuserAbstract with ExpireSession[AbstractRequestWithPwOpt]

/** Часто нужно админить узлы рекламной сети. Тут комбинация IsSuperuser + IsAdnAdmin. */
trait IsSuperuserAdnNodeAbstract extends ActionBuilder[AbstractRequestForAdnNode] {
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
          Future successful Results.NotFound("Adn node not found: " + adnId)
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
  extends IsSuperuserAdnNodeAbstract
  with ExpireSession[AbstractRequestForAdnNode]



case class FeeTariffRequest[A](
  tariff: MBillTariffFee,
  contract: MBillContract,
  pwOpt: PwOpt_t,
  request: Request[A],
  sioReqMd: SioReqMd
) extends AbstractRequestWithPwOpt[A](request)

trait IsSuperuserFeeTariffContractAbstract extends ActionBuilder[FeeTariffRequest] {
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
  extends IsSuperuserFeeTariffContractAbstract
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

trait IsSuperuserContractAbstract extends ActionBuilder[ContractRequest] {
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
  extends IsSuperuserContractAbstract
  with ExpireSession[ContractRequest]

