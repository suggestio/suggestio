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
final case class MScrollArgs(
                              query              : QueryBuilder,
                              model              : EsModelCommonStaticT,
                              sourcingHelper     : IEsSrbMutator,
                              keepAlive          : TimeValue         = new TimeValue(EsModelUtil.SCROLL_KEEPALIVE_MS_DFLT),
                              maxResults         : Option[Long]      = None,
                              resultsPerScroll   : Int               = EsModelUtil.SCROLL_SIZE_DFLT
                            )


