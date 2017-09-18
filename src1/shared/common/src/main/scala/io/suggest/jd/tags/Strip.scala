package io.suggest.jd.tags

import io.suggest.ad.blk.BlockMeta
import io.suggest.model.n2.node.meta.colors.MColorData
import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.08.17 15:24
  * Description: Тэг одной "полосы" контента в документе.
  *
  * Интернет-сайты, особенно лендинги, приобрели свойство блочности и полосатости страниц
  * и размещения контента на них.
  */
object Strip {

  /** Поддержка play-json для полос контента. */
  implicit val STRIP_FORMAT: OFormat[Strip] = (
    (__ \ "bm").formatNullable[BlockMeta] and
    IBgColorOpt.bgColorOptFormat and
    IDocTag.CHILDREN_IDOC_TAG_FORMAT
  )( apply, unlift(unapply) )

  def a( bm        : Option[BlockMeta]   = None,
         bgColor   : Option[MColorData]  = None
       )( children: IDocTag* ): Strip = {
    apply(bm, bgColor, children)
  }

  implicit def univEq: UnivEq[Strip] = UnivEq.force

}


/** Класс, описывающий одну полосу контента.
  *
  * @param bm BlockMeta с параметрами рендера полосы как блока плитки.
  *           Если None, значит strip не предназначен для рендера в плитке.
  * @param children Содержимое этой полосы.
  */
case class Strip(
                  bm                      : Option[BlockMeta]   = None,
                  override val bgColor    : Option[MColorData]  = None,
                  override val children   : Seq[IDocTag]
                )
  extends IDocTag
  with IBgColorOpt
{

  override type T = Strip

  override def jdTagName = MJdTagNames.STRIP

  override def withChildren(children: Seq[IDocTag]): Strip = {
    copy( children = children )
  }

  def withBlockMeta(bm: Option[BlockMeta]) = copy(bm = bm)
  def withBgColor(bgColor: Option[MColorData]) = copy(bgColor = bgColor)

}
