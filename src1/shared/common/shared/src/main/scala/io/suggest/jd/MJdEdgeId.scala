package io.suggest.jd

import io.suggest.common.geom.d2.ISize2di
import io.suggest.err.ErrorConstants
import io.suggest.img.crop.MCrop
import io.suggest.img.MImgFormat
import io.suggest.n2.edge.EdgeUid_t
import io.suggest.primo.id.IId
import io.suggest.scalaz.ScalazUtil
import io.suggest.text.StringUtil
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.functional.syntax._
import play.api.libs.json._
import scalaz.syntax.apply._
import scalaz.{Validation, ValidationNel}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.09.17 12:40
  * Description: Модель описания изображения с накладываемыми на него операциями.
  */
object MJdEdgeId {

  object Fields {
    final def EDGE_UID = "e"
    final def IMG_FORMAT = "f"
    final def CROP = "c"
  }

  /** Поддержка play-json. */
  implicit def mJdEdgeIdFormat: OFormat[MJdEdgeId] = {
    val F = Fields
    (
      (__ \ F.EDGE_UID).format[EdgeUid_t] and
      (__ \ F.IMG_FORMAT).formatNullable[MImgFormat] and
      (__ \ F.CROP).formatNullable[MCrop]
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MJdEdgeId] = UnivEq.derive


  /** Валидация инстанса [[MJdEdgeId]] с помощью остальных данных.
    *
    * @param m            Проверяемый инстанс [[MJdEdgeId]].
    * @param edges        Карта допустимых эджей.
    * @param imgContSzOpt Размер контейнера изображения (для проверки кропа).
    * @return Результат валидации.
    */
  def validate(
                m             : MJdEdgeId,
                edges         : Map[EdgeUid_t, MJdEdgeVldInfo],
                imgContSzOpt  : Option[ISize2di]
              ): ValidationNel[String, MJdEdgeId] = {
    val edgeInfoOpt = edges.get( m.edgeUid )

    Validation
      .liftNel( edgeInfoOpt.orNull )( _ == null, s"edge #${m.edgeUid} missing" )
      .andThen { edgeInfo =>
        val cropAndWhVldOpt = for {
          mcrop     <- m.crop
          contSz    <- imgContSzOpt
          img       <- edgeInfo.file
          imgWh     <- img.imgWh
        } yield {
          MCrop.validate(
            crop      = mcrop,
            tgContSz  = contSz,
            imgWh     = imgWh
          )
        }

        (
          Validation.liftNel(m.edgeUid)(
            { _ => !edgeInfo.file.exists(_.isImg) },
            ErrorConstants.emsgF("img")("e")
          ) |@|
          // Формат пока просто копипастим из VldInfo: Юзер не управляет заданием выходного формата.
          ScalazUtil.liftNelSome(
            edgeInfo.file.flatMap(_.dynFmt),
            s"edge#${m.edgeUid}.dynFmt missing"
          )( Validation.success ) |@|
          ScalazUtil.optValidationOrNone( cropAndWhVldOpt )
        )(apply _)
      }
  }

  def edgeUid       = GenLens[MJdEdgeId](_.edgeUid)
  def outImgFormat  = GenLens[MJdEdgeId](_.outImgFormat)
  def crop          = GenLens[MJdEdgeId](_.crop)

}


/** Класс модели данных по картинке и её модификациям.
  *
  * @param edgeUid Данные доступа к эджу изображения.
  * @param outImgFormat Выходной формат рендера картинки.
  *                     Скопипасчен как обязательный, хотя может быть лучше его опциональным сделать? Вопрос...
  * @param crop Кроп текущего изображения, если есть.
  */
final case class MJdEdgeId(
                            edgeUid       : EdgeUid_t,
                            outImgFormat  : Option[MImgFormat]   = None,
                            crop          : Option[MCrop]     = None,
                          )
  extends IId[EdgeUid_t]
{

  override def id = edgeUid

  override def toString: String = {
    StringUtil.toStringHelper(this, 32) { renderF =>
      val F = MJdEdgeId.Fields
      renderF( F.EDGE_UID )(edgeUid)
      outImgFormat foreach renderF( F.IMG_FORMAT )
      crop foreach renderF( F.CROP )
    }
  }

}
