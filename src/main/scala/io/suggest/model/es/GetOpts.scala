package io.suggest.model.es

import org.elasticsearch.search.fetch.source.FetchSourceContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.15 19:26
 * Description: Модель редко-используемых параметров для GET и Multi-GET.
 */
trait ISourceFiltering {

  def includes: TraversableOnce[String]

  def excludes: TraversableOnce[String]

  def toFetchSourceCtx = new FetchSourceContext(includes.toArray, excludes.toArray)

}

case class SourceFiltering(
  override val includes: Traversable[String] = List("*"),
  override val excludes: Traversable[String] = Nil
)
  extends ISourceFiltering


trait IGetOpts {

  def sourceFiltering: Option[ISourceFiltering]

}

case class GetOpts(
  override val sourceFiltering: Option[ISourceFiltering] = None
)
  extends IGetOpts


object GetOptsDflt
  extends GetOpts()
