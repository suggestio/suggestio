package io.suggest.jd.tags

import io.suggest.common.geom.coord.MCoords2di
import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.08.17 22:23
  * Description: Заворачивание дочерних элементов в контейнер, спозиционированный абсолютно.
  */
object AbsPos {

  /** Поддержка play-json. */
  implicit val ABS_POS_FORMAT: OFormat[AbsPos] = (
    (__ \ "tl").format[MCoords2di] and
    IDocTag.CHILDREN_IDOC_TAG_FORMAT
  )(apply, unlift(unapply))


  def a(topLeft: MCoords2di)(children: IDocTag*): AbsPos = {
    apply(topLeft, children)
  }

  implicit def univEq: UnivEq[AbsPos] = UnivEq.force

}


/** Тег сборки контейнера с абсолютным позиционированием.
  *
  * @param topLeft Верхний левый угол контейнера.
  * @param children Дочерние теги.
  */
case class AbsPos(
                   topLeft: MCoords2di,
                   override val children: Seq[IDocTag]
                 )
  extends IDocTag {

  override def jdTagName = MJdTagNames.ABS_POS

  override def withChildren(children: Seq[IDocTag]): AbsPos = {
    copy( children = children )
  }

  override def shrink: Seq[IDocTag] = {
    if (children.isEmpty) {
      Nil
    } else {
      super.shrink
    }
  }

}
