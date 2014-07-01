package controllers

import util.PlayMacroLogsImpl
import util.acl.IsSuperuser
import models._
import util.SiowebEsUtil.client
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.event.SiowebNotifier.Implicts.sn
import play.api.db.DB
import play.api.Play.current
import views.html.sys1.market.billing._
import play.api.data._, Forms._
import util.FormUtil._
import org.joda.time.DateTime
import scala.concurrent.Future
import io.suggest.ym.parsers.Price
import util.billing.Billing

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.14 12:39
 * Description: Контроллер управления биллинга для операторов sio-market.
 */
object SysMarketBilling extends SioController with PlayMacroLogsImpl {

  import LOGGER._

  private val bDate = localDate
    .transform[DateTime](_.toDateTimeAtStartOfDay, _.toLocalDate)

  /** Маппинг для формы добавления/редактирования контракта. */
  private val contractFormM = Form(mapping(
    "adnId"         -> esIdM,
    "dateContract"  -> bDate
    ,
    "suffix"        -> optional(
      text(maxLength = 16)
        .transform(strTrimSanitizeF, strIdentityF)
    ),
    "isActive"      -> boolean,
    "hiddenInfo"    -> text(maxLength = 4096)
      .transform(strFmtTrimF, strIdentityF)
      .transform[Option[String]](
        {Some(_).filter(!_.isEmpty)},
        {_ getOrElse ""}
      ),
    "sioComission"  -> floatM
  )
  // apply()
  {(adnId, dateContract, suffix, isActive, hiddenInfo, sioComission) =>
    MBillContract(
      adnId = adnId,
      contractDate = dateContract,
      suffix = suffix,
      hiddenInfo = hiddenInfo,
      isActive = isActive,
      sioComission = sioComission
    )
  }
  // unapply()
  {mbc =>
    import mbc._
    Some((adnId, contractDate, suffix, isActive, hiddenInfo, sioComission))
  })


  /** Страница с информацией по биллингу. */
  def billingFor(adnId: String) = IsSuperuser.async { implicit request =>
    val adnNodeOptFut = MAdnNodeCache.getById(adnId)
    // Синхронные модели
    val syncResult = DB.withConnection { implicit c =>
      val contracts = MBillContract.findForAdn(adnId)
      val contractIds = contracts.map(_.id.get)
      val mbtsGrouper = { mbt: MBillContractSel => mbt.contractId }
      SysAdnNodeBillingArgs(
        balanceOpt      = MBillBalance.getByAdnId(adnId),
        contracts       = contracts,
        txns            = MBillTxn.findForContracts(contractIds),
        feeTariffsMap   = MBillTariffFee.getAll.groupBy(mbtsGrouper),
        statTariffsMap  = MBillTariffStat.getAll.groupBy(mbtsGrouper),
        dailyMmpsMap    = MBillMmpDaily.findByContractIds(contractIds).groupBy(mbtsGrouper)
      )
    }
    adnNodeOptFut map {
      case Some(adnNode) =>
        Ok(adnNodeBillingTpl(adnNode, syncResult))
      case None =>
        adnNodeNotFound(adnId)
    }
  }

  /** Форма создания нового контракта (договора). */
  def createContractForm(adnId: String) = IsSuperuser.async { implicit request =>
    getNodeAndSupAsync(adnId) map {
      case Some((adnNode, supOpt)) =>
        Ok(createContractFormTpl(adnNode, supOpt, contractFormM))

      case None => adnNodeNotFound(adnId)
    }
  }

