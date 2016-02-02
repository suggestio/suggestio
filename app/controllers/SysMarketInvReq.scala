package controllers

import com.google.inject.Inject
import controllers.sysctl.{SmSendEmailInvite, SysMarketUtil}
import io.suggest.common.fut.FutureUtil
import io.suggest.model.n2.edge.MNodeEdges
import models._
import models.mproj.ICommonDi
import models.req.IMirReq
import models.usr.EmailActivation
import play.api.mvc.{AnyContent, Result}
import util.PlayMacroLogsImpl
import util.acl._
import util.mail.IMailerWrapper
import views.html.sys1.market.invreq._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.06.14 10:16
 * Description: Sys-контроллер для обработки входящих инвайтов.
 * v1: Отображать список, отображать каждый элемент, удалять.
 * v2: Нужен многошаговый и удобный мастер создания узлов со всеми контрактами и инвайтами, отметками о ходе
 * обработки запроса и т.д.
 */
class SysMarketInvReq @Inject() (
  sysMarketUtil                   : SysMarketUtil,
  override val mInviteRequest     : MInviteRequests,
  override val mailer             : IMailerWrapper,
  override val mCommonDi          : ICommonDi
)
  extends SioControllerImpl
  with PlayMacroLogsImpl
  with SmSendEmailInvite
  with IsSuperuserMir
  with IsSuperuser
{

  import LOGGER._
  import mCommonDi._

  def MIRS_FETCH_COUNT = 300

  /** Макс.кол-во попыток сохранения экземлпяров MInviteRequest. */
  def UPDATE_RETRIES_MAX = 5

  /** Вернуть страницу, отображающую всю инфу по текущему состоянию подсистемы IR.
    * Список реквестов, в первую очередь. */
  def index = IsSuperuser.async { implicit request =>
    mInviteRequest.getAll(MIRS_FETCH_COUNT) flatMap { mirs =>
      val thisCount = mirs.size
      val allCountFut: Future[Long] = if (thisCount >= MIRS_FETCH_COUNT) {
        mInviteRequest.countAll
      } else {
        Future successful thisCount.toLong
      }
      allCountFut.map { allCount =>
        Ok(irIndexTpl(mirs, thisCount, allCount))
      }
    }
  }


  /** Отрендерить страницу одного инвайт-реквеста. */
  def showIR(mirId: String) = IsSuperuserMir(mirId).async { implicit request =>
    import request.mir
    val nodeOptFut = getNodeOptFut(mir)
    val eactOptFut = getEactOptFut(mir)
    for {
      nodeOpt <- nodeOptFut
      eactOpt <- eactOptFut
    } yield {
      Ok(irShowOneTpl(mir, nodeOpt, eactOpt))
    }
  }


  /** Удалить один MIR. */
  def deleteIR(mirId: String) = IsSuperuserMir(mirId).async { implicit request =>
    import request.mir
    mir.eraseResources flatMap { _ =>
      mir.delete map { isDeleted =>
        val flasher: (String, String) = if (isDeleted) {
          FLASH.SUCCESS -> "Запрос на подключение удалён."
        } else {
          FLASH.ERROR   -> "Возникли какие-то проблемы с удалением."
        }
        Redirect(routes.SysMarketInvReq.index())
          .flashing(flasher)
      }
    }
  }


  /** Запрос страницы с формой редактирования заготовки узла. */
  def nodeEdit(mirId: String) = isNodeLeftOrMissing(mirId).async { implicit request =>
    nodeEditBody(request.mir, Ok)
  }

  private def nodeEditBody(mir: MInviteRequest, rs: Status)
                          (implicit request: IMirReq[AnyContent]): Future[Result] = {
    getNodeOptFut(mir) map { adnNodeOpt =>
      val form0 = sysMarketUtil.adnNodeFormM
      val formFilled = adnNodeOpt.fold(form0)(form0.fill)
      val html = nodeEditTpl(mir, adnNodeOpt, formFilled)
      rs(html)
    }
  }

  /** Сабмит формы редактирования заготовки рекламного узла.
    * Здесь может быть подвох в поле companyId, которое может содержать пустое значение. */
  def nodeEditFormSubmit(mirId: String) = isNodeLeftOrMissing(mirId).async { implicit request =>
    import request.mir
    sysMarketUtil.adnNodeFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"nodeEditFormSubmit($mirId): Failed to bind form:\n${formatFormErrors(formWithErrors)}")
        nodeEditBody(mir, NotAcceptable)
      },
      {adnNode2 =>
        val updFut = mInviteRequest.tryUpdate(mir) { mir0 =>
          val adnNode3 = mir0.adnNode.fold [MNode] (adnNode2) { adnNodeEith =>
            sysMarketUtil.updateAdnNode(adnNodeEith.left.get, adnNode2)
          }
          mir0.copy(
            adnNode = Some(Left(adnNode3))
          )
        }
        for (_ <- updFut) yield {
          rdrToIr(mirId, "Шаблон узла сохранён.")
        }
      }
    )
  }

  /** sio-админ приказывает развернуть рекламный узел. */
  def nodeInstallSubmit(mirId: String) = isNodeLeft(mirId).async { implicit request =>
    import request.mir
    val adnNode0 = mir.adnNode.get.left.get
    // Определить, существовал ли узел. Если нет, то при ошибке обновления MIR новый созданный узел будет удалён.
    val previoslyExistedFut = adnNode0.id.fold [Future[Boolean]]
      { Future successful false }
      { MNode.isExist }
    previoslyExistedFut flatMap { previoslyExisted =>
      val p = MPredicates.WcLogo
      val adnNode = adnNode0.copy(
        edges = adnNode0.edges.copy(
          out = {
            val edges0Iter = adnNode0.edges
              .withoutPredicateIter(p)
            MNodeEdges.edgesToMap1(edges0Iter)
          }
        )
      )
      // Запуск сохранения узла.
      adnNode.save flatMap { adnId =>
        // Узел сохранён. Пора обновить экземпляр MIR
        val updateFut = mInviteRequest.tryUpdate(mir) { mir0 =>
          mir0.copy(
            adnNode = Some(Right(adnId))
          )
        }
        if (previoslyExisted) {
          updateFut onFailure { case ex =>
            warn(s"nodeInstallSubmit($mirId): Rollbacking node[$adnId] installation due to exception during MIR update")
            MNode.deleteById(adnId)
          }
        }
        for (_ <- updateFut) yield {
          rdrToIr(mirId, "Рекламный узел добавлен в систему.")
        }
      }
    }
  }

  /** sio-админ приказывает деинсталлировать узел. */
  def nodeUninstallSubmit(mirId: String) = isNodeRight(mirId).async { implicit request =>
    import request.mir
    val adnId = mir.adnNode.get.right.get
    MNode.getById(adnId) flatMap {
      case Some(adnNode) =>
        adnNode.delete flatMap {
          case true =>
            // Узел удалён. Пора залить его в состояние MIR
            val updateFut = mInviteRequest.tryUpdate(mir) { mir0 =>
              mir0.copy(
                adnNode = Some(Left(adnNode))
              )
            }
            updateFut onFailure { case ex =>
              adnNode.save
              warn(s"nodeUnistallSubmit($mirId): rollbacking node[$adnId] deinstallation due to exception.", ex)
            }
            for (_ <- updateFut) yield {
              rdrToIr(mirId, "Рекламный узел деинсталлирован назад в шаблон узла. Это может нарушить ссылочную целостность.")
            }
          case false =>
            ExpectationFailed(s"Cannot delete node[$adnId], proposed for installation.")
        }

      case None =>
        NotFound(s"Node $adnId not exist, but it should.")
    }
  }


  /** sio-админ командует создать реквест и отправить письмо на email. */
  def eactInstallSubmit(mirId: String) = isNodeEactLeft(mirId).async { implicit request =>
    import request.mir
    val adnId = mir.adnNode.get.right.get
    val adnNodeOptFut = mNodeCache.getById(adnId)
    val eact = mir.emailAct.get.left.get.copy(
      key = adnId
    )
    val previoslyExistedFut = eact.id.fold [Future[Boolean]]
      { Future successful false }
      { EmailActivation.isExist }
    previoslyExistedFut flatMap { previoslyExisted =>
      adnNodeOptFut flatMap {
        case Some(adnNode) =>
          eact.save flatMap { eaId =>
            val ea1 = eact.copy(id = Option(eaId))
            val sendEmailFut = Future {
              sendEmailInvite(ea1, adnNode)
            }
            val updFut = sendEmailFut flatMap { _ =>
              // Пора переключить состояние mir
              mInviteRequest.tryUpdate(mir) { mir0 =>
                mir0.copy(
                  emailAct = Some(Right(eaId))
                )
              }
            }
            if (!previoslyExisted) {
              updFut onFailure { case ex =>
                ea1.delete
                warn(s"eactInstallSubmit($mirId): Rollbacking save of eact[$eaId] due to exception.", ex)
              }
            }
            for (_ <- updFut) yield {
              rdrToIr(mirId, s"Сохранён запрос на активацию. Письмо отправлено на ${ea1.email}.")
            }
          }

        case None => NotFound("Node marked as installed, but not found: " + adnId)
      }
    }
  }


  /** Общий код редиректа назад на страницу обработки реквеста вынесен сюда. */
  private def rdrToIr(mirId: String, flashMsg: String, flashCode: String = FLASH.SUCCESS) = {
    Redirect( routes.SysMarketInvReq.showIR(mirId) )
      .flashing(flashCode -> flashMsg)
  }

  private def isNodeLeft(mirId: String) = new IsSuperuserMir(mirId) {
     override def mirStateInvalidMsg: String = {
      "MIR.node is installed, but action possible only for NOT installed node. Go back and press F5."
    }
    override def isMirStateOk(mir: MInviteRequest): Boolean = {
      super.isMirStateOk(mir) && {
        mir.adnNode.exists(_.isLeft)
      }
    }
  }

  private def isNodeLeftOrMissing(mirId: String) = new IsSuperuserMir(mirId) {
     override def mirStateInvalidMsg: String = {
      "MIR.node is installed, but action possible only for NOT installed node. Go back and press F5."
    }
    override def isMirStateOk(mir: MInviteRequest): Boolean = {
      super.isMirStateOk(mir) && {
        mir.adnNode.isEmpty  ||  mir.adnNode.exists(_.isLeft)
      }
    }
  }


  private def isNodeRight(mirId: String) = new IsSuperuserMir(mirId) {
    override def mirStateInvalidMsg: String = {
      "MIR.node is NOT installed, but action possible only for already installed node. Go back and press F5."
    }
    override def isMirStateOk(mir: MInviteRequest): Boolean = {
      super.isMirStateOk(mir) && {
        mir.adnNode.exists(_.isRight)
      }
    }
  }

  private def isNodeEactLeft(mirId: String) = new IsSuperuserMir(mirId) {
    override def mirStateInvalidMsg: String = {
      "MIR.eact is already installed OR node NOT installed, but action possible only for already NOT-installed EAct and installed node. Go back and press F5."
    }
    override def isMirStateOk(mir: MInviteRequest): Boolean = {
      super.isMirStateOk(mir) && {
        mir.adnNode.exists(_.isRight) && mir.emailAct.exists(_.isLeft)
      }
    }
  }

  private def getNodeOptFut(mir: MInviteRequest): Future[Option[MNode]] = {
    FutureUtil.optFut2futOpt(mir.adnNode) { nodeEith =>
      nodeEith.fold[Future[Option[MNode]]](
        { an => Future successful Option(an)},
        { adnId => mNodeCache.getById(adnId)}
      )
    }
  }

  private def getEactOptFut(mir: MInviteRequest): Future[Option[EmailActivation]] = {
    FutureUtil.optFut2futOpt(mir.emailAct) { eactEith =>
      eactEith.fold[Future[Option[EmailActivation]]](
        { eact => Future successful Option(eact)},
        { EmailActivation.getById(_) }
      )
    }
  }

}
