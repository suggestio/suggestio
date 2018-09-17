package io.suggest.common.tags

import io.suggest.text.StringUtil

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.10.16 13:44
  * Description: Утиль для работы со строковыми названияем тегов.
  */
object TagFacesUtil {

  def query2tags(q: String): Seq[String] = {
    StringUtil
      .trimLeft( q )
      .split("[,;\\s#]")
      .toSeq
  }

  def queryOpt2tags(qOpt: Option[String]): Seq[String] = {
    qOpt.fold [Seq[String]] (Nil) (query2tags)
  }


  /** Рендер списка тегов в опциональную строку. */
  def tags2StringOpt(tags: Seq[String]): Option[String] = {
    if (tags.isEmpty) {
      None
    } else {
      Some( tags2String(tags) )
    }
  }
  def tags2String(tags: Seq[String]): String = {
    tags.mkString("#", ", #", "")
  }

}
