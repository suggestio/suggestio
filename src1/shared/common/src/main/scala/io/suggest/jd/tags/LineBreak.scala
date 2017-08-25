package io.suggest.jd.tags

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.08.17 12:29
  * Description: Line break.
  */
case object LineBreak extends IDocTag {

  override def jdTagName = MJdTagNames.LINE_BREAK

  override def children = Nil

}
