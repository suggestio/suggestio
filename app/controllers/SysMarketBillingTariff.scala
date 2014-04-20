package controllers

import util.PlayMacroLogsImpl
import play.api.data._, Forms._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import util.acl._
import models._
import io.suggest.ym.model.common.{PeriodicalTariffBody, TariffBody}
import util.FormUtil._
import views.html.sys1.market.billing.tariff._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.04.14 21:12
 * Description: Работа с тарифами в биллинге.
 */
object SysMarketBillingTariff extends SioController with PlayMacroLogsImpl {

  /** Маппер тела тарифа абонплаты. */
  val periodicalTariffBodyM = mapping(
    "period" -> isoPeriodM,
    "price"  -> priceStrictNoraw
  )
  // apply()
  {(period, price) =>
    PeriodicalTariffBody(period, price.price, price.currency.getCurrencyCode)
  }
  {ptb =>
    Some((ptb.period, ptb.getPrice))
  }


  /** Генератор форм для различных тарифов. */
  def tariffFormM[T <: TariffBody](tBodyM: Mapping[T]) = Form(mapping(
    "id"      -> number,
    "name"    -> nonEmptyText(maxLength = 128),
    "ttype"   -> tariffTypeM,
    "enabled" -> boolean,
    "tbody"   -> tBodyM
  )
  {(id, name, ttype, enabled, tbody) =>
    Tariff(id, name, ttype, enabled, tbody)
  }
  {tariff =>
    import tariff._
    Some((id, name, tType, isEnabled, tBody.asInstanceOf[T]))
  })

  val periodicalTariffFormM = tariffFormM(periodicalTariffBodyM)


  /** Отрендерить страницу с формой создания нового тарифа. */
  def addTariffForm(adnId: String) = IsSuperuserAdnNode(adnId).async { implicit request =>
    import request.adnNode
    val nextId = if (adnNode.tariffs.isEmpty) {
      0
    } else {
      adnNode.tariffs.maxBy(_.id).id + 1
    }
    Ok(addTariffFormTpl(adnNode, nextId, periodicalTariffFormM))
  }

  /** Сабмит формы добавления нового тарифа. */
  def addTariffFormSubmit(adnId: String) = IsSuperuserAdnNode(adnId).async { implicit request =>
    ???
  }
}
