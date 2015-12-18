package controllers

import com.google.inject.Inject
import io.suggest.ym.parsers.Price
import models.mbill.{MContract, MTariff, MTariffFee, MTariffStat}
import models.mproj.MCommonDi
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.data._
import util.FormUtil._
import util.PlayMacroLogsImpl
import util.acl._
import views.html.sys1.market.billing.tariff._
import views.html.sys1.market.billing.tariff.stat._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.04.14 21:12
 * Description: Работа с fee- и stat-тарифами в биллинге.
 */
class SysMarketBillingTariff @Inject() (
  override val mCommonDi          : MCommonDi
)
  extends SioControllerImpl
  with PlayMacroLogsImpl
  with IsSuperuserContract
  with IsSuperuserStatTariffContract
  with IsSuperuserFeeTariffContract
{

  import LOGGER._
  import mCommonDi._

  private def nameKM      = "name"      -> nonEmptyText(maxLength = 128)
  private def enabledKM   = "enabled"   -> boolean
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
    MTariffFee(
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
    import request.mcontract
    mNodeCache.getById(mcontract.adnId) map { adnNodeOpt =>
      Ok(addTariffFormTpl(adnNodeOpt.get, mcontract, feeTariffFormM(contractId)))
    }
  }

  /** Сабмит формы добавления нового тарифа. */
  def addFeeTariffFormSubmit(contractId: Int) = IsSuperuserContract(contractId).async { implicit request =>
    lazy val logPrefix = s"addFeeTariffFormSubmit($contractId): "
    import request.mcontract
    val formBinded = feeTariffFormM(contractId).bindFromRequest()
    formBinded.fold(
      {formWithErrors =>
        debug(logPrefix + "Failed to bind form: " + formatFormErrors(formWithErrors))
        mNodeCache.getById(mcontract.adnId) map { adnNodeOpt =>
          NotAcceptable(addTariffFormTpl(adnNodeOpt.get, mcontract, formWithErrors))
        }
      },
      {tariff =>
        // Добавляем новый тариф в adnNode
        val tariffSaved = db.withConnection { implicit c =>
          tariff.save
        }
        val flashMsg = s"Тариф #${tariffSaved.id.get} добавлен к договору ${mcontract.legalContractId}."
        rdrFlashing(mcontract.adnId, flashMsg)
      }
    )
  }


  /** Отрендерить страницу с формой редактирования существующего тарифа. */
  def editFeeTariffForm(tariffId: Int) = IsSuperuserFeeTariffContract(tariffId).async { implicit request =>
    import request.{mcontract, mTariffFee}
    mNodeCache.getById(mcontract.adnId) map { adnNodeOpt =>
      val form = feeTariffFormM(mcontract.id.get).fill(mTariffFee)
      Ok(editTariffFormTpl(adnNodeOpt.get, mcontract, mTariffFee, form))
    }
  }

  /** Сабмит формы редактирования тарифа. */
  def editFeeTariffFormSubmit(tariffId: Int) = IsSuperuserFeeTariffContract(tariffId).async { implicit request =>
    lazy val logPrefix = s"editFeeTariffFormSubmit($tariffId): "
    import request.{mcontract, mTariffFee => tariff0}
    feeTariffFormM(mcontract.id.get).bindFromRequest().fold(
      {formWithErrors =>
        debug(logPrefix + "Failed to bind form: " + formatFormErrors(formWithErrors))
        mNodeCache.getById(mcontract.adnId).map { adnNodeOpt =>
          NotAcceptable(editTariffFormTpl(adnNodeOpt.get, mcontract, tariff0, formWithErrors))
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
        val tariffSaved = db.withConnection { implicit c =>
          tariff2.save
        }
        val flashMsg = editSaveFlashMsg(tariffSaved, mcontract)
        rdrFlashing(mcontract.adnId, flashMsg)
      }
    )
  }

  /** POST на удаление тарифа. */
  def deleteFeeTariffSubmit(tariffId: Int) = IsSuperuserFeeTariffContract(tariffId).apply { implicit request =>
    import request.{mcontract, mTariffFee}
    val rowsDeleted = db.withConnection { implicit c =>
      mTariffFee.delete
    }
    val flashMsg = deleteFlashMsg(tariffId, rowsDeleted, mcontract)
    rdrFlashing(mcontract.adnId, flashMsg)
  }


  /** Перенаправить юзера на страницу биллинга узла с flash-сообщением об успехе. */
  private def rdrFlashing(adnId: String, message: String) = {
    Redirect(routes.SysMarketBilling.billingFor(adnId))
      .flashing(FLASH.SUCCESS -> message)
  }

  private def editSaveFlashMsg(tariff: MTariff, contract: MContract): String = {
    s"Изменения в тарифе #${tariff.id.get} сохранены (договор ${contract.legalContractId})."
  }

  private def deleteFlashMsg(tariffId: Int, rowsDeleted: Int, contract: MContract): String = {
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
    MTariffStat(
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
    mNodeCache.getById(request.mcontract.adnId) map {
      case Some(adnNode) =>
        val stf = statTariffFormM(contractId)
        Ok(addStatTariffFormTpl(adnNode, stf, request.mcontract))
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
        mNodeCache.getById(request.mcontract.adnId) map { adnNodeOpt =>
          NotAcceptable(addStatTariffFormTpl(adnNodeOpt.get, formWithErrors, request.mcontract))
        }
      },
      {mbts =>
        val tariffSaved = db.withConnection { implicit c =>
          mbts.save
        }
        rdrFlashing(request.mcontract.adnId, "Создан тариф #" + tariffSaved.id.get)
      }
    )
  }


  /** Рендер страницы с формой редактирования существующего stat-тарифа. */
  def editStatTariff(tariffId: Int) = IsSuperuserStatTariffContract(tariffId).async { implicit request =>
    mNodeCache.getById(request.mcontract.adnId) map { adnNodeOpt =>
      val formBinded = statTariffFormM(request.mcontract.id.get).fill(request.mTariffStat)
      Ok(editStatTariffFormTpl(adnNodeOpt.get, request.mTariffStat, request.mcontract, formBinded))
    }
  }

  /** Сабмит формы редактирования stat-тарифа. */
  def editStatTariffSubmit(tariffId: Int) = IsSuperuserStatTariffContract(tariffId).async { implicit request =>
    import request.mTariffStat
    statTariffFormM(request.mcontract.id.get).bindFromRequest().fold(
      {formWithErrors =>
        debug(s"editStatTariffSubmit($tariffId): Failed to bind edit form:\n${formatFormErrors(formWithErrors)}")
        mNodeCache.getById(request.mcontract.adnId) map { adnNodeOpt =>
          NotAcceptable(editStatTariffFormTpl(adnNodeOpt.get, mTariffStat, request.mcontract, formWithErrors))
        }
      },
      {tariff2 =>
        val now = DateTime.now
        val tariff3 = mTariffStat.copy(
          debitAmount = tariff2.debitAmount,
          debitFor    = tariff2.debitFor,
          name        = tariff2.name,
          dateModified = Some(now),
          isEnabled   = tariff2.isEnabled,
          dateStatus  = if (mTariffStat.isEnabled != tariff2.isEnabled) now else mTariffStat.dateStatus,
          dateFirst   = tariff2.dateFirst
        )
        db.withConnection { implicit c =>
          tariff3.save
        }
        val flashMsg = editSaveFlashMsg(mTariffStat, request.mcontract)
        rdrFlashing(request.mcontract.adnId, flashMsg)
      }
    )
  }


  /** Запрос на удаление stat-тарифа. */
  def deleteStatTariffSubmit(tariffId: Int) = IsSuperuserStatTariffContract(tariffId).apply { implicit request =>
    import request.{mcontract, mTariffStat}
    val rowsDeleted = db.withConnection { implicit c =>
      mTariffStat.delete
    }
    val flashMsg = deleteFlashMsg(tariffId, rowsDeleted, mcontract)
    rdrFlashing(mcontract.adnId, flashMsg)
  }

}
