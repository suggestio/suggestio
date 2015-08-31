package io.suggest.sc.sjs.m.msrv.ads.find

import io.suggest.sc.sjs.m.mgrid.MGridParamsJsonRaw

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.05.15 14:54
 * Description: JSON Stub для доступа к полям JSON-ответа на запрос к findAds v2.
 */

@js.native
final class MFindAdsRespJson extends js.Object {

  /** Отрендеренный карточки плитки (блоки) в необходимом порядке.
    * Отсутствие поля block означает пустой массив. */
  val mads: UndefOr[js.Array[MFoundAdJson]] = js.native

  /** Отрендеренные стили карточек. */
  val css: UndefOr[String] = js.native

  /** Параметры отображения плитки. */
  val params: UndefOr[MGridParamsJsonRaw] = js.native

}