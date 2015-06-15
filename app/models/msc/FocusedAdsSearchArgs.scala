package models.msc

import models.{AdSearchWrapper, AdSearchWrapper_, AdSearch}
import play.api.mvc.QueryStringBindable
import io.suggest.ad.search.AdSearchConstants._
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
                   boolB      : QueryStringBindable[Boolean],
                   strOptB    : QueryStringBindable[Option[String]]
                  ): QueryStringBindable[FocusedAdsSearchArgs] = {
    new QueryStringBindable[FocusedAdsSearchArgs] with QsbKey1T {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, FocusedAdsSearchArgs]] = {
        for {
          maybeAdSearch       <- adSearchB.bind(key, params)
          maybeWithHeadAd     <- boolB.bind(WITH_HEAD_AD_FN, params)
          maybeFadsLastProdId <- strOptB.bind(FADS_LAST_PROD_ID_FN, params)
        } yield {
          for {
            _adSearch         <- maybeAdSearch.right
            _withHeadAd       <- maybeWithHeadAd.right
            _fadsLastProdId   <- maybeFadsLastProdId.right
          } yield {
            new FocusedAdsSearchArgs with AdSearchWrapper {
              override def _dsArgsUnderlying  = _adSearch
              override def withHeadAd         = _withHeadAd
              override def fadsLastProducerId = _fadsLastProdId
            }
          }
        }
      }

      override def unbind(key: String, value: FocusedAdsSearchArgs): String = {
        boolB.unbind(WITH_HEAD_AD_FN, value.withHeadAd) +
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
  def withHeadAd: Boolean

  /**
   * v2
   * Поле последнего id продьюсера из предыдущей цепочки focused ads.
   * Пришло на смену флагу withHeadAd, которого стало не хватать.
   * @return Some(producerId) или None, если предыдущего запроса не было (когда offset = 0).
   */
  def fadsLastProducerId: Option[String]

}


/** Враппер для [[FocusedAdsSearchArgs]]. */
trait FocusedAdsSearchArgsWrapper extends FocusedAdsSearchArgs with AdSearchWrapper_ {

  override type WT = FocusedAdsSearchArgs

  override def withHeadAd = _dsArgsUnderlying.withHeadAd
  override def fadsLastProducerId = _dsArgsUnderlying.fadsLastProducerId

}