  /** Сабмит формы создания нового контакта (договора). */
  def createContractFormSubmit = IsSuperuser.async(parse.urlFormEncoded) { implicit request =>
    contractFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug("createContractFormSubmit(): Form bind failed: " + formatFormErrors(formWithErrors))
        val adnId = request.body("adnId").head
        getNodeAndSupAsync(adnId) map {
          case Some((adnNode, supOpt)) =>
            NotAcceptable(createContractFormTpl(adnNode, supOpt, formWithErrors))
          case None =>
            adnNodeNotFound(adnId)
        }
      },
      {mbc =>
        val mbc1 = DB.withTransaction { implicit c =>
          val _mbc1 = mbc.save
          // Сразу создать баланс, если ещё не создан.
          if (MBillBalance.getByAdnId(mbc.adnId).isEmpty) {
            MBillBalance(mbc.adnId, amount = 0F).save
          }
          _mbc1
        }
        Redirect(routes.SysMarketBilling.billingFor(mbc1.adnId))
          .flashing("success" -> s"Создан договор #${mbc1.legalContractId}.")
      }
    )
  }


  /** Запрос страницы редактирования контракта. */
  def editContractForm(contractId: Int) = IsSuperuser.async { implicit request =>
    val mbcOpt = DB.withConnection { implicit c =>
      MBillContract.getById(contractId)
    }
    mbcOpt match {
      case Some(mbc) =>
        val afut = getNodeAndSupAsync(mbc.adnId)
        val formFilled = contractFormM.fill(mbc)
        afut map {
          case Some((adnNode, supOpt)) =>
            Ok(editContractFormTpl(adnNode, supOpt, mbc, formFilled))
          case None =>
            adnNodeNotFound(mbc.adnId)
        }

      case None => NotFound("No such contract: " + contractId)
    }
  }

  /** Самбит формы редактирования договора. */
  def editContractFormSubmit(contractId: Int) = IsSuperuser.async { implicit request =>
    val mbc0 = DB.withConnection { implicit c =>
      MBillContract.getById(contractId).get
    }
    contractFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug("editContractFormSubmit(): Form bind failed: " + formatFormErrors(formWithErrors))
        val adnId = mbc0.adnId
        getNodeAndSupAsync(adnId) map {
          case Some((adnNode, supOpt)) =>
            NotAcceptable(editContractFormTpl(adnNode, supOpt, mbc0, formWithErrors))
          case None =>
            adnNodeNotFound(adnId)
        }
      },
      {mbc1 =>
        val mbc3 = mbc0.copy(
          contractDate = mbc1.contractDate,
          hiddenInfo   = mbc1.hiddenInfo,
          isActive     = mbc1.isActive,
          suffix       = mbc1.suffix,
          sioComission = mbc1.sioComission
        )
        DB.withConnection { implicit c =>
          mbc3.save
        }
        Redirect(routes.SysMarketBilling.billingFor(mbc3.adnId))
          .flashing("success" -> s"Изменения сохранены: #${mbc3.legalContractId}.")
      }
    )
  }


  /** Маппинг для формы ввода входящего платежа. */
  private val paymentFormM = Form(mapping(
    "txnUid" -> nonEmptyText(minLength = 4, maxLength = 64)
      .transform(strTrimSanitizeLowerF, strIdentityF),
    "amount" -> priceStrictM
      .transform[Price]({_._2}, {"" -> _}),
    "datePaid" -> bDate,
    "paymentComment" -> nonEmptyText(minLength = 10, maxLength = 512)
      .transform(strTrimSanitizeF, strIdentityF)
  )
  // apply().
  {(txnUid, price, datePaid, paymentComment) =>
    val lci = MBillContract.parseLegalContractId(paymentComment).head
    val txn = MBillTxn(
      contractId = lci.contractId,
      amount     = price.price,
      datePaid   = datePaid,
      txnUid     = txnUid,
      currencyCodeOpt = Some(price.currency.getCurrencyCode),
      paymentComment  = paymentComment
    )
    lci -> txn
  }
  // unapply()
  {case (lci, txn) =>
    Some((txn.txnUid, Price(txn.amount, txn.currency), txn.datePaid, txn.paymentComment))
  })

  /** Форма добавления транзакции в обработку. Ожидается, что оператор будет просто
    * вставлять куски реквизитов платежа, а система сама разберётся по какому договору проводить
    * платеж. */
  def incomingPaymentForm = IsSuperuser { implicit request =>
    Ok(createIncomingPaymentFormTpl(paymentFormM))
  }

  /** Сабмит формы добавления платежной транзакции. */
  def incomingPaymentFormSubmit = IsSuperuser.async { implicit request =>
    val formBinded = paymentFormM.bindFromRequest()
    formBinded.fold(
      {formWithErrors =>
        debug("incomingPaymentFormSubmit(): Failed to bind form: " + formatFormErrors(formWithErrors))
        NotAcceptable(createIncomingPaymentFormTpl(formWithErrors))
      },
      {case (lci, txn) =>
        // Форма распарсилась, но может быть там неверные данные по платежу.
        // Следует помнить, что этот сабмит для проверки, сохранение данных будет на следующем шаге.
        val maybeMbc0 = DB.withConnection { implicit c =>
          MBillContract.getById(lci.contractId)
        }
        val maybeMbc = maybeMbc0.filter { mbc =>
          val crandMatches = mbc.crand == lci.crand
          val suffixMatches = mbc suffixMatches lci.suffix
          val result = crandMatches && suffixMatches
          if (!result) {
            debug("incomingPaymentFormSubmit(): Failed to fully match contract id: " +
              s"crand->$crandMatches (${lci.crand} mbc=${mbc.crand}) suffix->$suffixMatches (${lci.suffix} mbc=${mbc.suffix})")
          }
          result
        }
        if (maybeMbc.isEmpty) {
          // Нет точного совпадения с искомым контрактом. Надо переспросить оператора, выдав ему подсказки.
          var mbcs = DB.withConnection { implicit c =>
            MBillContract.findByCrand(lci.crand)
          }
          if (maybeMbc0.isDefined)
            mbcs ::= maybeMbc0.get
          Future.traverse(mbcs) { mbc =>
            MAdnNodeCache.getById(mbc.adnId) map { adnNodeOpt =>
              mbc -> adnNodeOpt.get
            }
          } map { mbcsNoded =>
            NotAcceptable(createIncomingPaymentFormTpl(formBinded, Some(mbcsNoded)))
          }
        } else {
          // Есть точное совпадение. Нужно отрендерить страницу-подтверждение.
          val mbc = maybeMbc.get
          MAdnNodeCache.getById(mbc.adnId) map { adnNodeOpt =>
            val adnNode = adnNodeOpt.get
            Ok(confirmIncomingPaymentTpl(adnNode, mbc, formBinded))
          }
        }
      } // END mapped ok
    )
  }

  /** Сабмит подтверждения платежа. */
  def confirmIncomingPaymentSubmit = IsSuperuser.async { implicit request =>
    // Тут не должно быть никаких проблем, поэтому не тратим время на отработку ошибок.
    paymentFormM.bindFromRequest().fold(
      {formWithErrors =>
        warn("confirmIncomingPaymentSubmit(): Failed to bind locked form: " + formatFormErrors(formWithErrors))
        NotAcceptable("Invalid data. This should not occur. Use 'back' browser button to continue.")
      },
      {case (lci, txn) =>
        val mbc = DB.withConnection { implicit c =>
          MBillContract.getById(lci.contractId).get
        }
        if (mbc.crand != lci.crand || !mbc.suffixMatches(lci.suffix))
          throw new IllegalArgumentException("invalid id")
        val result = Billing.addPayment(txn)
        val okMsg = "Платеж успешно проведён. Баланс: " + result.newBalance.amount
        Redirect(routes.SysMarketBilling.billingFor(mbc.adnId))
          .flashing("success" -> okMsg)
      }
    )
  }


  /** Индексная страница для быстрого доступа операторов к основным функциям. */
  def index = IsSuperuser.async { implicit request =>
    val (lastPays, contracts) = DB.withConnection { implicit c =>
      val _lastPays = MBillTxn.lastNPayments()
      val contractIds = _lastPays.map(_.contractId).distinct
      val _contracts = MBillContract.multigetByIds(contractIds)
      _lastPays -> _contracts
    }
    val adnIds = contracts.map(_.adnId).distinct
    val adnsFut = MAdnNode.multiGet(adnIds)
      .map { adnNodes =>
        adnNodes.map {
          adnNode => adnNode.id.get -> adnNode
        }.toMap
      }
    val contractsMap = contracts.map { mbc =>
      mbc.id.get -> mbc
    }.toMap
    adnsFut map { adnsMap =>
      Ok(billingIndexTpl(lastPays, contractsMap, adnsMap))
    }
  }


  /** Что рисовать в браузере, если не найден запрошенный узел рекламной сети. */
  private def adnNodeNotFound(adnId: String) = {
    NotFound("Adn node " + adnId + " does not exists.")
  }

  private def getNodeAndSupAsync(adnId: String): Future[Option[(MAdnNode, Option[MAdnNode])]] = {
    MAdnNodeCache.getById(adnId) flatMap {
      case Some(adnNode) =>
        MAdnNodeCache.maybeGetByIdCached(adnNode.adn.supId) map { supOpt =>
          Some(adnNode -> supOpt)
        }
      case None => Future successful None
    }
  }

}
