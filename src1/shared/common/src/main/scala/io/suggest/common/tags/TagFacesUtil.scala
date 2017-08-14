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

  def handleTagsQueryOpt[T](qOpt: Option[String])(f: String => Seq[T]): Seq[T] = {
    qOpt.fold [Seq[T]] (Nil) (f)
  }

  def queryOpt2tags(qOpt: Option[String]): Seq[String] = {
    handleTagsQueryOpt(qOpt) { q =>
      query2tags(q)
    }
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
