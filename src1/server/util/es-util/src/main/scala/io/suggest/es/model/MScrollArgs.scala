package io.suggest.es.model

import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.index.query.QueryBuilder

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.06.17 11:05
  * Description: Модель-контейнер данных для осуществления скролла в индексах.
  * Изначально параметры пробрасывались поштучно, но это очень некрасиво выглядело.
  */

trait IScrollArgs {

  def query: QueryBuilder

  def model: EsModelCommonStaticT

  /** Используемый в работе Sourcing Helper. */
  def sourcingHelper: IEsSrbMutator

  def keepAlive: TimeValue

  def maxResults: Option[Long]

  def resultsPerScroll: Int

}


/** Реализация [[IScrollArgs]]. */
case class MScrollArgs(
                        override val query              : QueryBuilder,
                        override val model              : EsModelCommonStaticT,
                        override val sourcingHelper     : IEsSrbMutator,
                        override val keepAlive          : TimeValue         = new TimeValue(EsModelUtil.SCROLL_KEEPALIVE_MS_DFLT),
                        override val maxResults         : Option[Long]      = None,
                        override val resultsPerScroll   : Int               = EsModelUtil.SCROLL_SIZE_DFLT
                      )
  extends IScrollArgs


