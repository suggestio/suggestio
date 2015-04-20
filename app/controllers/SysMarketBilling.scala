package controllers

import com.google.inject.Inject
import models.MBillContract.LegalContractId
import play.api.i18n.MessagesApi
import play.api.mvc.{Result, AnyContent}
import util.PlayMacroLogsImpl
import util.acl.{IsSuperuserContractNode, AbstractRequestWithPwOpt, IsSuperuser}
import models._
import util.SiowebEsUtil.client
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.db.Database
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
class SysMarketBilling @Inject() (
  override val messagesApi: MessagesApi,
  db: Database
)
  extends SioControllerImpl with PlayMacroLogsImpl
{

  import LOGGER._

  /** Внутренний маппинг для даты LocalDate. */
  private def bDate = localDateM
    .transform[DateTime](_.toDateTimeAtStartOfDay, _.toLocalDate)

  /** Маппинг для формы добавления/редактирования контракта. */
  private def contractFormM = Form(mapping(
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
      )
  )
  // apply()
  {(adnId, dateContract, suffix, isActive, hiddenInfo) =>
    MBillContract(
      adnId = adnId,
      contractDate = dateContract,
      suffix = suffix,
      hiddenInfo = hiddenInfo,
      isActive = isActive
    )
  }
  // unapply()
  {mbc =>
    import mbc._
    Some((adnId, contractDate, suffix, isActive, hiddenInfo))
  })


  /** Страница с информацией по биллингу. */
  def billingFor(adnId: String) = IsSuperuser.async { implicit request =>
    val adnNodeOptFut = MAdnNodeCache.getById(adnId)
    // Синхронные модели
    val syncResult = db.withConnection { implicit c =>
      val contracts = MBillContract.findForAdn(adnId)
      val contractIds = contracts.map(_.id.get)
      val mbtsGrouper = { mbt: MBillContractSel => mbt.contractId }
      SysAdnNodeBillingArgs(
        balanceOpt      = MBillBalance.getByAdnId(adnId),
        contracts       = contracts,
        txns            = MBillTxn.findForContracts(contractIds),
        feeTariffsMap   = MBillTariffFee.getAll.groupBy(mbtsGrouper),
        statTariffsMap  = MBillTariffStat.getAll.groupBy(mbtsGrouper),
        dailyMmpsMap    = MBillMmpDaily.findByContractIds(contractIds).groupBy(mbtsGrouper),
        sinkComissionMap = contractIds.flatMap(MSinkComission.findByContractId(_)).groupBy(_.contractId)
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
        val mbcStub = MBillContract(
          adnId         = adnId,
          isActive      = true
        )
        val formM = contractFormM fill mbcStub
        Ok(createContractFormTpl(adnNode, supOpt, formM))

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
        val mbc1 = db.withTransaction { implicit c =>
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
    val mbcOpt = db.withConnection { implicit c =>
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
    val mbc0 = db.withConnection { implicit c =>
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
          suffix       = mbc1.suffix
        )
        db.withConnection { implicit c =>
          mbc3.save
        }
        Redirect(routes.SysMarketBilling.billingFor(mbc3.adnId))
          .flashing("success" -> s"Изменения сохранены: #${mbc3.legalContractId}.")
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
    val paymentStub: (LegalContractId, MBillTxn) = {
      val lci = LegalContractId(0, 0, None, "")
      val now = DateTime.now
      val txn = MBillTxn(
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
          var mbcs = db.withConnection { implicit c =>
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
        val mbc = db.withConnection { implicit c =>
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
    val (lastPays, contracts) = db.withConnection { implicit c =>
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



  // Доступ к модели MSinkComission.
  import sink._

  private def sinkComM: Mapping[MSinkComission] = {
    mapping(
      "sink"          -> sinkM,
      "sioComission"  -> floatM
    )
    {(sink, sioComission) =>
      MSinkComission(contractId = -1, sink = sink, sioComission = sioComission)
    }
    {msc =>
      import msc._
      Some((sink, sioComission))
    }
  }
  private def sinkComFormM: Form[MSinkComission] = Form(sinkComM)

  /** Рендер страницы с формой создания новой [[models.MSinkComission]]. */
  def createSinkCom(contractId: Int) = IsSuperuserContractNode(contractId).apply { implicit request =>
    // Определять необходимый sink, фильтруя node.adn.sinks по имеющимся msc.
    val currentMscs = db.withConnection { implicit c =>
      MSinkComission.findByContractId(contractId)
    }
    val needSinks = request.adnNode.adn.sinks -- currentMscs.map(_.sink)
    val sink = needSinks.headOption getOrElse AdnSinks.default
    val mscStub = MSinkComission(
      contractId = contractId,
      sink = sink,
      sioComission = sink.sioComissionDflt
    )
    val form = sinkComFormM fill mscStub
    Ok(createSinkComTpl(form, request.contract, request.adnNode))
  }

  /** Сабмит формы создания тарифа sink comission. */
  def createSinkComSubmit(contractId: Int) = IsSuperuser.async { implicit request =>
    sinkComFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"createSinkComSubmit($contractId): Failed to bind form:\n${formatFormErrors(formWithErrors)}")
        val contract = db.withConnection { implicit c =>
          MBillContract.getById(contractId).get
        }
        MAdnNodeCache.getById(contract.adnId) map { adnNodeOpt =>
          Ok(createSinkComTpl(formWithErrors, contract, adnNodeOpt.get))
        }
      },
      {msc0 =>
        val msc = msc0.copy(contractId = contractId)
        val adnId = db.withConnection { implicit c =>
          msc.save
          MBillContract.getById(contractId).get.adnId
        }
        // Отправить админа назад на биллинг.
        Redirect( routes.SysMarketBilling.billingFor(adnId) )
          .flashing("success" -> s"Создан тариф для выдачи ${msc.sink.longName}")
      }
    )
  }

  /** Рендер страницы редактирования sink'а. */
  def editSinkCom(scId: Int) = IsSuperuser.async { implicit request =>
    _editSinkCom(scId)
  }

  private def _editSinkCom(scId: Int, formOpt: Option[Form[MSinkComission]] = None)(implicit request: AbstractRequestWithPwOpt[AnyContent]): Future[Result] = {
    val syncResult = db.withConnection { implicit c =>
      val msc = MSinkComission.getById(scId).get
      val mbc = MBillContract.getById(msc.contractId).get
      msc -> mbc
    }
    val (msc, mbc) = syncResult
    val adnNodeOptFut = MAdnNodeCache.getById(mbc.adnId)
    val form: Form[MSinkComission] = formOpt getOrElse { sinkComFormM fill msc }
    adnNodeOptFut map { adnNodeOpt =>
      Ok(editSinkComTpl(msc, form, mbc, adnNodeOpt.get))
    }
  }

  /** Сабмит формы редактирования экземпляра [[models.MSinkComission]]. */
  def editSinkComSubmit(scId: Int) = IsSuperuser.async { implicit request =>
    sinkComFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"editSinkComSubmit($scId): Failed to bind form:\n${formatFormErrors(formWithErrors)}")
        _editSinkCom(scId, Some(formWithErrors))
      },
      {msc1 =>
        val adnId = db.withTransaction { implicit c =>
          val msc = MSinkComission.getById(scId, SelectPolicies.UPDATE).get
          val msc2 = msc.copy(
            sink = msc1.sink,
            sioComission = msc1.sioComission
          )
          val msc3 = msc2.save
          // Узнать куда редиректить админа после действа
          MBillContract.getById(msc3.contractId).get.adnId
        }
        Redirect( routes.SysMarketBilling.billingFor(adnId) )
          .flashing("success" -> "Измения сохранены.")
      }
    )
  }

  /** Админ приказал удалить указанный sink comission. */
  def sinkComDeleteSubmit(scId: Int) = IsSuperuser { implicit request =>
    val adnIdOpt = db.withConnection { implicit c =>
      val msc = MSinkComission.getById(scId)
      msc foreach { _.delete }
      msc
        .flatMap { msc => MBillContract.getById(msc.contractId) }
        .map { _.adnId }
    }
    adnIdOpt match {
      case Some(adnId) =>
        Redirect( routes.SysMarketBilling.billingFor(adnId) )
          .flashing("success" -> "sink-тариф удалён.")

      case None =>
        Redirect( routes.SysMarket.index() )
          .flashing("error" -> "Что-то пошло не так... sink-тариф уже удалён?")
    }
  }

}
