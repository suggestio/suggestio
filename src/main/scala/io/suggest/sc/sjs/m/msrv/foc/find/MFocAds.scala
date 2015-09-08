package io.suggest.sc.sjs.m.msrv.foc.find

import io.suggest.sc.sjs.m.msrv.index.MNodeIndex
import io.suggest.sc.sjs.m.msrv.MSrvAnswer
import io.suggest.sc.sjs.util.router.srv.routes
import io.suggest.sjs.common.msg.ErrorMsgs
import io.suggest.sjs.common.xhr.Xhr

import scala.concurrent.{Future, ExecutionContext}
import scala.scalajs.js._
import io.suggest.sc.ScConstants.Resp._
import io.suggest.sc.ScConstants.Focused.FOC_ANSWER_ACTION

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
  protected def _findJson(args: MFocAdSearch)(implicit ec: ExecutionContext): Future[WrappedDictionary[Any]] = {
    val route = routes.controllers.MarketShowcase.focusedAds(args.toJson)
    Xhr.getJson(route).map { json0 =>
      json0.asInstanceOf[Dictionary[Any]]
    }
  }

  /** Поиск focused-карточек "в лоб".
    * args.openIndexAdId должен быть None. */
  def find(args: MFocAdSearchNoOpenIndex)(implicit ec: ExecutionContext): Future[MFocAds] = {
    if (args.openIndexAdId.nonEmpty) {
      Future failed {
        new IllegalArgumentException( ErrorMsgs.OPEN_AD_ID_MUST_BE_NONE + " " + args.openIndexAdId)
      }
    } else {
      _findJson(args)
        .map { apply }
    }
  }

  /** Поиск карточек или index-страницы узла-продьюсера.
    * args.openIndexAdId должен быть заполнен соответствующим образом. */
  def findOrIndex(args: MFocAdSearch)(implicit ec: ExecutionContext): Future[Either[MNodeIndex, MFocAds]] = {
    for (jsonDict <- _findJson(args)) yield {
      MSrvAnswer(jsonDict).actionOpt match {
        case None =>
          throw new NoSuchElementException( ErrorMsgs.FOC_ANSWER_ACTION_MISSING )
        case Some(action) =>
          if (action == FOC_ANSWER_ACTION)
            Right( MFocAds(jsonDict) )
          else if (action == INDEX_RESP_ACTION)
            Left( MNodeIndex(jsonDict) )
          else
            throw new IllegalArgumentException( ErrorMsgs.FOC_ANSWER_ACTION_INVALID + " " + action )
      }
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

  /** inline-стили для focused-карточек. */
  def styles: Option[String]

}


/** Реализация модели ответов на запросы к focused-api. */
case class MFocAds(json: WrappedDictionary[Any]) extends IMFocAds {

  lazy val focusedAdsRaw: WrappedArray[Dictionary[Any]] = {
    // Сервер обычно НЕ присылает пустое поле fads, поэтому нужно вручную отрабатывать его отсутствие.
    json.get(FOCUSED_ADS_FN) match {
      case None =>
        WrappedArray.empty[Dictionary[Any]]
      case Some(arrRaw) =>
        arrRaw.asInstanceOf[ Array[Dictionary[Any]] ]
    }
  }

  override def styles: Option[String] = {
    json.get(STYLES_FN)
      .asInstanceOf[Option[String]]
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
