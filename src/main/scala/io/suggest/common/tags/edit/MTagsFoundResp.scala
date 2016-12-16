package io.suggest.common.tags.edit

import boopickle.Default._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 17:51
  * Description: Модель ответа сервера по теме поиска тегов.
  */

object MTagsFoundResp {

  implicit val pickler: Pickler[MTagsFoundResp] = generatePickler[MTagsFoundResp]

}


/** Модель ответа на запрос поиска тегов. */
case class MTagsFoundResp(
  tags: Seq[MTagFound]
)


/** Инфа по одному найденному тегу.*/
case class MTagFound(
  name  : String,
  count : Int
)

