package io.suggest.jd.tags

import io.suggest.ad.blk.BlockMeta
import io.suggest.model.n2.node.meta.colors.MColorData
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
    (__ \ "bg").formatNullable[MColorData] and
    IDocTag.CHILDREN_IDOC_TAG_FORMAT
  )( rawApply, unlift(rawUnapply) )

  def rawApply(bm: Option[BlockMeta], bgColor: Option[MColorData], chs: Seq[IDocTag]): Strip = {
    apply(bm)(chs: _*)
  }

  def rawUnapply(s: Strip): Option[(Option[BlockMeta], Option[MColorData], Seq[IDocTag])] = {
    Some((s.bm, s.bgColor, s.children))
  }


}


/** Класс, описывающий одну полосу контента.
  *
  * @param bm BlockMeta с параметрами рендера полосы как блока плитки.
  *           Если None, значит strip не предназначен для рендера в плитке.
  * @param children Содержимое этой полосы.
  */
case class Strip(
                  bm        : Option[BlockMeta]   = None,
                  bgColor   : Option[MColorData]  = None
                )(
                  override val children: IDocTag*
                )
  extends IDocTag {

  override def dtName = MJdTagNames.STRIP

}
