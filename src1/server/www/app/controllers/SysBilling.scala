package controllers

import io.suggest.bill.{MCurrencies, MPrice}
import io.suggest.common.fut.FutureUtil
import io.suggest.es.model.EsModel
import io.suggest.mbill2.m.balance.{MBalance, MBalances}
import io.suggest.mbill2.m.contract.{MContract, MContracts}
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.txn.MTxns
import io.suggest.n2.bill.MNodeBilling
import io.suggest.n2.bill.tariff.daily.MTfDaily
import io.suggest.n2.node.{MNode, MNodes}
import io.suggest.util.logs.MacroLogsImplLazy

import javax.inject.Inject
import models.mcal.MCalendars
import models.msys.bill._
import models.req.{INodeContractReq, INodeReq}
import play.api.data.Form
import play.api.data.Forms.{mapping, text}
import play.api.mvc.Result
import util.FormUtil.{currencyOrDfltM, doubleM, toStrOptM}
import util.acl.{IsSu, IsSuNode, IsSuNodeContract, IsSuNodeNoContract, SioControllerApi}
import util.billing.{Bill2Conf, Bill2Util, ContractUtil, TfDailyUtil}
import views.html.sys1.bill._
import views.html.sys1.bill.contract._
import views.html.sys1.bill.contract.balance._
import views.html.sys1.bill.daily._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.12.15 23:01
 * Description: Контроллер sys-биллинга второго поколения.
 * Второй биллинг имеет тарифы внутри узлов и контракты-ордеры-item'ы в РСУБД.
 */
