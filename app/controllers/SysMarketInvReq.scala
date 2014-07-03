package controllers

import org.elasticsearch.index.engine.VersionConflictEngineException
import play.api.mvc.{AnyContent, Result}
import play.twirl.api.HtmlFormat
import util.acl._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import models._
import scala.concurrent.Future
import views.html.sys1.market.invreq._
import util.PlayMacroLogsImpl
import util.event.SiowebNotifier.Implicts.sn
import SysMarket.companyFormM

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.06.14 10:16
 * Description: Sys-контроллер для обработки входящих инвайтов.
 * v1: Отображать список, отображать каждый элемент, удалять.
 * v2: Нужен многошаговый и удобный мастер создания узлов со всеми контрактами и инвайтами, отметками о ходе
 * обработки запроса и т.д.
 */
object SysMarketInvReq extends SioController with PlayMacroLogsImpl {

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

  /** Страница редактирования компании, встроенной в интанс [[models.MInviteRequest]]'a. */
  def companyEdit(mirId: String) = isCompanyLeft(mirId).apply { implicit request =>
    import request.mir
    val mc = mir.company.left.get
    val formBinded = companyFormM fill mc
    Ok(companyEditTpl(mir, mc, formBinded))
  }

  /** Сабмит формы редактирования компании. */
  def companyEditFormSubmit(mirId: String) = isCompanyLeft(mirId).async { implicit request =>
    import request.mir
    val mc = mir.company.left.get
    companyFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"companyEditFormSubmit(mir=$mirId): Failed to bind company form:\n${formatFormErrors(formWithErrors)}")
        NotAcceptable(companyEditTpl(mir, mc, formWithErrors))
      },
      {mc2 =>
        tryUpdateMir(mir) { mir0 =>
          val mc3 = SysMarket.updateCompany(mc, mc2)
          mir0.copy(
            company = Left(mc3)
          )
        } map { _mirId =>
          rdrToIr(_mirId, "Шаблон будущей компании успешно обновлён.")
        }
      }
    )
  }

  /** Sio-админ затребовал установку шаблона компании в базу. Нужно сохранить компанию и обновить состояние MIR. */
  def companyInstallSubmit(mirId: String) = isCompanyLeft(mirId).async { implicit request =>
    import request.mir
    val mc0 = mir.company.left.get
    val previoslyExistedFut = mc0.id.fold [Future[Boolean]] (Future successful false) { MCompany.isExist }
    previoslyExistedFut.flatMap { previoslyExisted =>
      mc0.save.flatMap { savedMcId =>
        // Компания сохранена. Пора попробовать обновить текущий MIR
        val updateFut = tryUpdateMir(mir) { mir0 =>
          mir0.copy(
            company = Right(savedMcId)
          )
        }
        if (!previoslyExisted) {
          updateFut onFailure { case ex =>
            warn(s"Rollbacking company install: mir=$mirId companyId=$savedMcId due to exception", ex)
            MCompany.deleteById(savedMcId)
          }
        }
        updateFut map { _mirId =>
          rdrToIr(_mirId, s"Создана компания '${mc0.meta.name}'")
        }
      }
    }
  }

  /** Sio-админ запрашивает деинсталляцию шаблона компании назад в шаблон из базы MCompany.
    * Нужно удалить компанию и обновить состояние MIR. */
  def companyUninstallSubmit(mirId: String) = isCompanyRight(mirId).async { implicit request =>
    import request.mir
    val mcId = mir.company.right.get
    MCompany.getById(mcId) flatMap {
      case Some(mc) =>
        lazy val logPrefix = s"companyUninstallSubmit($mir): "
        mc.delete flatMap {
          case true =>
            debug(logPrefix + "Company uninstalled: " + mcId)
            val updFut = tryUpdateMir(mir) { mir0 =>
              mir0.copy(
                company = Left(mc)
              )
            }
            updFut onFailure { case ex =>
              warn(s"${logPrefix}Rollbacking deletion of $mc due to exception during MIR update", ex)
              mc.save
            }
            updFut map { _mirId =>
              rdrToIr(_mirId, "Компания деинсталлирована назад в шаблон компании.")
            }

          case false =>
            ExpectationFailed(s"Unable to remove existsing company $mcId. MC.delete() returned false.")
        }

      case None =>
        NotFound("Previously installed company not found, mcId=" + mcId)
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


  /** Форма для редактирования узла, но вместо id компании может быть любой мусор. */
  private val adnNodeFormM = {
    import play.api.data._, Forms._
    SysMarket.getAdnNodeFormM(text(maxLength = 40))
  }

  /** Запрос страницы с формой редактирования заготовки узла. */
  def nodeEdit(mirId: String) = isNodeLeft(mirId).async { implicit request =>
    nodeEditBody(request.mir)(Ok(_))
  }

  private def nodeEditBody(mir: MInviteRequest)(onSuccess: HtmlFormat.Appendable => Result)
                          (implicit request: MirRequest[AnyContent]): Future[Result] = {
    val mcOptFut = getCompanyOptFut(mir)
    val adnOptFut = getNodeOptFut(mir)
    mcOptFut flatMap {
      case Some(mc) =>
        adnOptFut map {
          case Some(adnNode) =>
            val formFilled = adnNodeFormM fill adnNode
            Ok(nodeEditTpl(mir, adnNode, mc, formFilled))
          case None =>
            NotFound("Node not found")
        }

      case None =>
        NotFound("Company not found.")
    }
  }

  /** Сабмит формы редактирования заготовки рекламного узла.
    * Здесь может быть подвох в поле companyId, которое может содержать пустое значение. */
  def nodeEditFormSubmit(mirId: String) = isNodeLeft(mirId).async { implicit request =>
    import request.mir
    adnNodeFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"nodeEditFormSubmit($mirId): Failed to bind form:\n${formatFormErrors(formWithErrors)}")
        nodeEditBody(mir)(NotAcceptable(_))
      },
      {adnNode2 =>
        tryUpdateMir(mir) { mir0 =>
          val adnNode = mir0.adnNode.left.get
          val adnNode3 = SysMarket.updateAdnNode(adnNode, adnNode2)
          mir0.copy(
            adnNode = Left(adnNode3)
          )
        } map { _mirId =>
          rdrToIr(_mirId, "Шаблон узла сохранён.")
        }
      }
    )
  }

  /** sio-админ приказывает развернуть рекламный узел. */
  def nodeInstallSubmit(mirId: String) = isNodeLeft(mirId).async { implicit request =>
    import request.mir
    assert(mir.company.isRight, "error.company.not.installed")
    val mcId = mir.company.right.get
    val adnNode0 = mir.adnNode.left.get
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
          companyId = mcId,
          meta = adnNode0.meta.copy(
            welcomeAdId = waSavedIdOpt
          )
        )
        // Запуск сохранения узла.
        val resultFut = adnNode.save flatMap { adnId =>
          // Узел сохранён. Пора обновить экземпляр MIR
          val updateFut = tryUpdateMir(mir) { mir0 =>
            mir0.copy(
              adnNode = Right(adnId),
              waOpt = waSavedIdOpt.map(Right.apply)
            )
          }
          if (previoslyExisted) {
            updateFut onFailure { case ex =>
              warn(s"nodeInstallSubmit($mirId): Rollbacking node[$adnId] installation due to exception during MIR update")
              MAdnNode.deleteById(adnId)
            }
          }
          updateFut map { _mirId =>
            rdrToIr(_mirId, "Рекламный узел добавлен в систему.")
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
    val adnId = mir.adnNode.right.get
    MAdnNode.getById(adnId) flatMap {
      case Some(adnNode) =>
        adnNode.delete flatMap {
          case true =>
            // Узел удалён. Пора залить его в состояние MIR
            val updateFut = tryUpdateMir(mir) { mir0 =>
              mir0.copy(
                adnNode = Left(adnNode)
              )
            }
            updateFut onFailure { case ex =>
              adnNode.save
              warn(s"nodeUnistallSubmit($mirId): rollbacking node[$adnId] deinstallation due to exception.", ex)
            }
            updateFut map { _mirId =>
              rdrToIr(_mirId, "Рекламный узел деинсталлирован назад в шаблон узла. Это может нарушить ссылочную целостность.")
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
    val adnId = mir.adnNode.right.get
    val adnNodeOptFut = MAdnNodeCache.getById(adnId)
    val eact = mir.emailAct.left.get.copy(
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
              SysMarket.sendEmailInvite(ea1, adnNode)
            }
            val updFut = sendEmailFut flatMap { _ =>
              // Пора переключить состояние mir
              tryUpdateMir(mir) { mir0 =>
                mir0.copy(
                  emailAct = Right(eaId)
                )
              }
            }
            if (!previoslyExisted) {
              updFut onFailure { case ex =>
                ea1.delete
                warn(s"eactInstallSubmit($mirId): Rollbacking save of eact[$eaId] due to exception.", ex)
              }
            }
            updFut map { _mirId =>
              rdrToIr(_mirId, s"Сохранён запрос на активацию. Письмо отправлено на ${ea1.email}.")
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


  /** Обновления инстанса [[models.MInviteRequest]] с аккуратным подавлением конфликтов версий.
    * @param mir0 Начальный инстанс MIR. Будет опробован только при нулевой попытке.
    * @param n Необязательный номер попытки, который не превышает максимум UPDATE_RETRIES_MAX.
    * @param updateF Фунцкия патчинга, которая должа накатывать изменения на переданный интанс MInviteRequest.
    * @return Фьючерс с сохранённым id, т.е. результат EsModelT.save().
    */
  private def tryUpdateMir(mir0: MInviteRequest, n: Int = 0)(updateF: MInviteRequest => MInviteRequest): Future[String] = {
    updateF(mir0)
      .save
      .recoverWith {
        case ex: VersionConflictEngineException =>
          lazy val logPrefix = s"tryUpdateMir(${mir0.id.get}): "
          if (n < UPDATE_RETRIES_MAX) {
            val n1 = n + 1
            warn(s"${logPrefix}Version conflict while trying to save MIR. Retrying ($n1/$UPDATE_RETRIES_MAX)...")
            MInviteRequest.getById(mir0.id.get) flatMap {
              case Some(mir00) =>
                tryUpdateMir(mir00, n1)(updateF)
              case None =>
                throw new IllegalStateException(s"${logPrefix}Looks like MIR instance has been deleted during update. last try was $n", ex)
            }
          } else {
            throw new RuntimeException(logPrefix + "Too many save-update retries failed: " + n, ex)
          }
      }
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
        mir.adnNode.isLeft
      }
    }
  }

  private def isNodeRight(mirId: String) = new IsSuperuserMir(mirId) {
    override def mirStateInvalidMsg: String = {
      "MIR.node is NOT installed, but action possible only for already installed node. Go back and press F5."
    }
    override def isMirStateOk(mir: MInviteRequest): Boolean = {
      super.isMirStateOk(mir) && {
        mir.adnNode.isRight
      }
    }
  }

  private def isEactLeft(mirId: String) = new IsSuperuserMir(mirId) {
    override def mirStateInvalidMsg: String = {
      "MIR.eact is installed, but action possible only for already NOT-installed EAct. Go back and press F5."
    }
    override def isMirStateOk(mir: MInviteRequest): Boolean = {
      super.isMirStateOk(mir) && {
        mir.emailAct.isLeft
      }
    }
  }
  private def isNodeEactLeft(mirId: String) = new IsSuperuserMir(mirId) {
    override def mirStateInvalidMsg: String = {
      "MIR.eact is already installed OR node NOT installed, but action possible only for already NOT-installed EAct and installed node. Go back and press F5."
    }
    override def isMirStateOk(mir: MInviteRequest): Boolean = {
      super.isMirStateOk(mir) && {
        mir.adnNode.isRight && mir.emailAct.isLeft
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
    mir.adnNode.fold[Future[Option[MAdnNode]]](
      { an => Future successful Option(an) },
      { adnId => MAdnNodeCache.getById(adnId) }
    )
  }

  private def getEactOptFut(mir: MInviteRequest): Future[Option[EmailActivation]] = {
    mir.emailAct.fold [Future[Option[EmailActivation]]] (
      { eact => Future successful Option(eact) },
      { EmailActivation.getById }
    )
  }

}
