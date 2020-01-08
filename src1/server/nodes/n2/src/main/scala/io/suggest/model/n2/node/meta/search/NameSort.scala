package io.suggest.model.n2.node.meta.search

import io.suggest.es.search.NameSortBase
import io.suggest.model.n2.node.MNodeFields

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.15 18:57
 * Description: Поисковый аддон для сортировки по нетокенизируемому short-name.
 */
trait NameSort extends NameSortBase {
  override protected def _NAME_FN = MNodeFields.Meta.BASIC_NAME_SHORT_NOTOK_FN
}
