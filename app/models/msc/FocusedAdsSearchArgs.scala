package models.msc

import models.{AdSearchWrapper, AdSearchWrapper_, AdSearch}
import play.api.mvc.QueryStringBindable
import io.suggest.ad.search.AdSearchConstants.WITH_HEAD_AD_FN

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
                   boolB      : QueryStringBindable[Boolean]
                  ): QueryStringBindable[FocusedAdsSearchArgs] = {
    new QueryStringBindable[FocusedAdsSearchArgs] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, FocusedAdsSearchArgs]] = {
        for {
          maybeAdSearch   <- adSearchB.bind(key, params)
          maybeWithHeadAd <- boolB.bind(WITH_HEAD_AD_FN, params)
        } yield {
          for {
            _adSearch     <- maybeAdSearch.right
            _withHeadAd   <- maybeWithHeadAd.right
          } yield {
            new FocusedAdsSearchArgs with AdSearchWrapper {
              override def _dsArgsUnderlying = _adSearch
              override def withHeadAd = _withHeadAd
            }
          }
        }
      }

      override def unbind(key: String, value: FocusedAdsSearchArgs): String = {
        boolB.unbind(WITH_HEAD_AD_FN, value.withHeadAd) +
          "&" + adSearchB.unbind(key, value)
      }
    }
  }

}


/** Экземпляр модели в виде трейта для возможности простого враппинга, встраивания и расширения. */
trait FocusedAdsSearchArgs extends AdSearch {

  /**
   * Поле наличия или отсутствия head-ad рендера в json-ответе.
   * @return true: означает, что нужна начальная страница с html.
   *         false: возвращать только json-массив с отрендеренными блоками,
   *         без html-страницы с первой карточкой.
   */
  def withHeadAd: Boolean

}


/** Враппер для [[FocusedAdsSearchArgs]]. */
trait FocusedAdsSearchArgsWrapper extends FocusedAdsSearchArgs with AdSearchWrapper_ {

  override type WT = FocusedAdsSearchArgs

  override def withHeadAd = _dsArgsUnderlying.withHeadAd

}
