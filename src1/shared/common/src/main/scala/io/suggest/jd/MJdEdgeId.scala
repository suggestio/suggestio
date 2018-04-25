package io.suggest.jd

import io.suggest.common.geom.d2.ISize2di
import io.suggest.err.ErrorConstants
import io.suggest.img.crop.MCrop
import io.suggest.img.{MImgFmt, MImgFmts}
import io.suggest.model.n2.edge.EdgeUid_t
import io.suggest.scalaz.ScalazUtil
import io.suggest.scalaz.ScalazUtil.Implicits._
import japgolly.univeq.UnivEq
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

  /** Поддержка play-json. */
  implicit val mJdEdgePtrFormat: OFormat[MJdEdgeId] = {
    val oldJdEdgeIdReads = (__ \ "i").read[EdgeUid_t]

    val jdEdgeFmt0: OFormat[MJdEdgeId] = (
      (__ \ "e").format {
        // TODO 2018-04-20 Раньше было {i: 234}, а теперь просто 234. Удалить лишнее после resaveAll().
        val f0 = implicitly[Format[EdgeUid_t]]
        val r2 = f0.orElse( oldJdEdgeIdReads )
        Format(r2, f0)
      } and
      (__ \ "f").formatNullableWithDefault[MImgFmt] {
        println("Failed to decode img.output format. Did you executed AdnJdEdgesMigration.doIt()*?")
        Some( MImgFmts.default )
      } and
      (__ \ "c").formatNullable[MCrop]
    )(apply, unlift(unapply))

    // TODO 2018-04-20 Это совместимость со старой одноимённой моделью. Выставление outImgFmt произойдёт при портировании через AdnJdEdgesMigration.
    val jdEdgeReads2 = jdEdgeFmt0.orElse(
      for (oldJdEdgeUid <- oldJdEdgeIdReads) yield {
        apply(
          edgeUid       = oldJdEdgeUid
        )
      }
    )

    OFormat(jdEdgeReads2, jdEdgeFmt0)
  }

  implicit def univEq: UnivEq[MJdEdgeId] = UnivEq.derive


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
    val cropAndWhVldOpt = for {
      mcrop     <- m.crop
      contSz    <- imgContSzOpt
      edgeInfo  <- edgeInfoOpt
      img       <- edgeInfo.img
      imgWh     <- img.imgWh
    } yield {
      MCrop.validate(
        crop      = mcrop,
        tgContSz  = contSz,
        imgWh     = imgWh
      )
    }

    val outImgFmtVldOpt = for {
      edgeInfo <- edgeInfoOpt
      img <- edgeInfo.img
      if img.isImg
    } yield {
      Validation.liftNel(m.outImgFormat)(_.isEmpty, "img.fmt." + ErrorConstants.Words.MISSING)
    }

    (
      Validation.liftNel(m.edgeUid)(
        { _ => !edgeInfoOpt.exists(_.img.exists(_.isImg)) },
        ErrorConstants.emsgF("img")("e")
      ) |@|
      outImgFmtVldOpt.getOrElseNone |@|
      ScalazUtil.optValidationOrNone( cropAndWhVldOpt )
    )(apply _)
  }

}


/** Класс модели данных по картинке и её модификациям.
  *
  * @param edgeUid Данные доступа к эджу изображения.
  * @param dynFormat Выходной формат рендера картинки.
  *                  Скопипасчен как обязательный, хотя может быть лучше его опциональным сделать? Вопрос...
  * @param crop Кроп текущего изображения, если есть.
  */
case class MJdEdgeId(
                       edgeUid       : EdgeUid_t,
                       outImgFormat  : Option[MImgFmt]   = None,
                       crop          : Option[MCrop]     = None,
                     ) {

  def withImgEdge(imgEdge: EdgeUid_t) = copy(edgeUid = imgEdge)
  def withOutImgFormat(outImgFormat: Option[MImgFmt])  = copy(outImgFormat = outImgFormat)
  def withCrop(crop: Option[MCrop])   = copy(crop = crop)

}
