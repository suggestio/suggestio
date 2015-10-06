package io.suggest.model.n2.node.meta.search

import io.suggest.model.n2.node.MNode
import io.suggest.model.search.{NameSortBaseWrap, NameSortBaseDflt, NameSortBase}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.15 18:57
 * Description: Поисковый аддон для сортировки по нетокенизируемому short-name.
 */
trait NameSort extends NameSortBase {
  override protected def _NAME_FN = MNode.Fields.Meta.BASIC_NAME_SHORT_NOTOK_FN
}


trait NameSortDflt
  extends NameSort
  with NameSortBaseDflt


trait NameSortWrap
  extends NameSort
  with NameSortBaseWrap
