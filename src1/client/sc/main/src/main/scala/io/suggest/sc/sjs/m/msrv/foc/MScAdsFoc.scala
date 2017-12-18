package io.suggest.sc.sjs.m.msrv.foc

import io.suggest.msg.ErrorMsgs
import io.suggest.routes.scRoutes
import io.suggest.sc.resp.MScRespActionTypes
import io.suggest.sc.sjs.m.msrv._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.routes.JsRoutes_ScControllers._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.06.15 17:17
 * Description: Модель асинхронного поиска focused-карточек через focused-APIv2.
 */
object MScAdsFoc {

  /**
   * Отправить на сервер запрос поиска карточек.
   *
   * @param args Аргументы.
   * @return Фьючерс с распарсенным ответом.
   */
  protected def _findJson(args: MFocAdSearch): Future[MScResp] = {
    val route = scRoutes.controllers.Sc.focusedAds(args.toJson)
    MSrv.doRequest(route)
  }

  /** Поиск focused-карточек "в лоб".
    * args.openIndexAdId должен быть None. */
  def find(args: MFocAdSearchNoOpenIndex): Future[MScRespAdsFoc] = {
    if (args.allowReturnJump) {
      Future.failed {
        new IllegalArgumentException( ErrorMsgs.OPEN_AD_ID_MUST_BE_NONE + " " + args.allowReturnJump)
      }
    } else {
      for (mResp <- _findJson(args)) yield {
        mResp.actions
          .head
          .adsFocused.get
      }
    }
  }

  /** Поиск карточек или index-страницы узла-продьюсера.
    * args.openIndexAdId должен быть заполнен соответствующим образом. */
  def findOrIndex(args: MFocAdSearch): Future[IFocResp] = {
    for (mResp <- _findJson(args)) yield {
      // TODO Быдлокод, т.к. пока обрабатывается только первый экшен.
      val rAct = mResp.actions.head
      rAct.action match {
        case MScRespActionTypes.AdsFoc =>
          rAct.adsFocused.get
        case MScRespActionTypes.Index =>
          rAct.index.get
        case other =>
          throw new IllegalArgumentException( ErrorMsgs.FOC_ANSWER_ACTION_INVALID + " " + other )
      }
    }
  }

}
