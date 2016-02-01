package io.suggest.lk.adv.direct.fsm.states

import io.suggest.common.fut.FutureUtil
import io.suggest.lk.adv.direct.fsm.FsmStubT
import io.suggest.lk.adv.direct.m.price.{XhrResult, Resp}
import io.suggest.lk.adv.direct.vm.{PriceUrlInput, Form}
import io.suggest.lk.price.vm.PriceVal
import io.suggest.sjs.dt.period.vm.InfoContainer
import io.suggest.sjs.common.msg.{WarnMsgs, ErrorMsgs}
import io.suggest.sjs.common.util.ISjsLogger
import io.suggest.sjs.common.xhr.Xhr

import scala.concurrent.Promise
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.01.16 11:38
 * Description: Поддержка аддона для запроса и получения отрендеренной цены
 * и кусков формы с сервера.
 *
 * Т.к. форма в целом стабильна. то состояние не переключается, а только меняются данные в состоянии.
 */
trait GetData extends FsmStubT with ISjsLogger {

  /** Запустить запрос к серверу, обновив данные текущего состояния. */
  override protected def _needUpdateData(): Unit = {
    val forRes = for {
      form          <- Form.find()
      priceUrlInput <- PriceUrlInput.find()
      method        <- priceUrlInput.method
      priceUrl      <- priceUrlInput.url
    } yield {
      // Запустить отправку XHR на сервер.
      val fut0 = Xhr.successWithStatus(200) {
        FutureUtil.tryCatchFut {
          Xhr.send(
            method = method,
            url = priceUrl,
            body = Some(form.formData)
          )
        }
      }
      // Десериализовать сырой XHR-ответ.
      for (xhr <- fut0) yield {
        Resp.fromJson(xhr.response)
      }
    }

    // Если for() пустой, то надо будет отправить экземпшен в receive.
    val p0 = Promise[Resp]()
    forRes.fold {
      val ex = new NoSuchElementException( ErrorMsgs.ADV_DIRECT_FORM_PRICE_URL_METHOD_MISS )
      p0.failure(ex)
    } { _fut =>
      p0.completeWith( _fut )
    }

    // Подписать события фьючерс на этот FSM.
    val tstamp = _sendFutResBackTimestamped(p0.future, XhrResult)

    // Сохранить timestamp отправленного запроса в состояние
    _stateData = _stateData.copy(
      getPriceTs = Some(tstamp)
    )
  }


  /** Трейт состояния, ожидающего данные формы от сервера. */
  trait GetDataStateT extends FsmEmptyReceiverState {

    /** Дополнить ресивер поддержкой получения ответа по данным. */
    override def receiverPart: Receive = super.receiverPart orElse {
      // Получен ответ от сервера, необязательно он положительный.
      case res: XhrResult =>
        val sd0 = _stateData
        if ( !sd0.getPriceTs.contains(res.timestamp) ) {
          // Принят неактуальный ответ от сервера. Ждём следующего.
          log(WarnMsgs.ADV_DIRECT_XHR_TS_DROP + " " + res)

        } else {
          res.result match {
            case Success(resp) =>
              _handleGetPriceXhrResp(resp)
            case Failure(ex) =>
              error(ErrorMsgs.ADV_DIRECT_FORM_PRICE_FAIL, ex)
              _getPriceCompleted()
          }
        }
    }


    /** Получен ответ сервера на запрос цены и других элементов формы. */
    def _handleGetPriceXhrResp(resp: Resp): Unit = {
      // Отрендерить куски ответа в соотв.места на странице.
      // Рендерим инфу по датам:
      for {
        ic  <- InfoContainer.find()
        ic2 <- InfoContainer.ofHtml( resp.periodHtml )
      } {
        ic.replaceWith(ic2)
      }

      // Рендерим данные по рассчитанной стоимости размещения
      for (pv <- PriceVal.find()) {
        pv.setContent( resp.priceHtml )
      }

      _getPriceCompleted()
    }

    /** Завершение логики работы getPrice. Не важно, успешно или нет. */
    def _getPriceCompleted(): Unit = {
      _stateData = _stateData.copy(
        getPriceTs = None
      )
    }

  }

}
