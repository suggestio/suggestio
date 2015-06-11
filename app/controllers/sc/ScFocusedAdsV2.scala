package controllers.sc

import models.msc.{FocRenderResult, MScApiVsns, AdBodyTplArgs, FocusedAdsSearchArgs}
import play.api.mvc.Result
import util.acl.AbstractRequestWithPwOpt

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
    extends FocusedAdsLogicHttp {

    // При рендере генерятся контейнеры render-результатов, который затем конвертируются в json.
    override type OBT = FocRenderResult

    /** При рендере блоков между итерации передаётся id продьюсера от прошлой карточки.
      * Метод рендера, видя расхождение между текущим id продьюсера и предыдущим, рендереит full focused ad.
      * Если продьюсер совпадает, то рендерится просто focused ad. */
    override type BrAcc_t = Option[String]

    /** Начальный акк рендера блоков содержит id последнего продьюсера, опционально переданный в url qs. */
    override def blockHtmlRenderAcc0: BrAcc_t = {
      _adSearch.fadsLastProducerId
    }

    override def renderOuterBlockAcc(args: AdBodyTplArgs, brAcc0: BrAcc_t): (Future[OBT], BrAcc_t) = {
      // TODO В вышеуказанном комменте написано, что должен делать этот метод на основе id продьюсера
      ???
    }

    override def resultFut: Future[Result] = {
      ???
    }

  }


  // Добавить поддержку v2-логики в getLogic:
  override def getLogicFor(adSearch: FocusedAdsSearchArgs)(implicit request: AbstractRequestWithPwOpt[_]): FocusedAdsLogicHttp = {
    if (adSearch.apiVsn == MScApiVsns.Sjs1) {
      new FocusedLogicHttpV2(adSearch)
    } else {
      super.getLogicFor(adSearch)
    }
  }

}
