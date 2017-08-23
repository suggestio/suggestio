package io.suggest.jd.tags

import io.suggest.common.geom.coord.MCoords2di
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
  )(rawApply, unlift(rawUnapply))


  def rawApply(topLeft: MCoords2di, children: Seq[IDocTag]): AbsPos = {
    apply(topLeft)(children: _*)
  }

  def rawUnapply(ap: AbsPos): Option[(MCoords2di, Seq[IDocTag])] = {
    Some((ap.topLeft, ap.children))
  }

}


/** Тег сборки контейнера с абсолютным позиционированием.
  *
  * @param topLeft Верхний левый угол контейнера.
  * @param children Дочерние теги.
  */
case class AbsPos(
                   topLeft: MCoords2di
                 )(
                   override val children: IDocTag*
                 )
  extends IDocTag {

  override def dtName = MJdTagNames.AbsPos

}
