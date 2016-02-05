package controllers

import com.google.inject.Inject
import io.suggest.ym.parsers.Price
import models._
import models.mbill.MContract.LegalContractId
import models.mbill._
import models.mproj.ICommonDi
import models.msys.MSysAdnNodeBillingArgs
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.data._
import util.FormUtil._
import util.PlayMacroLogsImpl
import util.acl._
import util.async.AsyncUtil
import util.billing.Billing
import views.html.sys1.market.billing._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.14 12:39
 * Description: Контроллер управления биллинга для операторов sio-market.
 */
class SysMarketBilling @Inject() (
  billing                         : Billing,
  override val mCommonDi          : ICommonDi
)
  extends SioControllerImpl
  with PlayMacroLogsImpl
  with IsSuperuserContractNode
  with IsSuNode
  with IsSuperuser
{

  import LOGGER._
  import mCommonDi._

  /** Внутренний маппинг для даты LocalDate. */
  private def bDate = localDateM
    .transform[DateTime](_.toDateTimeAtStartOfDay, _.toLocalDate)

  /** Маппинг для формы добавления/редактирования контракта. */
  private def contractFormM = Form(mapping(
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
      )
  )
  // apply()
  {(dateContract, suffix, isActive, hiddenInfo) =>
    MContract(
      adnId = null,
      contractDate = dateContract,
      suffix = suffix,
      hiddenInfo = hiddenInfo,
      isActive = isActive
    )
  }
  // unapply()
  {mbc =>
    import mbc._
    Some((contractDate, suffix, isActive, hiddenInfo))
  })


  /** Страница с информацией по биллингу. */
  def billingFor(adnId: String) = IsSuperuser.async { implicit request =>
    val adnNodeOptFut = mNodeCache.getById(adnId)
    // Синхронные модели
    val syncResult = db.withConnection { implicit c =>
      val contracts = MContract.findForAdn(adnId)
      val contractIds = contracts.map(_.id.get)
      val mbtsGrouper = { mbt: MContractSel => mbt.contractId }
      MSysAdnNodeBillingArgs(
        balanceOpt      = MBalance.getByAdnId(adnId),
        contracts       = contracts,
        txns            = MTxn.findForContracts(contractIds),
        feeTariffsMap   = MTariffFee.getAll.groupBy(mbtsGrouper),
        dailyMmpsMap    = MTariffDaily.findByContractIds(contractIds).groupBy(mbtsGrouper)
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
  def createContractForm(adnId: String) = IsSuNodeGet(adnId).async { implicit request =>
    val mbcStub = MContract(
      adnId         = adnId,
      isActive      = true
    )
    val formM = contractFormM fill mbcStub
    Ok( createContractFormTpl(request.mnode, formM) )
  }

  /** Сабмит формы создания нового контакта (договора). */
  def createContractFormSubmit(nodeId: String) = {
    IsSuNodePost(nodeId).async(parse.urlFormEncoded) { implicit request =>
      contractFormM.bindFromRequest().fold(
        {formWithErrors =>
          debug("createContractFormSubmit(): Form bind failed: " + formatFormErrors(formWithErrors))
          NotAcceptable(createContractFormTpl(request.mnode, formWithErrors))
        },
        {mbcRaw =>
          val mbc = mbcRaw.copy(
            adnId = nodeId
          )
          val saveFut = Future {
            db.withConnection { implicit c =>
              val _mbc1 = mbc.save
              // Сразу создать баланс, если ещё не создан.
              if (MBalance.getByAdnId(mbc.adnId).isEmpty) {
                MBalance(mbc.adnId, amount = 0F).save
              }
              _mbc1
            }
          }(AsyncUtil.jdbcExecutionContext)
          for (mbc1 <- saveFut) yield {
            Redirect(routes.SysMarketBilling.billingFor(mbc1.adnId))
              .flashing(FLASH.SUCCESS -> s"Создан договор #${mbc1.legalContractId}.")
          }
        }
      )
    }
  }


  /** Запрос страницы редактирования контракта. */
  def editContractForm(contractId: Int) = IsSuperuserContractNodeGet(contractId) { implicit request =>
    val formFilled = contractFormM.fill( request.mcontract )
    Ok( editContractFormTpl(request.mnode, request.mcontract, formFilled) )
  }

  /** Самбит формы редактирования договора. */
  def editContractFormSubmit(contractId: Int) = IsSuperuserContractNodePost(contractId).async { implicit request =>
    contractFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug("editContractFormSubmit(): Form bind failed: " + formatFormErrors(formWithErrors))
        NotAcceptable(editContractFormTpl(request.mnode, request.mcontract, formWithErrors))
      },
      {mbc1 =>
        val mbc3 = request.mcontract.copy(
          contractDate = mbc1.contractDate,
          hiddenInfo   = mbc1.hiddenInfo,
          isActive     = mbc1.isActive,
          suffix       = mbc1.suffix
        )
        val saveFut = Future {
          db.withConnection { implicit c =>
            mbc3.save
          }
        }(AsyncUtil.jdbcExecutionContext)
        for (_ <- saveFut) yield {
          Redirect(routes.SysMarketBilling.billingFor(mbc3.adnId))
            .flashing(FLASH.SUCCESS -> "Changes.saved")
        }
      }
    )
  }


  /** Маппинг для формы ввода входящего платежа. */
  private def paymentFormM = Form(mapping(
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
    val lci = MContract.parseLegalContractId(paymentComment).head
    val txn = MTxn(
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
    val paymentStub: (LegalContractId, MTxn) = {
      val lci = LegalContractId(0, 0, None, "")
      val now = DateTime.now
      val txn = MTxn(
        contractId = -1,
        amount = 0,
        datePaid = now,
        txnUid = "",
        dateProcessed = now,
        paymentComment = "",
        adId = None
      )
      (lci, txn)
    }
    val formM = paymentFormM fill paymentStub
    Ok(createIncomingPaymentFormTpl(formM))
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
        val maybeMbc0 = db.withConnection { implicit c =>
          MContract.getById(lci.contractId)
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
          var mbcs = db.withConnection { implicit c =>
            MContract.findByCrand(lci.crand)
          }
          if (maybeMbc0.isDefined)
            mbcs ::= maybeMbc0.get
          Future.traverse(mbcs) { mbc =>
            mNodeCache.getById(mbc.adnId)
              .map { adnNodeOpt =>
                mbc -> adnNodeOpt.get
              }
          } map { mbcsNoded =>
            NotAcceptable(createIncomingPaymentFormTpl(formBinded, Some(mbcsNoded)))
          }
        } else {
          // Есть точное совпадение. Нужно отрендерить страницу-подтверждение.
          val mbc = maybeMbc.get
          mNodeCache.getById(mbc.adnId) map { adnNodeOpt =>
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
        val mbc = db.withConnection { implicit c =>
          MContract.getById(lci.contractId).get
        }
        if (mbc.crand != lci.crand || !mbc.suffixMatches(lci.suffix))
          throw new IllegalArgumentException("invalid id")
        val result = billing.addPayment(txn)
        Redirect( routes.SysMarketBilling.billingFor(mbc.adnId) )
          .flashing(FLASH.SUCCESS -> ("Платеж успешно проведён. Баланс: " + result.newBalance.amount))
      }
    )
  }


  /** Индексная страница для быстрого доступа операторов к основным функциям. */
  def index = IsSuperuser.async { implicit request =>
    val (lastPays, contracts) = db.withConnection { implicit c =>
      val _lastPays = MTxn.lastNPayments()
      val contractIds = _lastPays.map(_.contractId).distinct
      val _contracts = MContract.multigetByIds(contractIds)
      _lastPays -> _contracts
    }
    val adnIds = contracts.map(_.adnId).distinct
    val adnsFut = MNode.multiGetRev(adnIds)
      .map { adnNodes =>
        adnNodes.map {
          adnNode => adnNode.id.get -> adnNode
        }.toMap
      }
    val contractsMap = {
      contracts.iterator
        .map { mbc =>
          mbc.id.get -> mbc
        }
        .toMap
    }
    adnsFut map { adnsMap =>
      Ok(billingIndexTpl(lastPays, contractsMap, adnsMap))
    }
  }


  /** Что рисовать в браузере, если не найден запрошенный узел рекламной сети. */
  private def adnNodeNotFound(adnId: String) = {
    NotFound("Adn node " + adnId + " does not exists.")
  }

}
