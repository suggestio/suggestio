package io.suggest.jd.tags.qd

import io.suggest.common.empty.EmptyProduct
import io.suggest.model.n2.node.meta.colors.MColorData
import io.suggest.primo.{IEqualsEq, IHashCodeLazyVal, ISetUnset}
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.08.17 15:45
  * Description: Модель аттрибутов текста.
  */
object MQdAttrsText {

  /** Поддержка play-json. */
  implicit val QD_ATTRS_FORMAT: OFormat[MQdAttrsText] = (
    (__ \ "b").formatNullable[ISetUnset[Boolean]] and
    (__ \ "i").formatNullable[ISetUnset[Boolean]] and
    (__ \ "u").formatNullable[ISetUnset[Boolean]] and
    (__ \ "r").formatNullable[ISetUnset[Boolean]] and
    (__ \ "c").formatNullable[ISetUnset[MColorData]] and
    (__ \ "g").formatNullable[ISetUnset[MColorData]] and
    (__ \ "l").formatNullable[ISetUnset[String]] and
    (__ \ "s").formatNullable[ISetUnset[String]]
  )(apply, unlift(unapply))

  implicit def univEq: UnivEq[MQdAttrsText] = UnivEq.derive

}


/** Класс модели аттрибутов quill-delta-операции. */
case class MQdAttrsText(
                     bold        : Option[ISetUnset[Boolean]]       = None,
                     italic      : Option[ISetUnset[Boolean]]       = None,
                     underline   : Option[ISetUnset[Boolean]]       = None,
                     strike      : Option[ISetUnset[Boolean]]       = None,
                     color       : Option[ISetUnset[MColorData]]    = None,
                     background  : Option[ISetUnset[MColorData]]    = None,
                     link        : Option[ISetUnset[String]]        = None,
                     src         : Option[ISetUnset[String]]        = None,
                   )
  extends EmptyProduct
  // Для ScalaCSS-рендера: Максимальная скорость работы `==` и hashCode()
  with IHashCodeLazyVal
  with IEqualsEq
{

  /** Подразумевает ли данный набор аттрибутов необходимость использования css-стилей? */
  def isCssStyled = color.isDefined || background.isDefined

}