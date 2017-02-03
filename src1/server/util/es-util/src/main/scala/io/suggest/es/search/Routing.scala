package io.suggest.es.search

import org.elasticsearch.action.search.SearchRequestBuilder

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.12.14 13:03
 * Description: Добавление поля настройки роутинга dyn-search запроса.
 */
trait Routing extends DynSearchArgs {

  /** Дополнительно задать ключ для роутинга. */
  def withRouting: Seq[String]

  override def prepareSearchRequest(srb: SearchRequestBuilder): SearchRequestBuilder = {
    val srb1 = super.prepareSearchRequest(srb)
    // Заливаем ключи роутинга, если он задан.
    if (withRouting.nonEmpty)
      srb1.setRouting(withRouting : _*)
    srb1
  }

  override def sbInitSize: Int = {
    collStringSize(withRouting, super.sbInitSize)
  }

  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("withRouting", withRouting, super.toStringBuilder)
  }

}

trait RoutingDflt extends Routing {
  override def withRouting: Seq[String] = Nil
}

trait RoutingWrap extends Routing with DynSearchArgsWrapper {
  override type WT <: Routing
  override def withRouting = _dsArgsUnderlying.withRouting
}
