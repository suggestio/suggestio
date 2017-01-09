package io.suggest.common.tags.search

import boopickle.Default._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.03.16 16:30
  * Description: Инфа по одному аггрегированному тегу.
  */

object MTagFound {

  implicit val pickler: Pickler[MTagFound] = generatePickler[MTagFound]

}

case class MTagFound(
  face    : String,
  count   : Int,
  nodeId  : Option[String] = None
)



object MTagsFound {

  implicit val pickler: Pickler[MTagsFound] = {
    implicit val mtfP = MTagFound.pickler
    generatePickler[MTagsFound]
  }

}

/** Контейнер результата аггрегации тегов. */
case class MTagsFound(
  tags  : Seq[MTagFound]
)
