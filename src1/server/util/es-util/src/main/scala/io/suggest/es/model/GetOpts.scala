package io.suggest.es.model

import org.elasticsearch.search.fetch.subphase.FetchSourceContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.15 19:26
 * Description: Модель редко-используемых параметров для GET и Multi-GET.
 */
case class SourceFiltering(
                            includes: Traversable[String] = List("*"),
                            excludes: Traversable[String] = Nil
                          ) {

  def toFetchSourceCtx: FetchSourceContext = {
    new FetchSourceContext(true, includes.toArray, excludes.toArray)
  }

}


case class GetOpts(
                    sourceFiltering: Option[SourceFiltering] = None
                  )


object GetOptsDflt extends GetOpts()
