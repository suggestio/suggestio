package models.msc

import models.{AdSearchWrapper, AdSearchWrapper_, AdSearch}
import play.api.mvc.QueryStringBindable
import io.suggest.ad.search.AdSearchConstants._
import util.PlayMacroLogsDyn
import util.qsb.QsbKey1T
import views.js.sc.m._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.06.15 17:30
 * Description: Расширение модели AdSearch данными, интересующими focused-логику.
 * Для дедубликации кода, почти всё реализовано через врапперы и прочее.
 */
object FocusedAdsSearchArgs {

  /** Маппер экземпляров модели для url query string. */
  implicit def qsb(implicit
                   adSearchB  : QueryStringBindable[AdSearch],
                   boolOptB   : QueryStringBindable[Option[Boolean]],
                   strOptB    : QueryStringBindable[Option[String]]
                  ): QueryStringBindable[FocusedAdsSearchArgs] = {
    new QueryStringBindable[FocusedAdsSearchArgs] with QsbKey1T {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, FocusedAdsSearchArgs]] = {
        for {
          maybeAdSearch       <- adSearchB.bind (key,                   params)
          maybeWithHeadAd     <- boolOptB.bind  (WITH_HEAD_AD_FN,       params)
          maybeFadsLastProdId <- strOptB.bind   (FADS_LAST_PROD_ID_FN,  params)
          maybeOnlyWithAd     <- strOptB.bind   (ONLY_WITH_AD_ID_FN,    params)
        } yield {
          for {
            _adSearch         <- maybeAdSearch.right
            _withHeadAd       <- maybeWithHeadAd.right
            _fadsLastProdId   <- maybeFadsLastProdId.right
            _onlyWithAdId     <- maybeOnlyWithAd.right
          } yield {
            new FocusedAdsSearchArgsWrappedImpl {
              override def _dsArgsUnderlying  = _adSearch
              override def withHeadAd         = _withHeadAd contains true
              override def fadsLastProducerId = _fadsLastProdId
              override def onlyWithAdId       = _onlyWithAdId
            }
          }
        }
      }

      override def unbind(key: String, value: FocusedAdsSearchArgs): String = {
        boolOptB.unbind(WITH_HEAD_AD_FN, Some(value.withHeadAd)) +
          "&" + adSearchB.unbind(key, value)
      }

      /** Js-код поддержки интеграции модели с jsrouter. */
      override def javascriptUnbind: String = {
        scFocusedAdSearchJsUnbindTpl(KEY_DELIM).body
      }
    }
  }

}


/** Экземпляр модели в виде трейта для возможности простого враппинга, встраивания и расширения. */
trait FocusedAdsSearchArgs extends AdSearch {

  /**
   * v1
   * Поле наличия или отсутствия head-ad рендера в json-ответе.
   * @return true: означает, что нужна начальная страница с html.
   *         false: возвращать только json-массив с отрендеренными блоками,
   *         без html-страницы с первой карточкой.
   */
  def withHeadAd: Boolean = false

  /**
   * v2
   * Поле последнего id продьюсера из предыдущей цепочки focused ads.
   * Пришло на смену флагу withHeadAd, которого стало не хватать.
   *
   * Пока собирались запилить, архитектура focused-выдачи изменилась, необходимость этой опции упала,
   * и client-side реализация была отложена.
   * @return Some(producerId) или None, если предыдущего запроса не было (когда offset = 0).
   */
  def fadsLastProducerId: Option[String] = None

  /**
   * v2
   * Контроллер должен убедиться, что карточка с указанным id присутствует в ответе.
   * Это необходимо на случай рассинхронизации offset на клиенте и на сервере.
   * Такое возможно, если какая-то новая карточка опубликовалась, или скрылась.
   * @return None, если дополнительно допиливать результат не требуется.
   *         Some(madId), если необходимо проконтроллировать наличие указанной карточки в результате.
   */
  def onlyWithAdId: Option[String] = None

}

class FocusedAdsSearchArgsImpl
  extends FocusedAdsSearchArgs
  with PlayMacroLogsDyn


abstract class FocusedAdsSearchArgsWrappedImpl
  extends AdSearchWrapper
  with FocusedAdsSearchArgs


/** Враппер для [[FocusedAdsSearchArgs]]. */
abstract class FocusedAdsSearchArgsWrapper extends AdSearchWrapper_ with FocusedAdsSearchArgs {

  override type WT = FocusedAdsSearchArgs

  override def withHeadAd         = _dsArgsUnderlying.withHeadAd
  override def fadsLastProducerId = _dsArgsUnderlying.fadsLastProducerId
  override def onlyWithAdId       = _dsArgsUnderlying.onlyWithAdId

}
