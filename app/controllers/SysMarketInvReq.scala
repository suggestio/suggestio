package controllers

import org.elasticsearch.index.engine.VersionConflictEngineException
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
    val mcOptFut = mir.company.fold[Future[Option[MCompany]]](
      { mc => Future successful Some(mc) },
      { mcId => MCompany.getById(mcId) }
    )
    for {
      mcOpt <- mcOptFut
    } yield {
      Ok(irShowOneTpl(mir, mcOpt))
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
          Redirect(routes.SysMarketInvReq.showIR(_mirId))
            .flashing("success" -> "Шаблон будущей компании успешно обновлён.")
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
        updateFut map { _ =>
          Redirect( routes.SysMarketInvReq.showIR(mirId) )
            .flashing("success" -> s"Создана компания '${mc0.meta.name}'")
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
            updFut map { _ =>
              Redirect( routes.SysMarketInvReq.showIR(mirId) )
                .flashing("success" -> "Компания деинсталлирована назад в шаблон компании.")
            }

          case false =>
            ExpectationFailed(s"Unable to remove existsing company $mcId. MC.delete() returned false.")
        }

      case None =>
        NotFound("Previously installed company not found, mcId=" + mcId)
    }
  }


  /** Удалить один MIR. */
  def deleteIR(irId: String) = IsSuperuser.async { implicit request =>
    MInviteRequest.deleteById(irId) map { isDeleted =>
      val flasher: (String, String) = if (isDeleted) {
        "success" -> "Запрос на подключение удалён."
      } else {
        "error"   -> "Не найдено документа для удаления."
      }
      Redirect(routes.SysMarketInvReq.index())
        .flashing(flasher)
    }
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
                throw new IllegalStateException(s"${logPrefix}Looks like MIR instance has been deleted during update. last try was $n")
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

}
