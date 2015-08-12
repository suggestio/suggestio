package io.suggest.sc.sjs.m.msrv.foc.find

import io.suggest.sc.sjs.m.msrv.MSrvUtil
import io.suggest.sc.sjs.util.router.srv.routes

import scala.concurrent.{Future, ExecutionContext}
import scala.scalajs.js.{WrappedArray, Dictionary, Any, Array}
import io.suggest.sc.ScConstants.Resp._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.06.15 17:17
 * Description: Модель асинхронного поиска focused-карточек через focused-APIv2.
 */
object MFocAds {

  /**
   * Отправить на сервер запрос поиска карточек.
   * @param args Аргументы.
   * @return Фьючерс с распарсенным ответом.
   */
  def find(args: MFocAdSearch)(implicit ec: ExecutionContext): Future[MFocAds] = {
    val route = routes.controllers.MarketShowcase.focusedAds(args.toJson)
    MSrvUtil.reqJson(route) map { json =>
      new MFocAds( json.asInstanceOf[Dictionary[Any]] )
    }
  }

}


/** Интерфейс экземпляра модели (распарсенного ответа сервера). */
trait IMFocAds {
  /** Итератор распарсенных focused-карточек в рамках запрошенной порции. */
  def focusedAdsIter: Iterator[MFocAd]

  /** Кол-во карточек в текущем наборе. */
  def fadsCount: Int

  /** Общее кол-во карточек во всей запрошенной выборке. */
  def totalCount: Int

}


/** Реализация модели ответов на запросы к focused-api. */
class MFocAds(json: Dictionary[Any]) extends IMFocAds {

  lazy val focusedAdsRaw: WrappedArray[Dictionary[Any]] = {
    val arrRaw = json(FOCUSED_ADS_FN)
    if (arrRaw != null) {
      arrRaw.asInstanceOf[Array[Dictionary[Any]]]
    } else {
      WrappedArray.empty
    }
  }
  
  override def focusedAdsIter: Iterator[MFocAd] = {
    focusedAdsRaw
      .iterator 
      .map { MFocAd.apply }
  }

  override def fadsCount: Int = focusedAdsRaw.size

  override def totalCount: Int = {
    json(TOTAL_COUNT_FN)
      .asInstanceOf[Int]
  }

}
