package io.suggest.adn.edit.m

import io.suggest.adn.edit.NodeEditConstants
import io.suggest.common.empty.EmptyUtil
import io.suggest.common.html.HtmlConstants.`.`
import io.suggest.err.ErrorConstants
import io.suggest.img.MImgEdgeWithOps
import io.suggest.jd.{MJdEdgeId, MJdEdgeVldInfo}
import io.suggest.model.n2.edge.{EdgeUid_t, MPredicate, MPredicates}
import io.suggest.scalaz.{ScalazUtil, StringValidationNel}
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scalaz.Validation
import scalaz.syntax.apply._
import scalaz.std.stream._
import scalaz.std.iterable._
import japgolly.univeq._

import scala.collection.TraversableOnce

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.04.18 21:55
  * Description: Модель описания картинок adn-узла.
  * Является view-частью для картинок.
  */
object MAdnResView {

  implicit def mAdnImgsFormat: OFormat[MAdnResView] = (
    (__ \ "l").formatNullable[MJdEdgeId] and
    (__ \ "w").formatNullable[MJdEdgeId] and
    (__ \ "g").formatNullable[Seq[MImgEdgeWithOps]]
      .inmap[Seq[MImgEdgeWithOps]](
        EmptyUtil.opt2ImplEmptyF(Nil),
        { galImgs => if (galImgs.isEmpty) None else Some(galImgs) }
      )
  )(apply, unlift(unapply))

  implicit def univEq: UnivEq[MAdnResView] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

  def logoF = { rv: MAdnResView => rv.logo }
  def wcFgF = { rv: MAdnResView => rv.wcFg }
  def galImgsF = { rv: MAdnResView => rv.galImgs }


  /** Валидация инстанса [[MAdnResView]] с помощью карты эджей. */
  def validate(rv: MAdnResView, edgesMap: Map[EdgeUid_t, MJdEdgeVldInfo]): StringValidationNel[MAdnResView] = {
    // Фунция валидации опционального jdId, на который ссылается этот view
    def __vldJdId(jdIdOpt: Option[MJdEdgeId], pred: MPredicate, name: => String) = {
      ScalazUtil.liftNelOpt(jdIdOpt) { jdId =>
        ScalazUtil.liftNelSome( edgesMap.get(jdId.edgeUid), name + `.` + ErrorConstants.Words.MISSING ) { vldEdge =>
          Validation.liftNel(vldEdge.jdEdge.predicate)(_ !=* pred, name + `.` + ErrorConstants.Words.INVALID)
            .map { _ => vldEdge }
        }.map { _ => jdId }
      }
    }

    (
      __vldJdId(rv.logo, MPredicates.Logo, "logo") |@|
      __vldJdId(rv.wcFg, MPredicates.WcFgImg, "wc") |@|
      ScalazUtil.validateAll(rv.galImgs) { galImg =>
        MImgEdgeWithOps.validate(galImg, edgesMap, Some(NodeEditConstants.Gallery.WH_PX))
          .map(Stream(_))
      }
    )(apply _)
  }

}


/** Контейнер view-части для jd-эджей.
  *
  * @param logo Картинка-логотип.
  * @param wcFg Картинка приветствия.
  * @param galImgs Галерея картинок.
  */
case class MAdnResView(
                        logo      : Option[MJdEdgeId],
                        wcFg      : Option[MJdEdgeId],
                        galImgs   : Seq[MImgEdgeWithOps]
                      ) {

  def withLogo(logo: Option[MJdEdgeId]) = copy(logo = logo)
  def withWcFg(wcFg: Option[MJdEdgeId]) = copy(wcFg = wcFg)
  def withGalImgs(galImgs: Seq[MImgEdgeWithOps]) = copy(galImgs = galImgs)


  def edgeUids: Iterator[MJdEdgeId] = {
    productIterator
      .flatMap {
        case opt: Option[_] => opt.iterator
        case tr: TraversableOnce[_] => tr
        case x => x :: Nil
      }
      .flatMap {
        case id: MJdEdgeId => id :: Nil
        case imgId: MImgEdgeWithOps => imgId.imgEdge :: Nil
        case _ => Nil
      }
  }

}
