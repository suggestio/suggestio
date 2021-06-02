package io.suggest.adn.edit.m

import io.suggest.adn.edit.NodeEditConstants
import io.suggest.common.empty.{EmptyProduct, EmptyUtil, IEmpty}
import io.suggest.common.html.HtmlConstants.`.`
import io.suggest.err.ErrorConstants
import io.suggest.jd.{MJdEdgeId, MJdEdgeVldInfo}
import io.suggest.n2.edge.{EdgeUid_t, MPredicates}
import io.suggest.scalaz.{ScalazUtil, StringValidationNel}
import io.suggest.xplay.json.PlayJsonUtil
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scalaz.Validation
import scalaz.syntax.apply._
import scalaz.std.list._
import scalaz.std.iterable._
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.04.18 21:55
  * Description: Кросс-платформенная модель описания картинок adn-узла.
  * Является view-частью для картинок, вынесена из эджей.
  */
object MAdnResView extends IEmpty {

  override type T = MAdnResView
  override def empty = apply()

  object Fields {
    val LOGO_FN = "logo"
    val WELCOME_FG_FN = "welcomeForeground"
    val GALLERY_IMGS_FN = "gallery"
  }

  implicit def mAdnImgsFormat: OFormat[MAdnResView] = {
    val F = Fields
    (
      PlayJsonUtil.fallbackPathFormatNullable[MJdEdgeId]( F.LOGO_FN, "l" ) and
      PlayJsonUtil.fallbackPathFormatNullable[MJdEdgeId]( F.WELCOME_FG_FN, "w" ) and
      PlayJsonUtil.fallbackPathFormatNullable[Seq[MJdEdgeId]]( F.GALLERY_IMGS_FN, "g" )
        .inmap[Seq[MJdEdgeId]](
          EmptyUtil.opt2ImplEmptyF(Nil),
          galImgs => Option.when(galImgs.nonEmpty)(galImgs)
        )
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MAdnResView] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }


  /** Валидация инстанса [[MAdnResView]] с помощью карты эджей. */
  def validate(rv: MAdnResView, edgesMap: Map[EdgeUid_t, MJdEdgeVldInfo]): StringValidationNel[MAdnResView] = {
    // Фунция валидации опционального jdId, на который ссылается этот view
    def __vldJdId(jdIdOpt: Option[MJdEdgeId], name: String) = {
      ScalazUtil.liftNelOpt(jdIdOpt) { jdId =>
        (
          MJdEdgeId.validateImgId(jdId, edgesMap, None) |@|
          ScalazUtil.liftNelSome( edgesMap.get(jdId.edgeUid), name + `.` + ErrorConstants.Words.MISSING ) { vldEdge =>
            Validation.liftNel(vldEdge.jdEdge.predicate)(_ !=* MPredicates.JdContent.Image, name + `.` + ErrorConstants.Words.INVALID)
              .map { _ => vldEdge }
          }
        ){ (jdId2, _) => jdId2 }
      }
    }

    (
      __vldJdId(rv.logo, "logo") |@|
      __vldJdId(rv.wcFg, "wc") |@|
      ScalazUtil.validateAll(rv.galImgs) { galImg =>
        MJdEdgeId.validateImgId(galImg, edgesMap, Some(NodeEditConstants.Gallery.WH_PX))
          .map(_ :: Nil)
      }
    )(apply _)
  }

  def logo = GenLens[MAdnResView](_.logo)
  def wcFg = GenLens[MAdnResView](_.wcFg)
  def galImgs = GenLens[MAdnResView](_.galImgs)

}


/** Контейнер view-части для jd-эджей.
  *
  * @param logo Картинка-логотип.
  * @param wcFg Картинка приветствия.
  * @param galImgs Галерея картинок.
  */
case class MAdnResView(
                        logo      : Option[MJdEdgeId]     = None,
                        wcFg      : Option[MJdEdgeId]     = None,
                        galImgs   : Seq[MJdEdgeId]        = Nil,
                        // При добавлении новых полей с эджами: НЕ ЗАБЫВАТЬ про edgeUids ниже.
                      )
  extends EmptyProduct
{

  def edgeUids: LazyList[MJdEdgeId] = {
    (logo.iterator #::
     wcFg.iterator #::
     galImgs.iterator #::
     LazyList.empty)
      .flatten
  }

}
