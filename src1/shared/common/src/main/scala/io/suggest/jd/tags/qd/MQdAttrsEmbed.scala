package io.suggest.jd.tags.qd

import io.suggest.common.empty.EmptyProduct
import io.suggest.common.geom.d2.MSize2di
import io.suggest.err.ErrorConstants
import io.suggest.math.MathConst
import io.suggest.media.MediaConst
import io.suggest.primo.{IEqualsEq, IHashCodeLazyVal, ISetUnset}
import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scalaz.ValidationNel
import scalaz.syntax.apply._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.09.17 17:18
  * Description: Embed-only аттрибуты.
  */
object MQdAttrsEmbed {

  object Fields {
    val WIDTH_FN  = MediaConst.NamesShort.WIDTH_FN
    val HEIGHT_FN = MediaConst.NamesShort.HEIGHT_FN
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
  implicit def univEq: UnivEq[MQdAttrsEmbed] = UnivEq.derive


  def validateForStore(attrsEmbed: MQdAttrsEmbed): ValidationNel[String, MQdAttrsEmbed] = {
    val errMsgF = ErrorConstants.emsgF("embed")
    def sideValidate(suOpt: Option[ISetUnset[Int]], fn: String) = {
      ISetUnset.validateSetOpt( suOpt,  errMsgF(fn) ) { value =>
        // TODO Вписать нормальные цифры, например исходя из размера внешнего контейнера.
        // TODO Уменьшать цифры, если выходят за пределы контейнера. Пропорционально.
        MathConst.Counts.validateMinMax(value, min = 10, max = 1000, errMsgF(fn) )
      }
    }
    val F = Fields
    (
      sideValidate(attrsEmbed.width,  F.WIDTH_FN) |@|
      sideValidate(attrsEmbed.height, F.HEIGHT_FN)
    )(apply)
  }

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

}