final class SysBilling @Inject() (
                                   sioControllerApi           : SioControllerApi,
                                 )
  extends MacroLogsImplLazy
{

  import sioControllerApi._
  import mCommonDi._
  import mCommonDi.current.injector

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val tfDailyUtil = injector.instanceOf[TfDailyUtil]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val mCalendars = injector.instanceOf[MCalendars]
  private lazy val mContracts = injector.instanceOf[MContracts]
  private lazy val mBalances = injector.instanceOf[MBalances]
  private lazy val mTxns = injector.instanceOf[MTxns]
  private lazy val contractUtil = injector.instanceOf[ContractUtil]
  private lazy val isSu = injector.instanceOf[IsSu]
  private lazy val isSuNode = injector.instanceOf[IsSuNode]
  private lazy val isSuNodeContract = injector.instanceOf[IsSuNodeContract]
  private lazy val isSuNodeNoContract = injector.instanceOf[IsSuNodeNoContract]
  private lazy val bill2Conf = injector.instanceOf[Bill2Conf]
  private lazy val bill2Util = injector.instanceOf[Bill2Util]


  /**
   * Отображение страницы биллинга для узла.
   *
   * @param nodeId id просматриваемого узла.
   */
  def forNode(nodeId: String) = csrf.AddToken {
    isSuNode(nodeId).async { implicit request =>
      val contractIdOpt = request.mnode.billing.contractId

      val mContractOptFut = FutureUtil.optFut2futOpt(contractIdOpt) { contractId =>
        val mContractOptAction = mContracts.getById(contractId)
        slick.db.run(mContractOptAction)
      }

      val mBalancesFut = contractIdOpt.fold[Future[Seq[MBalance]]] {
        Future.successful(Nil)
      } { contractId =>
        val mBalancesAction = mBalances.findByContractId(contractId)
        slick.db.run(mBalancesAction)
      }

      // Получить узел CBCA для доступа к дефолтовому тарифу, если требуется.
      val cbcaNodeOptFut = if (request.mnode.billing.tariffs.daily.isEmpty && bill2Conf.CBCA_NODE_ID != nodeId) {
        bill2Util.cbcaNodeOptFut
      } else {
        Future.successful(None)
      }

      for {
        mContractOpt <- mContractOptFut
        mBalances    <- mBalancesFut
        cbcaNodeOpt  <- cbcaNodeOptFut
      } yield {
        // Поискать контракт, собрать аргументы для рендера, отрендерить forNodeTpl.
        val args = MForNodeTplArgs(
          mnode         = request.mnode,
          mContractOpt  = mContractOpt,
          mBalances     = mBalances,
          cbcaNodeOpt   = cbcaNodeOpt
        )
        Ok( forNodeTpl(args) )
      }
    }
  }


  /**
    * Обзор биллинга второго поколения. Пришел на смену ctl.SysMarketBilling.index().
    *
    * @return 200 Ok со страницей инфы по биллингу.
    */
  def overview() = csrf.AddToken {
    isSu().async { implicit request =>
      // Поиск последних финансовых транзакций для отображения таблицы оных.
      val txnsBalancesFut = slick.db.run {
        for {
          txns      <- mTxns.findLatestTxns(limit = 10)
          if txns.nonEmpty
          balances  <- mBalances.getByIds( txns.iterator.map(_.balanceId).toSet )
        } yield {
          (txns, balances)
        }
      }.recover { case _: NoSuchElementException =>
        (Nil, Nil)
      }

      val balancesMapFut = for {
        (_, balances) <- txnsBalancesFut
      } yield {
        balances
          .zipWithIdIter[Gid_t]
          .to( Map )
      }

      // Запустить рендер результата
      for {
        (txns, _)   <- txnsBalancesFut
        balancesMap <- balancesMapFut
      } yield {
        val args = MBillOverviewTplArgs(
          txns          = txns,
          balancesMap   = balancesMap
        )
        Ok(overviewTpl(args))
      }
    }
  }


  /**
   * Страница редактирования посуточного тарифа узла.
   *
   * @param nodeId id узла, для которого редактируется тариф.
   */
  def editNodeTfDaily(nodeId: String) = csrf.AddToken {
    isSuNode(nodeId).async { implicit request =>
      // Вычисляем эффективный тариф узла.
      val realTfFut = tfDailyUtil.nodeTf(request.mnode)

      val formEmpty = tfDailyUtil.tfDailyForm
      val formFut = for (realTf <- realTfFut) yield {
        formEmpty.fill(realTf)
      }

      _editNodeTfDaily(formFut, Ok)
    }
  }

  private def _editNodeTfDaily(formFut: Future[Form[MTfDaily]], rs: Status)
                              (implicit request: INodeReq[_]): Future[Result] = {
    import esModel.api._

    // Собираем доступные календари.
    val mcalsFut = mCalendars.getAll()

    // Рендер результата, когда все исходные вданные будут собраны.
    for {
      mcals  <- mcalsFut
      form   <- formFut
    } yield {
      val args = MTfDailyEditTplArgs(
        mnode     = request.mnode,
        mcals     = mcals,
        tf        = form
      )
      val html = tfDailyEditTpl(args)
      rs(html)
    }
  }

  /**
   * Сабмит формы редактирования тарифа узла.
   *
   * @param nodeId id редактируемого узла.
   * @return редирект на forNode().
   */
  def editNodeTfDailySubmit(nodeId: String) = csrf.Check {
    isSuNode(nodeId).async { implicit request =>
      tfDailyUtil.tfDailyForm.bindFromRequest().fold(
        {formWithErrors =>
          val respFut = _editNodeTfDaily(Future.successful(formWithErrors), NotAcceptable)
          LOGGER.debug(s"editNodeTfDailySubmit($nodeId): Failed to bind form:\n${formatFormErrors(formWithErrors)}")
          respFut
        },
        {tf2 =>
          val saveFut = tfDailyUtil.updateNodeTf(request.mnode, Some(tf2))

          for (_ <- saveFut) yield {
            Redirect(routes.SysBilling.forNode(nodeId))
              .flashing(FLASH.SUCCESS -> "Сохранен посуточный тариф для узла")
          }
        }
      )
    }
  }


  /**
   * Удаление текущего тарифа узла.
   *
   * @param nodeId id редактируемого узла.
   * @return Редирект на forNode().
   */
  def deleteNodeTfDaily(nodeId: String) = csrf.Check {
    isSuNode(nodeId).async { implicit request =>
      // Запустить стирание посуточного тарифа узла.
      val saveFut = tfDailyUtil.updateNodeTf(request.mnode, newTf = None)

      LOGGER.trace(s"deleteNodeTfDaily($nodeId): erasing tf...")

      // Отредиректить юзера на биллинг узла, когда всё будет готово.
      for (_ <- saveFut) yield {
        Redirect( routes.SysBilling.forNode(nodeId) )
          .flashing(FLASH.SUCCESS -> s"Сброшен тариф узла: ${request.mnode.guessDisplayNameOrId.orNull}")
      }
    }
  }

  /**
   * Рендер страницы с формой создания контракта.
   *
   * @param nodeId id узла, для которого создаётся контракт.
   */
  def createContract(nodeId: String) = csrf.AddToken {
    isSuNodeNoContract(nodeId).async { implicit request =>
      val form0 = contractUtil.contractForm

      val formDummy = form0.fill(
        MContract(
          suffix = Some( mContracts.SUFFIX_DFLT )
        )
      )

      _createContract(formDummy, Ok)
    }
  }

  private def _createContract(cf: Form[MContract], rs: Status)(implicit request: INodeReq[_]): Future[Result] = {
    val html = createTpl(
      mnode = request.mnode,
      cf    = cf
    )
    rs(html)
  }

  /**
   * Сабмит формы создания контракта.
   *
   * @param nodeId id узла, для которого создаётся контракт.
   */
  def createContractSubmit(nodeId: String) = csrf.Check {
    isSuNodeNoContract(nodeId).async { implicit request =>
      contractUtil.contractForm.bindFromRequest().fold(
        {formWithErrors =>
          val respFut = _createContract(formWithErrors, NotAcceptable)
          LOGGER.debug(s"createContractSubmit($nodeId): Failed to bind form:\n ${formatFormErrors(formWithErrors)}")
          respFut
        },
        {mc =>
          import esModel.api._

          LOGGER.trace(s"Creating new contract for node $nodeId...")
          val mcAct = mContracts.insertOne(mc)
          val mcSaveFut = slick.db.run(mcAct)

          // Запустить сохранение нового id контракта в узел.
          val nodeSaveFut = mcSaveFut.flatMap { mc =>
            val lens = MNode.billing
              .composeLens( MNodeBilling.contractId )
            mNodes.tryUpdate(request.mnode) { mnode =>
              assert( lens.get(mnode).isEmpty )
              lens.set( mc.id )(mnode)
            }
          }

          // Если контракт создан, а узел не обновлён, то надо удалить свежесозданный контракт.
          for {
            mc2 <- mcSaveFut
            ex  <- nodeSaveFut.failed
          } {
            val contractId = mc2.id.get
            val deleteFut = slick.db.run {
              bill2Util.deleteContract( contractId )
            }
            LOGGER.warn("Rollbacking contract save because of node save error", ex)
            if (LOGGER.underlying.isTraceEnabled) {
              deleteFut.onComplete { result =>
                LOGGER.trace(s"Deleted recently created contract $contractId. res => $result")
              }
            }
          }

          for {
            _   <- nodeSaveFut
            mc  <- mcSaveFut
          } yield {
            Redirect( routes.SysBilling.forNode(nodeId) )
              .flashing(FLASH.SUCCESS -> s"Создан контракт ${mc.legalContractId} для узла ${request.mnode.guessDisplayNameOrIdOrEmpty}")
          }
        }
      )
    }
  }


  /**
   * Экшен рендера страницы редактирования контракта узла.
   *
   * @param nodeId id изменяемого узла.
   */
  def editContract(nodeId: String) = csrf.AddToken {
    isSuNodeContract(nodeId).async { implicit request =>
      val cf = contractUtil.contractForm.fill( request.mcontract )
      _editContract(cf, Ok)
    }
  }

  private def _editContract(cf: Form[MContract], rs: Status)
                           (implicit request: INodeContractReq[_]): Future[Result] = {
    val html = editTpl(request.mnode, request.mcontract, cf)
    rs(html)
  }

  /**
   * Сабмит формы редактирования контракта.
   *
   * @param nodeId id узла.
   */
  def editContractSubmit(nodeId: String) = csrf.Check {
    isSuNodeContract(nodeId).async { implicit request =>
      contractUtil.contractForm.bindFromRequest().fold(
        {formWithErrors =>
          val fut = _editContract(formWithErrors, NotAcceptable)
          LOGGER.debug(s"editContractSubmit($nodeId): Failed to bind form:\n${formatFormErrors(formWithErrors)}")
          fut
        },
        {mc1 =>
          val mc2 = request.mcontract.copy(
            dateCreated = mc1.dateCreated,
            hiddenInfo  = mc1.hiddenInfo,
            suffix      = mc1.suffix
          )
          val updFut = slick.db.run(
            mContracts.updateOne(mc2)
          )

          LOGGER.trace(s"editContractSubmit($nodeId): Saving new contract: $mc2")

          for (rowsUpdated <- updFut if rowsUpdated == 1) yield {
            Redirect( routes.SysBilling.forNode(nodeId) )
              .flashing(FLASH.SUCCESS -> s"Сохранён контракт ${mc2.legalContractId}.")
          }
        }
      )
    }
  }


  /**
   * Сабмит удаления контракта.
   *
   * @param nodeId id узла.
   * @return Редирект на forNode().
   */
  def deleteContractSubmit(nodeId: String) = csrf.Check {
    isSuNodeContract(nodeId).async { implicit request =>
      val contractId = request.mcontract.id.get
      val deleteFut = slick.db.run {
        bill2Util.deleteContract( contractId )
      }

      lazy val logPrefix = s"deleteContractSubmit($nodeId):"

      LOGGER.debug(s"$logPrefix Erasing #${request.mcontract.legalContractId}")
      import esModel.api._

      val nodeSaveFut = deleteFut.flatMap { _ =>
        // TODO XXX дорефакторить tryUpdate + monocle. TODO Допроверять код на предмет null'ов, возвращаемых из tryUpdate.
        mNodes.tryUpdate(request.mnode) {
          MNode.billing
            .composeLens( MNodeBilling.contractId )
            .set( None )
        }
      }

      // Отработать сценарии, когда возникает ошибка при сохранении узла.
      for (_ <- deleteFut) {
        for (ex <- nodeSaveFut) {
          val act2 = mContracts.insertOne( request.mcontract )
          val reInsFut = slick.db.run(act2)
          LOGGER.error(s"$logPrefix Re-inserting deleted contract after node update failure: ${request.mcontract}", ex)
          for (ex2 <- reInsFut.failed)
            LOGGER.error(s"$logPrefix Unable to re-insert $act2", ex2)
        }
      }

      for {
        _           <- nodeSaveFut
        rowsDeleted <- deleteFut
      } yield {
        val res = Redirect( routes.SysBilling.forNode(nodeId) )
        val flash = rowsDeleted match {
          case 1 =>
            FLASH.SUCCESS -> "Контракт удалён безвозратно."
          case 0 =>
            FLASH.ERROR   -> "Кажется, контракт уже был удалён."
        }
        res.flashing(flash)
      }
    }
  }



  /** Маппинг форма приёма произвольного платежа на баланс. */
  private def _paymentFormM: Form[MPaymentFormResult] = {
    import MPaymentFormResult._
    Form(
      mapping(
        AMOUNT_FN         -> doubleM,
        CURRENCY_CODE_FN  -> currencyOrDfltM,
        COMMENT_FN        -> toStrOptM( text(maxLength = 256) )
      )
      { (realAmount, currency, commentOpt) =>
        MPaymentFormResult(
          amount        = MPrice.realAmountToAmount(realAmount, currency),
          currencyCode  = currency,
          comment       = commentOpt
        )
      }
      {mpfr =>
        for {
          (amount, currency, commentOpt) <- MPaymentFormResult.unapply(mpfr)
        } yield {
          val realAmount = MPrice.amountToReal( amount, currency )
          (realAmount, currency, commentOpt)
        }
      }
    )
  }

  /** Валюты, доступные юзеру в форме. */
  private def _currencies = {
    val rub = MCurrencies.RUB
    List(
      rub.value -> rub.toString
    )
  }

  /**
    * Страницы с формой пополнения баланса.
    *
    * @param nodeId id узла, чей баланс пополняется.
    * @return 200 Ок со страницей-результатом.
    */
  def payment(nodeId: String) = csrf.AddToken {
    isSuNodeContract(nodeId).async { implicit request =>
      _payment(_paymentFormM, Ok)
    }
  }

  private def _payment(bf: Form[MPaymentFormResult], rs: Status)(implicit request: INodeContractReq[_]): Future[Result] = {
    val args = MPaymentTplArgs(
      bf            = _paymentFormM,
      currencyOpts  = _currencies,
      mnode         = request.mnode,
      mcontract     = request.mcontract
    )
    rs(paymentTpl(args))
  }

  /**
    * Сабмит формы выполнения внутреннего платежа.
    *
    * @param nodeId id узла, на кошельках которого барабаним.
    * @return Редирект в биллинг узла, если всё ок.
    */
  def paymentSubmit(nodeId: String) = csrf.Check {
    def logPrefix = s"paymentSubmit($nodeId):"
    isSuNodeContract(nodeId).async { implicit request =>
      _paymentFormM.bindFromRequest().fold(
        {formWithErrors =>
          LOGGER.debug(s"$logPrefix Failed to bind form:\n ${formatFormErrors(formWithErrors)}")
          _payment(formWithErrors, NotAcceptable)
        },
        {res =>
          val mcId = request.mcontract.id.get
          val price = res.price
          val txnFut = slick.db.run {
            bill2Util.increaseBalanceAsIncome(mcId, price)
          }
          for (txn <- txnFut) yield {
            LOGGER.trace(s"$logPrefix txn => $txn")
            Redirect( routes.SysBilling.forNode(nodeId) )
              .flashing(FLASH.SUCCESS -> s"Баланс узла изменён на $price")
          }
        }
      )
    }
  }

}