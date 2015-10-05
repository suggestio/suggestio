package io.suggest.model.n2.extra.search

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.10.15 22:37
 * Description: Объединяющий поисковый аддон для мега-поля MNode.extras.
 */
trait ExtrasSearch
  extends AdnIsTest
  with AdnSinks


/** Дефолтовые критерии для поискового аддона поля MNode.extras. */
trait ExtrasSearchDflt
  extends AdnIsTestDflt
  with AdnSinksDflt


/** Wrap-аддон для критериев поля MNode.extras. */
trait ExtrasSearchWrap
  extends AdnIsTestWrap
  with AdnSinksWrap
{
  override type WT <: ExtrasSearch
}
