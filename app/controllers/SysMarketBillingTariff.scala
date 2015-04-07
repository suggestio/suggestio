package controllers

import com.google.inject.Inject
import play.api.i18n.MessagesApi
import util.PlayMacroLogsImpl
import play.api.data._, Forms._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import util.acl._
import models._
import util.FormUtil._
import views.html.sys1.market.billing.tariff._
import views.html.sys1.market.billing.tariff.stat._
import org.joda.time.DateTime
import play.api.db.DB
import play.api.Play.current
import io.suggest.ym.parsers.Price

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.04.14 21:12
 * Description: Работа с fee- и stat-тарифами в биллинге.
 */
class SysMarketBillingTariff @Inject() (val messagesApi: MessagesApi) extends SioControllerImpl with PlayMacroLogsImpl {

  import LOGGER._

  private def nameKM = "name" -> nonEmptyText(maxLength = 128)
  private def enabledKM = "enabled"   -> boolean
  private def dateFirstKM = "dateFirst" -> jodaDate("dd.MM.yyyy HH:mm")

  /** Генератор форм для различных тарифов абонплаты. */
  private def feeTariffFormM(contractId: Int) = Form(mapping(
    nameKM,
    enabledKM,
    dateFirstKM,
    "tinterval" -> pgIntervalM,
    "price"     -> priceStrictNoraw
  )
  {(name, enabled, dateFirst, tinterval, price) =>
    MBillTariffFee(
      contractId  = contractId,
      name        = name,
      isEnabled   = enabled,
      dateFirst   = dateFirst,
      fee         = price.price,
      feeCC       = price.currency.getCurrencyCode,
      tinterval   = tinterval
    )
  }
  {tariff =>
    import tariff._
    Some((name, isEnabled, dateFirst, tinterval, Price(fee, feeCurrency)))
  })


  /** Отрендерить страницу с формой создания нового тарифа. */
  def addFeeTariffForm(contractId: Int) = IsSuperuserContract(contractId).async { implicit request =>
    import request.contract
    MAdnNodeCache.getById(contract.adnId) map { adnNodeOpt =>
      Ok(addTariffFormTpl(adnNodeOpt.get, contract, feeTariffFormM(contractId)))
    }
  }

  /** Сабмит формы добавления нового тарифа. */
  def addFeeTariffFormSubmit(contractId: Int) = IsSuperuserContract(contractId).async { implicit request =>
    lazy val logPrefix = s"addFeeTariffFormSubmit($contractId): "
    import request.contract
    val formBinded = feeTariffFormM(contractId).bindFromRequest()
    formBinded.fold(
      {formWithErrors =>
        debug(logPrefix + "Failed to bind form: " + formatFormErrors(formWithErrors))
        MAdnNodeCache.getById(contract.adnId) map { adnNodeOpt =>
          NotAcceptable(addTariffFormTpl(adnNodeOpt.get, contract, formWithErrors))
        }
      },
      {tariff =>
        // Добавляем новый тариф в adnNode
        val tariffSaved = DB.withConnection { implicit c =>
          tariff.save
        }
        val flashMsg = s"Тариф #${tariffSaved.id.get} добавлен к договору ${contract.legalContractId}."
        rdrFlashing(contract.adnId, flashMsg)
      }
    )
  }


  /** Отрендерить страницу с формой редактирования существующего тарифа. */
  def editFeeTariffForm(tariffId: Int) = IsSuperuserFeeTariffContract(tariffId).async { implicit request =>
    import request.{contract, tariff}
    MAdnNodeCache.getById(contract.adnId) map { adnNodeOpt =>
      val form = feeTariffFormM(contract.id.get).fill(tariff)
      Ok(editTariffFormTpl(adnNodeOpt.get, contract, tariff, form))
    }
  }

  /** Сабмит формы редактирования тарифа. */
  def editFeeTariffFormSubmit(tariffId: Int) = IsSuperuserFeeTariffContract(tariffId).async { implicit request =>
    lazy val logPrefix = s"editFeeTariffFormSubmit($tariffId): "
    import request.{contract, tariff => tariff0}
    feeTariffFormM(contract.id.get).bindFromRequest().fold(
      {formWithErrors =>
        debug(logPrefix + "Failed to bind form: " + formatFormErrors(formWithErrors))
        MAdnNodeCache.getById(contract.adnId).map { adnNodeOpt =>
          NotAcceptable(editTariffFormTpl(adnNodeOpt.get, contract, tariff0, formWithErrors))
        }
      },
      {tariff1 =>
        val now = DateTime.now
        val tariff2 = tariff0.copy(
          name        = tariff1.name,
          tinterval   = tariff1.tinterval,
          fee         = tariff1.fee,
          feeCC       = tariff1.feeCC,
          dateModified = Some(now),
          dateFirst   = tariff1.dateFirst,
          isEnabled   = tariff1.isEnabled,
          dateStatus  = if (tariff0.isEnabled != tariff1.isEnabled) now else tariff0.dateStatus
        )
        val tariffSaved = DB.withConnection { implicit c =>
          tariff2.save
        }
        val flashMsg = editSaveFlashMsg(tariffSaved, contract)
        rdrFlashing(contract.adnId, flashMsg)
      }
    )
  }

