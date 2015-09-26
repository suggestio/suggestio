package controllers

import com.google.inject.Inject
import play.api.i18n.MessagesApi
import play.twirl.api.Html
import util.PlayMacroLogsImpl
import models._
import util.SiowebEsUtil.client
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.db.Database
import util.async.AsyncUtil
import views.html.sys1.market.billing.mmp.daily._
import play.api.data._, Forms._
import util.FormUtil._
import util.acl._
import play.api.mvc.AnyContent
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.05.14 12:07
 * Description: sys-контроллер для работы с mmp-тарификацией, т.е. когда тарификация настраивается по рекламным модулям.
 */
class SysMarketBillingMmp @Inject() (
  override val messagesApi: MessagesApi,
  db: Database
)
  extends SioControllerImpl with PlayMacroLogsImpl
{

  /** Маппинг для формы редактирования [[models.MBillMmpDaily]]. */
  private def mmpDailyM: Mapping[MBillMmpDaily] = {
    val floatGreaterThan1 = floatM.verifying(_ >= 1.0F)
    mapping(
      "currencyCode"  -> currencyCodeOrDfltM,
      "mmpWeekday"    -> floatM,
      "mmpWeekend"    -> floatM,
      "mmpPrimetime"  -> floatM,
      "onStartPage"   -> floatGreaterThan1,
      "onRcvrCat"     -> floatGreaterThan1,
      "weekendCalId"  -> esIdM,
      "primeCalId"    -> esIdM
    )
    {(currencyCode, mmpWeekday, mmpWeekend, mmpPrimetime, onStartPage, onRcvrCat, weekendCalId, primeCalId) =>
      MBillMmpDaily(
        contractId    = -1,
        currencyCode  = currencyCode,
        mmpWeekday    = mmpWeekday,
        mmpWeekend    = mmpWeekend,
        mmpPrimetime  = mmpPrimetime,
        onStartPage   = onStartPage,
        onRcvrCat     = onRcvrCat,
        weekendCalId  = weekendCalId,
        primeCalId    = primeCalId
      )
    }
    {mbmd =>
      import mbmd._
      Some((currencyCode, mmpWeekday, mmpWeekend, mmpPrimetime, onStartPage, onRcvrCat, weekendCalId, primeCalId))
    }
  }

  /** Маппинг формы для одного daily-тарификатора. */
  private def mmpFormM = Form(mmpDailyM)

  private def mmpStubForm: Form[MBillMmpDaily] = {
    val mmpStub = MBillMmpDaily(contractId = -1)
    mmpFormM.fill(mmpStub)
  }

  /**
   * Рендер страницы создания нового посуточного mmp-тарификтора.
   * @param contractId номер договора.
   */
  def createMmpDaily(contractId: Int) = IsSuperuserContract(contractId).async { implicit request =>
    val formM = mmpStubForm
    _createMmpDaily(formM)
      .map(Ok(_))
  }

  private def _createMmpDaily(formM: Form[MBillMmpDaily])(implicit request: ContractRequest[AnyContent]): Future[Html] = {
    val mcalsFut = MCalendar.getAll()
    for {
      adnNodeOpt <- MAdnNodeCache.getById(request.contract.adnId)
      mcals      <- mcalsFut
    } yield {
      createMppDailyTpl(request.contract, formM, adnNodeOpt, mcals)
    }
  }

  /**
   * Сабмит формы создания нового посуточного mmp-тарифного плана.
   * @param contractId номер договора.
   */
  def createMmpDailySubmit(contractId: Int) = IsSuperuserContract(contractId).async { implicit request =>
    mmpFormM.bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug(s"createMmpDailySubmit($contractId): Failed to bind form:\n${formatFormErrors(formWithErrors)}")
        _createMmpDaily(formWithErrors)
          .map(NotAcceptable(_))
      },
      {mbmd0 =>
        val mbmd = mbmd0.copy(contractId = contractId)
        val mbmd1 = db.withConnection { implicit c =>
          mbmd.save
        }
        Redirect(routes.SysMarketBilling.billingFor(request.contract.adnId))
          .flashing(FLASH.SUCCESS -> s"Создан посуточный тарификатор #${mbmd1.id.get} для договора ${request.contract.legalContractId}")
      }
    )
  }


  /**
   * Рендер страницы с формой редактирования посуточного mmp-тарифного плана.
   * @param mmpdId id mmp-тарифного плана.
   */
  def editMmpDaily(mmpdId: Int) = IsSuperuser.async { implicit request =>
    _editMmpDaily(mmpdId, mmpFormM)
      .map(Ok(_))
  }

  private def _editMmpDaily(mmpdId: Int, form: Form[MBillMmpDaily])(implicit request: AbstractRequestWithPwOpt[AnyContent]): Future[Html] = {
    val mcalsFut = MCalendar.getAll()
    val syncResult = db.withConnection { implicit c =>
      val mbmd = MBillMmpDaily.getById(mmpdId).get
      val mbc  = MBillContract.getById(mbmd.contractId).get
      (mbmd, mbc)
    }
    val (mbmd, mbc) = syncResult
    val nodeOptFut = MAdnNodeCache.getById(mbc.adnId)
    val formBinded = mmpFormM.fill(mbmd)
    for {
      nodeOpt <- nodeOptFut
      mcals   <- mcalsFut
    } yield {
      editMppDailyTpl(mbmd, mbc, formBinded, nodeOpt, mcals)
    }
  }

  /**
   * Сабмит формы редактирования mmp-тарифного плана.
   * @param mmpdId id mmp-тарифного плана.
   */
  def editMmpDailySubmit(mmpdId: Int) = IsSuperuser.async { implicit request =>
    mmpFormM.bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug(s"editMmpDailySubmit($mmpdId): Failed to bind form:\n${formatFormErrors(formWithErrors)}")
        _editMmpDaily(mmpdId, formWithErrors)
          .map(NotAcceptable(_))
      },
      {newMbmd =>
        val rdrToAdnFut = Future {
          db.withTransaction { implicit c =>
            val mbmd = MBillMmpDaily.getById(mmpdId).get
            val mbmd1 = newMbmd.copy(id = mbmd.id, contractId = mbmd.contractId)
            mbmd1.save
            // Вычисляем, куда нужно редиректить юзера после сохранения.
            val contract = MBillContract.getById(mbmd.contractId).get
            contract.adnId
          }
        }(AsyncUtil.jdbcExecutionContext)
        // Рендер результата.
        rdrToAdnFut map { rdrToAdn =>
          Redirect(routes.SysMarketBilling.billingFor(rdrToAdn))
            .flashing(FLASH.SUCCESS -> s"Изменения в тарифном плане #$mmpdId сохранены.")
        }
      }
    )
  }


  /** Сабмит удаления mmp-тарификатора. */
  def deleteMmpDailySubmit(mmpId: Int) = IsSuperuser.async { implicit request =>
    // Нужно узнать adnId на который редиректить (из контракта) и удалить mmp-тарификатор.
    val resultFut = Future {
      db.withConnection { implicit c =>
        val adnIdOpt = MBillMmpDaily.getById(mmpId).flatMap(_.contract).map(_.adnId)
        val rowsDeleted = MBillMmpDaily.deleteById(mmpId)
        (adnIdOpt, rowsDeleted)
      }
    }(AsyncUtil.jdbcExecutionContext)

    for {
      (adnIdOpt, rowsDeleted) <- resultFut
    } yield {
      val flashInfo: (String, String) = rowsDeleted match {
        case 1 => FLASH.SUCCESS -> s"Mmp-тарификатор #$mmpId удалён."
        case 0 => FLASH.ERROR   -> s"Mmp-тарификатор #$mmpId НЕ НАЙДЕН."
        case _ => FLASH.ERROR   -> s"Неизвестный результат операции для mmp#$mmpId: $rowsDeleted. Ожидалось 1 или 0."
      }
      val rdrCall = adnIdOpt.fold {
        routes.SysMarket.index()
      } {
        routes.SysMarketBilling.billingFor
      }
      Redirect(rdrCall)
        .flashing(flashInfo)
    }
  }


  /**
   * Бывает, что нужно очень массово отредактировать тарифы узлов.
   * @return Страница с формой массового редактирования mmp-тарифа.
   */
  def updateAllForm = IsSuperuser.async { implicit request =>
    _updateAllFormHtml( mmpStubForm )
      .map { Ok(_) }
  }

  /**
   * Рендер страницы с формой массового редактирования тарифов.
   * @param formM Маппинг формы для рендера.
   * @return HTML страницы формы редактирования.
    */
  private def _updateAllFormHtml(formM: Form[MBillMmpDaily])(implicit request: AbstractRequestWithPwOpt[_]): Future[Html] = {
    val mcalsFut = MCalendar.getAll()
    for {
      mcals <- mcalsFut
    } yield {
      updateAllFormTpl(formM, mcals)
    }
  }

  /**
   * Сабмит формы массового обновления daily-тарифов.
   * @return Редирект, либо форму с ошибкой маппинга.
   */
  def updateAllSubmit = IsSuperuser.async { implicit request =>
    mmpFormM.bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug("updateAllSubmit(): Failed to bind form:\n" + formatFormErrors(formWithErrors))
        _updateAllFormHtml(formWithErrors)
          .map { NotAcceptable(_) }
      },
      {mmp2 =>
        val countUpdatedFut = Future {
          db.withConnection { implicit c =>
            MBillMmpDaily.updateAll(mmp2)
          }
        }
        for {
          count <- countUpdatedFut
        } yield {
          Redirect( routes.SysMarketBilling.index() )
            .flashing(FLASH.SUCCESS -> s"Обновлено $count посуточных тарифов.")
        }
      }
    )
  }

}
