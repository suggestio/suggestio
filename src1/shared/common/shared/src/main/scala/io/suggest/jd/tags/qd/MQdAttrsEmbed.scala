package io.suggest.jd.tags.qd

import io.suggest.common.empty.EmptyProduct
import io.suggest.common.geom.d2.MSize2di
import io.suggest.err.ErrorConstants
import io.suggest.math.MathConst
import io.suggest.primo.{IEqualsEq, IHashCodeLazyVal, ISetUnset}
import io.suggest.scalaz.ScalazUtil
import io.suggest.text.StringUtil
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.functional.syntax._
import play.api.libs.json._
import scalaz.{Validation, ValidationNel}
import scalaz.syntax.apply._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.09.17 17:18
  * Description: Embed-only аттрибуты.
  */
object MQdAttrsEmbed {

  object Fields {
    val WIDTH_FN  = MSize2di.Fields.WIDTH_FN
    val HEIGHT_FN = MSize2di.Fields.HEIGHT_FN
  }


  /** Поддержка play-json. */
  implicit val MQD_ATTRS_EMBED_FORMAT: OFormat[MQdAttrsEmbed] = {
    val F = Fields
    (
      (__ \ F.WIDTH_FN).formatNullable[ISetUnset[Int]] and
      (__ \ F.HEIGHT_FN).formatNullable[ISetUnset[Int]]
    )(apply, unlift(unapply))
  }

  /** Поддержка UnivEq. */
  @inline implicit def univEq: UnivEq[MQdAttrsEmbed] = UnivEq.derive


  def validateForStore(attrsEmbed: MQdAttrsEmbed, isHeightNeeded: Boolean): ValidationNel[String, MQdAttrsEmbed] = {
    val errMsgF = ErrorConstants.emsgF("embed")
    def sideValidate(suOpt: Option[ISetUnset[Int]], fn: String, mustBeSome: Boolean) = {
      def __mkVld(suOpt: Option[ISetUnset[Int]]) = {
        ISetUnset.validateSetOpt( suOpt,  errMsgF(fn) ) { value =>
          // TODO Вписать нормальные цифры, например исходя из размера внешнего контейнера.
          // TODO Уменьшать цифры, если выходят за пределы контейнера. Пропорционально.
          MathConst.Counts.validateMinMax(value, min = 10, max = 1000, errMsgF(fn) )
        }
      }

      if (mustBeSome) {
        ScalazUtil.liftNelSome( suOpt, errMsgF(fn) )(Validation.success) andThen __mkVld
      } else {
        __mkVld( suOpt )
      }
    }
    val F = Fields
    (
      // Ширина должна быть задана всегда:
      sideValidate(attrsEmbed.width,  F.WIDTH_FN, true) |@|
      // Высота нужна только для фреймов, поэтому учитываем мнение внешней функции.
      sideValidate(attrsEmbed.height, F.HEIGHT_FN, isHeightNeeded)
    )(apply)
  }

  def width = GenLens[MQdAttrsEmbed](_.width)
  def height = GenLens[MQdAttrsEmbed](_.height)

}


/** Класс модели аттрибутов embed'а.
  * Абсолютно все - необязательные.
  *
  * @param width Ширина.
  * @param height Высота.
  */
case class MQdAttrsEmbed(
                          width     : Option[ISetUnset[Int]]    = None,
                          height    : Option[ISetUnset[Int]]    = None
                        )
  extends EmptyProduct
  // Для ScalaCSS-рендера: Максимальная скорость работы `==` и hashCode()
  with IHashCodeLazyVal
  with IEqualsEq
{

  def withWidthHeight(width: Option[ISetUnset[Int]], height: Option[ISetUnset[Int]]) = copy(width = width, height = height)

  def size2dOpt: Option[MSize2di] = {
    for {
      widthSU   <- width
      w         <- widthSU.toOption
      heightSU  <- height
      h         <- heightSU.toOption
    } yield {
      MSize2di( width = w, height = h )
    }
  }

  override def toString: String = {
    StringUtil.toStringHelper(this, 16) { renderF =>
      val F = MQdAttrsEmbed.Fields
      width foreach renderF( F.WIDTH_FN )
      height foreach renderF( F.HEIGHT_FN )
    }
  }

}
