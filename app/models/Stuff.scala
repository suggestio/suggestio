package models

import java.util.Currency
import play.api.i18n.Messages
import play.api.data.Form
import util.PlayLazyMacroLogsImpl
import scala.collection.JavaConversions._
/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.04.14 17:09
 * Description: Моделе-подобное барахло, которые в основном нужно для шаблонов.
 */

object CurrencyCodeOpt extends PlayLazyMacroLogsImpl {
  val CURRENCY_CODE_DFLT = "RUB"
}


trait CurrencyCode {
  import CurrencyCodeOpt.LOGGER._

  def currencyCode: String
  def currency = {
    try {
      Currency.getInstance(currencyCode)
    } catch {
      case ex: Exception =>
        error("Unsupported/unknown currency code: " + currencyCode + "; Supported are " + Currency.getAvailableCurrencies.toSeq.map(_.getCurrencyCode).mkString(", "), ex)
        throw ex
    }
  }
}

/** Опциональное поле currencyCode, подразумевающее дефолтовую валюту. */
trait CurrencyCodeOpt extends CurrencyCode {
  def currencyCodeOpt : Option[String]
  def currencyCode = currencyCodeOpt getOrElse CurrencyCodeOpt.CURRENCY_CODE_DFLT
}


/** У шаблона [[views.html.sys1.market.billing.adnNodeBillingTpl]] очень много параметров со сложными типам.
  * Тут удобный контейнер для всей кучи параметров шаблона. */
case class SysAdnNodeBillingArgs(
  balanceOpt: Option[MBillBalance],
  contracts: Seq[MBillContract],
  txns: Seq[MBillTxn],
  feeTariffsMap: collection.Map[Int, Seq[MBillTariffFee]],
  statTariffsMap: collection.Map[Int, Seq[MBillTariffStat]],
  dailyMmpsMap: collection.Map[Int, Seq[MBillMmpDaily]]
)


/** Статическая утиль для шаблонов, работающих со экшенами статистики. */
object AdStatActionsTpl {

  def adStatActionI18N(asa: AdStatAction): String = {
    "ad.stat.action." + asa.toString
  }

  /** Фунция для генерации списка пар (String, String), которые описывают  */
  def adStatActionsSeq(implicit ctx: Context): Seq[(AdStatAction, String)] = {
    import ctx._
    AdStatActions.values.toSeq.map { v =>
      val i18n = adStatActionI18N(v)
      v -> Messages(i18n)
    }
  }

  def adStatActionsSeqStr(implicit ctx: Context): Seq[(String, String)] = {
    import ctx._
    AdStatActions.values.toSeq.map { v =>
      val i18n = adStatActionI18N(v)
      v.toString -> Messages(i18n)
    }
  }

}



/** Выбор высоты шрифта влияет на высоту линии (интерлиньяж) и возможно иные параметры.
  * В любом случае, ключом является кегль шрифта. */
case class FontSize(size: Int, lineHeight: Int)



case class CurrentAdvsTplArgs(
  advs: Seq[MAdvI],
  adv2adn: Map[Int, MAdnNode],
  blockedSums: Seq[(Float, Currency)]
)
case class AdvFormTplArgs(
  adId: String,
  adnNodes: Seq[MAdnNode],
  af: Form[_],
  busyAdns: Map[String, MAdvI]
)


/** Размеры аудиторий. */
object AudienceSizes extends Enumeration {
  type AudienceSize = Value
  val LessThan20 = Value("lt20")
  val LessThan50 = Value("lt50")
  val Greater50  = Value("gt50")

  def maybeWithName(n: String): Option[AudienceSize] = {
    try {
      Some(withName(n))
    } catch {
      case ex: NoSuchElementException  =>  None
    }
  }
}


case class MAdvPricing(
  prices: Iterable[(Currency, Float)],
  hasEnoughtMoney: Boolean
)
