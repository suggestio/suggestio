package io.suggest.lk.adv.fsm

import io.suggest.common.fut.FutureUtil
import io.suggest.fsm.StateData
import io.suggest.lk.price.m.{Resp, XhrResult}
import io.suggest.lk.price.vm.PriceUrlInput
import io.suggest.lk.price.vm.PriceVal
import io.suggest.msg.{ErrorMsgs, WarnMsgs}
import io.suggest.sjs.common.fsm.SjsFsm
import io.suggest.sjs.common.vm.find.IFindEl
import io.suggest.sjs.common.vm.input.FormDataVmT
import io.suggest.sjs.dt.period.vm.InfoContainer
import io.suggest.sjs.common.xhr.Xhr
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.concurrent.Promise
import scala.util.{Failure, Success}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.03.16 15:45
  * Description: Трейты для быстрой сборки аддона обновления стоимости размещения с сервера.
  */

/** Интерфейс для FSM-метода запуска обновления формы с сервера. */
trait IUpdatePriceDataStart {
  /** Запустить запрос к серверу, обновив данные текущего состояния. */
  protected def _upStart(): Unit
}


/** Получить данные из  */
trait UpdateAdvFormPrice extends SjsFsm with StateData with IUpdatePriceDataStart {

  /** Объект-компаньон для доступа к форме. */
  protected def _formCompanion: IFindEl { type T <: FormDataVmT }

  /** Сохранить timestamp отправки запроса в состояние FSM. */
  protected def _upSaveXhrTstamp(tstampOpt: Option[Long], sd0: SD): SD

  override protected def _upStart(): Unit = {
    val forRes = for {
      form          <- _formCompanion.find()
      priceUrlInput <- PriceUrlInput.find()
      method        <- priceUrlInput.method
      priceUrl      <- priceUrlInput.url
    } yield {
      // Запустить отправку XHR на сервер.
      val fut0 = Xhr.successIf200 {
        FutureUtil.tryCatchFut {
          Xhr.sendRaw(
            method = method,
            url    = priceUrl,
            body   = form.formData
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
    _stateData = _upSaveXhrTstamp(Some(tstamp), _stateData)
  }


  /** Проверить timestamp запроса по данным состояния FSM. */
  protected def _upXhrGetTstamp(sd0: SD): Option[Long]


  /** Трейт состояния, ожидающего данные формы от сервера. */
  protected trait GetPriceStateT extends FsmEmptyReceiverState {

    /** Дополнить ресивер поддержкой получения ответа по данным. */
    override def receiverPart: Receive = super.receiverPart.orElse {
      // Получен ответ от сервера, необязательно он положительный.
      case res: XhrResult =>
        val sd0 = _stateData
        if ( !_upXhrGetTstamp(sd0).contains(res.timestamp) ) {
          // Принят неактуальный ответ от сервера. Ждём следующего.
          LOG.log(WarnMsgs.ADV_DIRECT_XHR_TS_DROP, msg = res)

        } else {
          res.result match {
            case Success(resp) =>
              _handleGetPriceXhrResp(resp)
            case Failure(ex) =>
              LOG.error(ErrorMsgs.ADV_DIRECT_FORM_PRICE_FAIL, ex)
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
      _stateData = _upSaveXhrTstamp(None, _stateData)
    }

  }

}
