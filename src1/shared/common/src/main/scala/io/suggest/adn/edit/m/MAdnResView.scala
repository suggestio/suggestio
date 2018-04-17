package io.suggest.adn.edit.m

import io.suggest.common.empty.EmptyUtil
import io.suggest.img.MImgEdgeWithOps
import io.suggest.jd.MJdEdgeId
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

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

}


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
