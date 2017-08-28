package io.suggest.jd.tags

import japgolly.univeq.UnivEq
import play.api.libs.json.OFormat
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.08.17 17:47
  * Description: Jd-тег для вложенного [гипер]текстового контента.
  * Children содержит сконверченные теги из tinyMCE,
  * а конкретно этот тег -- внешний контейнер для текстового элемента в целом.
  *
  * За него можно таскать, его можно целиком удалять, создавать, и всё такое.
  */
object Text {

  /** Поддержка play-json. */
  implicit val TEXT_FORMAT: OFormat[Text] = {
    IDocTag.CHILDREN_IDOC_TAG_FORMAT
      .inmap[Text]( applyPlain, _.children)
  }

  def applyPlain(children: Seq[IDocTag]): Text = {
    apply()(children: _*)
  }

  /** Поддержка UnivEq. */
  implicit def univEq: UnivEq[Text] = UnivEq.derive

}


/** Класс текстового объекта в json-документе.
  *
  * @param children Дочерние теги, т.е. форматированное текстового содержимое.
  */
case class Text()(
                  override val children: IDocTag*
                 )
  extends IDocTag
{

  override def jdTagName = MJdTagNames.TEXT

}
