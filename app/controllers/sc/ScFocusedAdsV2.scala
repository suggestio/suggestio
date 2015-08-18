package controllers.sc

import models.jsm.FocusedAdsResp2
import models.msc._
import play.api.mvc.Result
import util.acl.AbstractRequestWithPwOpt
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import views.html.sc.foc._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.06.15 18:12
 * Description: Логика Focused Ads API v2, которая претерпела значителньые изменения в сравнении с API v1.
 *
 * Все карточки рендерятся одним списком json-объектов, которых изначально было два типа:
 * - Focused ad: _focusedAdTpl.
 * - Full focused ad: _focusedAdsTpl
 * Этот неоднородный массив отрабатывается конечным автоматом на клиенте, который видя full-часть понимает,
 * что последующие за ней не-full части относяться к этому же продьюсеру.
 *
 * Разные куски списка могут прозрачно склеиваться.
 *
 * Так же, сервер может вернуть вместо вышеописанного ответа:
 * - index выдачу другого узла.
 * - команду для перехода по внешней ссылке.
 */
trait ScFocusedAdsV2 extends ScFocusedAds {

  /** Реализация v2-логики. */
  protected class FocusedLogicHttpV2(val _adSearch: FocusedAdsSearchArgs)
                                    (implicit val _request: AbstractRequestWithPwOpt[_])
    extends FocusedAdsLogicHttp with NoBrAcc {

    override def apiVsn = MScApiVsns.Sjs1

    // При рендере генерятся контейнеры render-результатов, который затем конвертируются в json.
    override type OBT = FocRenderResult

    override def renderOuterBlock(args: AdBodyTplArgs): Future[OBT] = {
      val fullArgsFut = focAdsRenderArgsFor(args)
      val bodyFut = renderBlockHtml(args)
        .map { html2str4json }
      val controlsFut = fullArgsFut map { fullArgs =>
        html2str4json(
          _controlsTpl(fullArgs)
        )
      }
      for {
        body      <- bodyFut
        controls  <- controlsFut
      } yield {
        FocRenderResult(
          madId       = args.brArgs.mad.id.get,
          body        = body,
          controls    = controls,
          producerId  = args.brArgs.mad.producerId,
          humanIndex  = args.index,
          index       = args.index - 1
        )
      }
    }

    /** Сборка HTTP-ответа APIv2. */
    override def resultFut: Future[Result] = {
      val _blockHtmlsFut = blocksHtmlsFut
      val _stylesFut = jsAdsCssFut
      for {
        madsCount   <- madsCountIntFut
        blockHtmls  <- _blockHtmlsFut
        _styles     <- _stylesFut
      } yield {
        val resp = FocusedAdsResp2(blockHtmls, madsCount, _styles)
        Ok(resp.toJson)
      }
    }

  }


  // Добавить поддержку v2-логики в getLogic()
  override def getLogicFor(adSearch: FocusedAdsSearchArgs)
                          (implicit request: AbstractRequestWithPwOpt[_]): FocusedAdsLogicHttp = {
    if (adSearch.apiVsn == MScApiVsns.Sjs1) {
      new FocusedLogicHttpV2(adSearch)
    } else {
      super.getLogicFor(adSearch)
    }
  }

}