  /** POST на удаление тарифа. */
  def deleteFeeTariffSubmit(tariffId: Int) = IsSuperuserFeeTariffContract(tariffId).apply { implicit request =>
    import request.{contract, tariff}
    val rowsDeleted = DB.withConnection { implicit c =>
      tariff.delete
    }
    val flashMsg = deleteFlashMsg(tariffId, rowsDeleted, contract)
    rdrFlashing(contract.adnId, flashMsg)
  }


  /** Перенаправить юзера на страницу биллинга узла с flash-сообщением об успехе. */
  private def rdrFlashing(adnId: String, message: String) = {
    Redirect(routes.SysMarketBilling.billingFor(adnId))
      .flashing("success" -> message)
  }

  private def editSaveFlashMsg(tariff: MBillTariff, contract: MBillContract): String = {
    s"Изменения в тарифе #${tariff.id.get} сохранены (договор ${contract.legalContractId})."
  }

  private def deleteFlashMsg(tariffId: Int, rowsDeleted: Int, contract: MBillContract): String = {
    s"Тариф #$tariffId удалён: ${rowsDeleted > 0}. Договор ${contract.legalContractId}."
  }


  // Тарифы, работающий по просмотрам/переходам (stat-тарифы).

  private def statTariffFormM(contractId: Int) = Form(mapping(
    nameKM,
    enabledKM,
    dateFirstKM,
    "debitFor"  -> adStatActionM,
    "price"     -> priceStrictNoraw
  )
  {(name, isEnabled, dateFirst, debitFor, price) =>
    MBillTariffStat(
      contractId  = contractId,
      name        = name,
      isEnabled   = isEnabled,
      dateFirst   = dateFirst,
      debitFor    = debitFor,
      debitAmount = price.price,
      currencyCode = price.currency.getCurrencyCode
    )
  }
  {mbts =>
    import mbts._
    val price = Price(debitAmount, currency)
    Some( (name, isEnabled, dateFirst, debitFor, price) )
  })

  /** Экшен рендера страницы добавления тарификации по просмотрам/переходам. */
  def addStatTariff(contractId: Int) = IsSuperuserContract(contractId).async { implicit request =>
    MAdnNodeCache.getById(request.contract.adnId) map {
      case Some(adnNode) =>
        val stf = statTariffFormM(contractId)
        Ok(addStatTariffFormTpl(adnNode, stf, request.contract))
      case None =>
        NotFound("Contract not found: " + contractId)
    }
  }

  /** Сабмит формы добавления тарификации по просмотрам/переходам. */
  def addStatTariffSubmit(contractId: Int) = IsSuperuserContract(contractId).async { implicit request =>
    val stf = statTariffFormM(contractId)
    stf.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"addStatTariffFormSubmit($contractId): Failed to bind form:\n${formatFormErrors(formWithErrors)}")
        MAdnNodeCache.getById(request.contract.adnId) map { adnNodeOpt =>
          NotAcceptable(addStatTariffFormTpl(adnNodeOpt.get, formWithErrors, request.contract))
        }
      },
      {mbts =>
        val tariffSaved = DB.withConnection { implicit c =>
          mbts.save
        }
        rdrFlashing(request.contract.adnId, "Создан тариф #" + tariffSaved.id.get)
      }
    )
  }


  /** Рендер страницы с формой редактирования существующего stat-тарифа. */
  def editStatTariff(tariffId: Int) = IsSuperuserStatTariffContract(tariffId).async { implicit request =>
    MAdnNodeCache.getById(request.contract.adnId) map { adnNodeOpt =>
      val formBinded = statTariffFormM(request.contract.id.get).fill(request.tariff)
      Ok(editStatTariffFormTpl(adnNodeOpt.get, request.tariff, request.contract, formBinded))
    }
  }

  /** Сабмит формы редактирования stat-тарифа. */
  def editStatTariffSubmit(tariffId: Int) = IsSuperuserStatTariffContract(tariffId).async { implicit request =>
    import request.tariff
    statTariffFormM(request.contract.id.get).bindFromRequest().fold(
      {formWithErrors =>
        debug(s"editStatTariffSubmit($tariffId): Failed to bind edit form:\n${formatFormErrors(formWithErrors)}")
        MAdnNodeCache.getById(request.contract.adnId) map { adnNodeOpt =>
          NotAcceptable(editStatTariffFormTpl(adnNodeOpt.get, tariff, request.contract, formWithErrors))
        }
      },
      {tariff2 =>
        val now = DateTime.now
        val tariff3 = tariff.copy(
          debitAmount = tariff2.debitAmount,
          debitFor    = tariff2.debitFor,
          name        = tariff2.name,
          dateModified = Some(now),
          isEnabled   = tariff2.isEnabled,
          dateStatus  = if (tariff.isEnabled != tariff2.isEnabled) now else tariff.dateStatus,
          dateFirst   = tariff2.dateFirst
        )
        DB.withConnection { implicit c =>
          tariff3.save
        }
        val flashMsg = editSaveFlashMsg(tariff, request.contract)
        rdrFlashing(request.contract.adnId, flashMsg)
      }
    )
  }


  /** Запрос на удаление stat-тарифа. */
  def deleteStatTariffSubmit(tariffId: Int) = IsSuperuserStatTariffContract(tariffId).apply { implicit request =>
    import request.{contract, tariff}
    val rowsDeleted = DB.withConnection { implicit c =>
      tariff.delete
    }
    val flashMsg = deleteFlashMsg(tariffId, rowsDeleted, contract)
    rdrFlashing(contract.adnId, flashMsg)
  }

}
