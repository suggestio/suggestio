package io.suggest.sc.sjs.m.msrv.ads.find

import io.suggest.sc.sjs.m.mgrid.MGridParamsJsonWrapper
import io.suggest.sc.sjs.util.router.srv.routes
import io.suggest.sjs.common.xhr.Xhr

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js.UndefOr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.05.15 14:40
 * Description: Модель для запроса поиска рекламных карточек на сервере и доступу к ответу-результату как к модели.
 */
object MFindAds {

  /**
   * Запуск поиска рекламных карточек на сервере.
   * @param adSearch Поисковые критерии, которые может понять jsRouter.
   * @return Фьючерс с результатом запроса.
   */
  def findAds(adSearch: MFindAdsReq)(implicit ec: ExecutionContext): Future[MFindAds] = {
    val route = routes.controllers.Sc.findAds( adSearch.toJson )
    for (json <- Xhr.getJson(route)) yield {
      val json1 = json.asInstanceOf[MFindAdsRespJson]
      MFindAds(json1)
    }
  }

}


/**
 * Реализация wrap-модели над [[MFindAdsRespJson]], чтобы получить более человеческий доступ к JSON ответа.
 * @param json JSON-распарсенный ответ сервера.
 */
sealed case class MFindAds(json: MFindAdsRespJson) {

  val mads: Seq[MFoundAdJson] = {
    val raws = UndefOr.undefOr2ops( json.mads )
    if (raws.isEmpty) {
      Nil
    } else {
      raws.get.toSeq
    }
  }

  val css = json.css.toOption

  val params = {
    json.params
      .toOption
      .map(MGridParamsJsonWrapper.apply)
  }

  override def toString: String = {
    getClass.getSimpleName + "(" + mads.size + "ads,css=" +
      css.fold("0")(_.length + "b") +
      ",params=" + params +
      ")"
  }

}

