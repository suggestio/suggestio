package controllers.sc

import models.jsm.FocusedAdsResp2
import models.msc._
import play.api.mvc.Result
import play.twirl.api.Html
import util.acl.AbstractRequestWithPwOpt
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Play.{current, configuration}

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

  /**
   * Активен ли разнородный рендер последовательных карточек с одинаковым producer_id?
   * @return false и все карточки в списке будут отрендерены как заглавные (с обрамлением продьюсера).
   *         true: Рендер заглавных/обычных карточек в наборе будет идти как-то так: AaaaBbCcc.
   */
  val OPTIMIZE_SAME_PRODUCER_ADS: Boolean = configuration.getBoolean("sc.focused.sameproducer.optimized") getOrElse false


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


    /** Запуск фонового рендера одного блока. */
    override def renderOneBlockAcc(args: AdBodyTplArgs, prevProdIdOpt: BrAcc_t): (Future[OBT], BrAcc_t) = {
      val renderMinified = OPTIMIZE_SAME_PRODUCER_ADS && (prevProdIdOpt contains args.brArgs.mad.producerId)
      val htmlFut: Future[Html] = {
        if (renderMinified) {
          // Карточка от того же продьюсера, что и предыдущая. Рендерим без лишнего обрамления.
          renderBlockHtml(args)
        } else {
          // Эту карточку надо рендерить как заглавную
          focAdsRenderArgsFor(args)
            .flatMap { renderFocusedFut }
        }
      }
      // Минифицировать html, завернуть в ответ.
      val resFut = htmlFut map { html =>
        import MFocRenderModes._
        val forRenderMode = if (renderMinified) Normal else Full
        FocRenderResult(html2str4json(html), forRenderMode, args.index)
      }
      // Сформировать новый акк и вернуть всё наверх.
      val acc1 = args.producer.id
      (resFut, acc1)
    }


    /** Сборка HTTP-ответа APIv2. */
    override def resultFut: Future[Result] = {
      val _blockHtmlsFut = blocksHtmlsFut
      for {
        madsCount   <- madsCountIntFut
        blockHtmls  <- _blockHtmlsFut
      } yield {
        val resp = FocusedAdsResp2(blockHtmls, madsCount)
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
