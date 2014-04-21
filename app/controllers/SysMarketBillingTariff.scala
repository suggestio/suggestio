package controllers

import util.PlayMacroLogsImpl
import play.api.data._, Forms._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import util.acl._
import models._
import util.FormUtil._
import views.html.sys1.market.billing.tariff._
import org.joda.time.DateTime
import play.api.db.DB
import play.api.Play.current
import io.suggest.ym.parsers.Price

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.04.14 21:12
 * Description: Работа с тарифами в биллинге.
 */
object SysMarketBillingTariff extends SioController with PlayMacroLogsImpl {

  import LOGGER._

  /** Генератор форм для различных тарифов. */
  val feeTariffFormM = Form(mapping(
    "name"      -> nonEmptyText(maxLength = 128),
    "enabled"   -> boolean,
    "dateFirst" -> jodaDate,
    "tinterval" -> pgIntervalM,
    "price"     -> priceStrictNoraw
  )
  {(name, enabled, dateFirst, tinterval, price) =>
    MBillTariffFee(
      contractId  = -1,
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
    MAdnNodeCache.getByIdCached(contract.adnId) map { adnNodeOpt =>
      Ok(addTariffFormTpl(adnNodeOpt.get, contract, feeTariffFormM))
    }
  }

  /** Сабмит формы добавления нового тарифа. */
  def addFeeTariffFormSubmit(contractId: Int) = IsSuperuserContract(contractId).async { implicit request =>
    lazy val logPrefix = s"addFeeTariffFormSubmit($contractId): "
    import request.contract
    val formBinded = feeTariffFormM.bindFromRequest()
    formBinded.fold(
      {formWithErrors =>
        debug(logPrefix + "Failed to bind form: " + formatFormErrors(formWithErrors))
        MAdnNodeCache.getByIdCached(contract.adnId) map { adnNodeOpt =>
          NotAcceptable(addTariffFormTpl(adnNodeOpt.get, contract, formWithErrors))
        }
      },
      {tariff =>
        tariff.contractId = contractId
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
    MAdnNodeCache.getByIdCached(contract.adnId) map { adnNodeOpt =>
      val form = feeTariffFormM.fill(tariff)
      Ok(editTariffFormTpl(adnNodeOpt.get, contract, tariff, form))
    }
  }

  /** Сабмит формы редактирования тарифа. */
  def editFeeTariffFormSubmit(tariffId: Int) = IsSuperuserFeeTariffContract(tariffId).async { implicit request =>
    lazy val logPrefix = s"editFeeTariffFormSubmit($tariffId): "
    import request.{contract, tariff => tariff0}
    feeTariffFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(logPrefix + "Failed to bind form: " + formatFormErrors(formWithErrors))
        MAdnNodeCache.getByIdCached(contract.adnId).map { adnNodeOpt =>
          NotAcceptable(editTariffFormTpl(adnNodeOpt.get, contract, tariff0, formWithErrors))
        }
      },
      {tariff1 =>
        tariff0.name = tariff1.name
        tariff0.tinterval = tariff1.tinterval
        tariff0.fee = tariff1.fee
        tariff0.feeCC = tariff1.feeCC
        val now = DateTime.now
        tariff0.dateModified = Some(now)
        if (tariff0.isEnabled != tariff1.isEnabled) {
          tariff0.dateStatus = now
          tariff0.isEnabled = tariff1.isEnabled
        }
        tariff0.dateFirst = tariff1.dateFirst
        val tariffSaved = DB.withConnection { implicit c =>
          tariff0.save
        }
        val flashMsg = s"Изменения в тарифе #${tariffSaved.id.get} сохранены (договор ${contract.legalContractId})."
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
    val flashMsg = s"Тариф #$tariffId удалён: ${rowsDeleted > 0}. Договор ${contract.legalContractId}."
    rdrFlashing(contract.adnId, flashMsg)
  }


  /** Перенаправить юзера на страницу биллинга узла с flash-сообщением об успехе. */
  private def rdrFlashing(adnId: String, message: String) = {
    Redirect(routes.SysMarketBilling.billingFor(adnId))
      .flashing("success" -> message)
  }

}
