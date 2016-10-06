package io.suggest.model.n2.tag

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.03.16 16:30
  * Description: Инфа по одному аггрегированному тегу.
  */

case class MTagFoundInfo(
  face    : String,
  count   : Int,
  nodeId  : Option[String] = None
)


/** Контейнер результата аггрегации тегов. */
case class MTagsSearchResult(
  tags  : Seq[MTagFoundInfo]
)
