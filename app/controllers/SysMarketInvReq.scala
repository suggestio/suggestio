package controllers

import com.google.inject.Inject
import controllers.sysctl.SmSendEmailInvite
import models.usr.EmailActivation
import play.api.i18n.MessagesApi
import play.api.mvc.{AnyContent, Result}
import play.twirl.api.Html
import util.acl._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import models._
import util.mail.IMailerWrapper
import scala.concurrent.Future
import views.html.sys1.market.invreq._
import util.PlayMacroLogsImpl
import sysctl.SysMarketUtil._

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
  override val messagesApi: MessagesApi,
  override val mailer: IMailerWrapper
)
  extends SioControllerImpl with PlayMacroLogsImpl with SmSendEmailInvite
{

  import LOGGER._

  val MIRS_FETCH_COUNT = 300

  /** Макс.кол-во попыток сохранения экземлпяров MInviteRequest. */
  val UPDATE_RETRIES_MAX = 5

  /** Вернуть страницу, отображающую всю инфу по текущему состоянию подсистемы IR.
    * Список реквестов, в первую очередь. */
  def index = IsSuperuser.async { implicit request =>
    MInviteRequest.getAll(MIRS_FETCH_COUNT) flatMap { mirs =>
      val thisCount = mirs.size
      val allCountFut: Future[Long] = if (thisCount >= MIRS_FETCH_COUNT) {
        MInviteRequest.countAll
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
      mcOpt   <- getCompanyOptFut(mir)
      nodeOpt <- nodeOptFut
      eactOpt <- eactOptFut
    } yield {
      Ok(irShowOneTpl(mir, mcOpt, nodeOpt, eactOpt))
    }
  }


  /** Удалить один MIR. */
  def deleteIR(mirId: String) = IsSuperuserMir(mirId).async { implicit request =>
    import request.mir
    mir.eraseResources flatMap { _ =>
      mir.delete map { isDeleted =>
        val flasher: (String, String) = if (isDeleted) {
          "success" -> "Запрос на подключение удалён."
        } else {
          "error"   -> "Возникли какие-то проблемы с удалением."
        }
        Redirect(routes.SysMarketInvReq.index())
          .flashing(flasher)
      }
    }
  }


  /** Запрос страницы с формой редактирования заготовки узла. */
  def nodeEdit(mirId: String) = isNodeLeftOrMissing(mirId).async { implicit request =>
    nodeEditBody(request.mir)(Ok(_))
  }

  private def nodeEditBody(mir: MInviteRequest)(onSuccess: Html => Result)
                          (implicit request: MirRequest[AnyContent]): Future[Result] = {
    getNodeOptFut(mir) map { adnNodeOpt =>
      val formFilled = adnNodeOpt match {
        case Some(adnNode) =>
          adnNodeFormM fill adnNode
        case None =>
          adnNodeFormM
      }
      Ok(nodeEditTpl(mir, adnNodeOpt, formFilled))
    }
  }

  /** Сабмит формы редактирования заготовки рекламного узла.
    * Здесь может быть подвох в поле companyId, которое может содержать пустое значение. */
  def nodeEditFormSubmit(mirId: String) = isNodeLeftOrMissing(mirId).async { implicit request =>
    import request.mir
    adnNodeFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"nodeEditFormSubmit($mirId): Failed to bind form:\n${formatFormErrors(formWithErrors)}")
        nodeEditBody(mir)(NotAcceptable(_))
      },
      {adnNode2 =>
        MInviteRequest.tryUpdate(mir) { mir0 =>
          val adnNode3 = mir0.adnNode.fold [MAdnNode] (adnNode2) { adnNodeEith =>
            updateAdnNode(adnNodeEith.left.get, adnNode2)
          }
          mir0.copy(
            adnNode = Some(Left(adnNode3))
          )
        } map { _ =>
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
      { MAdnNode.isExist }
    val waSavedIdOptFut = mir.waOpt.fold [Future[Option[String]]]
      { Future successful None }
      { _.fold[Future[Option[String]]] (
        { wa => wa.save.map(Some.apply) },
        { waId => Future successful Option(waId) }
      )}
    previoslyExistedFut flatMap { previoslyExisted =>
      waSavedIdOptFut flatMap { waSavedIdOpt =>
        val adnNode = adnNode0.copy(
          meta = adnNode0.meta.copy(
            welcomeAdId = waSavedIdOpt
          )
        )
        // Запуск сохранения узла.
        val resultFut = adnNode.save flatMap { adnId =>
          // Узел сохранён. Пора обновить экземпляр MIR
          val updateFut = MInviteRequest.tryUpdate(mir) { mir0 =>
            mir0.copy(
              adnNode = Some(Right(adnId)),
              waOpt = waSavedIdOpt.map(Right.apply)
            )
          }
          if (previoslyExisted) {
            updateFut onFailure { case ex =>
              warn(s"nodeInstallSubmit($mirId): Rollbacking node[$adnId] installation due to exception during MIR update")
              MAdnNode.deleteById(adnId)
            }
          }
          updateFut map { _ =>
            rdrToIr(mirId, "Рекламный узел добавлен в систему.")
          }
        }
        // При ошибке стереть свежесохранённый инстанс welcomeAdOpt
        if ( waSavedIdOpt.isDefined  &&  mir.waOpt.exists(_.isLeft) ) {
          resultFut onFailure { case ex =>
            MWelcomeAd.deleteById(waSavedIdOpt.get)
            warn(s"nodeInstallSubmit($mirId): Rollbacking welcome ad saving due to exception while MIR update.")
          }
        }
        resultFut
      }
    }
  }

  /** sio-админ приказывает деинсталлировать узел. */
  def nodeUninstallSubmit(mirId: String) = isNodeRight(mirId).async { implicit request =>
    import request.mir
    val adnId = mir.adnNode.get.right.get
    MAdnNode.getById(adnId) flatMap {
      case Some(adnNode) =>
        adnNode.delete flatMap {
          case true =>
            // Узел удалён. Пора залить его в состояние MIR
            val updateFut = MInviteRequest.tryUpdate(mir) { mir0 =>
              mir0.copy(
                adnNode = Some(Left(adnNode))
              )
            }
            updateFut onFailure { case ex =>
              adnNode.save
              warn(s"nodeUnistallSubmit($mirId): rollbacking node[$adnId] deinstallation due to exception.", ex)
            }
            updateFut map { _ =>
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
    val adnNodeOptFut = MAdnNodeCache.getById(adnId)
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
              MInviteRequest.tryUpdate(mir) { mir0 =>
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
            updFut map { _ =>
              rdrToIr(mirId, s"Сохранён запрос на активацию. Письмо отправлено на ${ea1.email}.")
            }
          }

        case None => NotFound("Node marked as installed, but not found: " + adnId)
      }
    }
  }


  /** Общий код редиректа назад на страницу обработки реквеста вынесен сюда. */
  private def rdrToIr(mirId: String, flashMsg: String, flashCode: String = "success") = {
    Redirect( routes.SysMarketInvReq.showIR(mirId) )
      .flashing(flashCode -> flashMsg)
  }


  private def isCompanyLeft(mirId: String) = new IsSuperuserMir(mirId) {
    override def mirStateInvalidMsg: String = {
      "MIR.company already installed, but action requested possible only on not-installed company. Go back and press F5."
    }
    override def isMirStateOk(mir: MInviteRequest): Boolean = {
      super.isMirStateOk(mir) && {
        mir.company.isLeft
      }
    }
  }

  private def isCompanyRight(mirId: String) = new IsSuperuserMir(mirId) {
    override def mirStateInvalidMsg: String = {
      "MIR.company NOT installed, but action requested possible only for installed company. Go back and press F5."
    }
    override def isMirStateOk(mir: MInviteRequest): Boolean = {
      super.isMirStateOk(mir) && {
        mir.company.isRight
      }
    }
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

  /** Прочитать MCompany из реквеста или из модели. */
  private def getCompanyOptFut(mir: MInviteRequest): Future[Option[MCompany]] = {
    mir.company.fold[Future[Option[MCompany]]](
      { mc => Future successful Option(mc) },
      { mcId => MCompany.getById(mcId) }
    )
  }

  private def getNodeOptFut(mir: MInviteRequest): Future[Option[MAdnNode]] = {
    mir.adnNode.fold [Future[Option[MAdnNode]]] (Future successful None) { nodeEith =>
      nodeEith.fold[Future[Option[MAdnNode]]](
        { an => Future successful Option(an)},
        { adnId => MAdnNodeCache.getById(adnId)}
      )
    }
  }

  private def getEactOptFut(mir: MInviteRequest): Future[Option[EmailActivation]] = {
    mir.emailAct.fold [Future[Option[EmailActivation]]] (Future successful None) { eactEith =>
      eactEith.fold[Future[Option[EmailActivation]]](
        { eact => Future successful Option(eact)},
        { EmailActivation.getById }
      )
    }
  }

}
