package models.msc

import models._
import play.api.mvc.QueryStringBindable
import io.suggest.ad.search.AdSearchConstants._
import io.suggest.model.play.qsb.QsbKey1T
import util.PlayMacroLogsDyn
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
        val f = key1F(key)
        for {
          maybeAdSearch       <- adSearchB.bind (key,                       params)
          maybeWithHeadAd     <- boolOptB.bind  (WITH_HEAD_AD_FN,           params)
          openInxAdIdEith     <- strOptB.bind   (f(OPEN_INDEX_AD_ID_FN),    params)
          maybeAdIdLookup     <- strOptB.bind   (f(AD_ID_LOOKUP_FN),        params)
        } yield {
          for {
            _adSearch         <- maybeAdSearch.right
            _withHeadAd       <- maybeWithHeadAd.right
            _openInxAdId      <- openInxAdIdEith.right
            _adIdLookup       <- maybeAdIdLookup.right
          } yield {
            new FocusedAdsSearchArgsWrappedImpl {
              override def _dsArgsUnderlying  = _adSearch
              override def withHeadAd         = _withHeadAd.contains(true)
              override def focOpenIndexAdId   = _openInxAdId
              override def focAdIdLookup      = _adIdLookup
            }
          }
        }
      }

      override def unbind(key: String, value: FocusedAdsSearchArgs): String = {
        val f = key1F(key)
        Iterator(
          adSearchB .unbind     (key,                     value),
          boolOptB  .unbind     (WITH_HEAD_AD_FN,         Some(value.withHeadAd)),
          strOptB   .unbind     (f(OPEN_INDEX_AD_ID_FN),  value.focOpenIndexAdId),
          strOptB   .unbind     (f(AD_ID_LOOKUP_FN),      value.focAdIdLookup)
        )
          .filter(_.nonEmpty)
          .mkString("&")
      }

      /** Js-код поддержки интеграции модели с jsrouter. */
      override def javascriptUnbind: String = {
        scFocusedAdSearchJsUnbindTpl(KEY_DELIM).body
      }

    } // new QSB[]
  }

}


/** Экземпляр модели в виде трейта для возможности простого враппинга, встраивания и расширения. */
trait FocusedAdsSearchArgs extends AdSearch {

  /**
   * v1
   * Поле наличия или отсутствия head-ad рендера в json-ответе.
   *
   * @return true: означает, что нужна начальная страница с html.
   *         false: возвращать только json-массив с отрендеренными блоками,
   *         без html-страницы с первой карточкой.
   */
  def withHeadAd: Boolean = false


  /** id карточки, для которой допускается вернуть index её продьюсера. */
  def focOpenIndexAdId : Option[String] = None


  /**
    * v2
    * Контроллер должен узнать начальные параметры текущей focused-выдачи для карточки с указанным id.
    * присутствует в ответе.
    *
    * Это необходимо на случай неизвестности offset/size на клиенте.
    *
    * @return None, если дополнительно допиливать результат не требуется.
    *         Some(madId), если необходимо проконтроллировать наличие указанной карточки в результате.
    */
  def focAdIdLookup: Option[String] = None

}

class FocusedAdsSearchArgsImpl
  extends FocusedAdsSearchArgs
  with PlayMacroLogsDyn


/** wrap-значение adSearch + значения, специфичные для [[FocusedAdsSearchArgs]]. */
abstract class FocusedAdsSearchArgsWrappedImpl
  extends FocusedAdsSearchArgs
  with AdSearchWrap


/** Враппер для [[FocusedAdsSearchArgs]]. */
trait FocusedAdsSearchArgsWrapper extends AdSearchWrapper_ with FocusedAdsSearchArgs {

  override type WT = FocusedAdsSearchArgs

  override def withHeadAd         = _dsArgsUnderlying.withHeadAd
  override def focOpenIndexAdId   = _dsArgsUnderlying.focOpenIndexAdId
  override def focAdIdLookup      = _dsArgsUnderlying.focAdIdLookup

}

abstract class FocusedAdsSearchArgsWrapperImpl
  extends FocusedAdsSearchArgsWrapper

