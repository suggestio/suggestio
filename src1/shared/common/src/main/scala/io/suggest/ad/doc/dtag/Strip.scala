package io.suggest.ad.doc.dtag

import io.suggest.ad.blk.BlockMeta
import play.api.libs.json._
import play.api.libs.functional.syntax._

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
    (__ \ "bm").format[BlockMeta] and
    IDocTag.CHILDREN_IDOC_TAG_FORMAT
  )( rawApply, unlift(rawUnapply) )

  def rawApply(bm: BlockMeta, chs: Seq[IDocTag]): Strip = {
    apply(bm)(chs: _*)
  }

  def rawUnapply(s: Strip): Option[(BlockMeta, Seq[IDocTag])] = {
    Some((s.bm, s.children))
  }


}


/** Класс, описывающий одну полосу контента.
  *
  * @param bm BlockMeta с параметрами рендера одной полоски.
  * @param children Содержимое этой полосы.
  */
case class Strip(
                  bm: BlockMeta
                )(
                  override val children: IDocTag*
                )
  extends IDocTag {

  override def dtName = MDtNames.Strip

}
