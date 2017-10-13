package io.suggest.jd.tags

import io.suggest.ad.blk.BlockMeta
import io.suggest.color.MColorData
import io.suggest.common.empty.{EmptyProduct, EmptyUtil, IEmpty}
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.img.MImgEdgeWithOps
import io.suggest.jd.tags.qd.MQdOp
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.09.17 13:03
  * Description: Кросс-платформенная унифицированна яJSON-модель пропертисов унифицированных jd-тегов.
  * 1 - потому что в будущем пропертисов будет больше, и наверное моделей тоже будет больше.
  *
  * Неявно-пустая модель.
  */
object MJdtProps1 extends IEmpty {

  override type T = MJdtProps1

  override def empty = MJdtProps1()

  /** Поддержка play-json. */
  implicit val MJD_TAG_PROPS1_FORMAT: OFormat[MJdtProps1] = (
    (__ \ "a").formatNullable[MColorData] and
    (__ \ "b").formatNullable[MImgEdgeWithOps] and
    (__ \ "c").formatNullable[BlockMeta] and
    (__ \ "d").formatNullable[MCoords2di] and
    (__ \ "e").formatNullable[Seq[MQdOp]]
      .inmap [Seq[MQdOp]] (
        EmptyUtil.opt2ImplEmpty1F( Nil ),
        {ops => if (ops.isEmpty) None else Some(ops) }
      )
  )(apply, unlift(unapply))

  implicit def univEq: UnivEq[MJdtProps1] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

}


/** Класс модели-контейнера пропертисов унифицированных jd-тегов.
  *
  * @param bgColor Цвет фона элемента.
  * @param bgImg Фоновая картинка элемента.
  * @param topLeft css-px-координаты левого верхнего угла элемента для абсолютного позиционирования.
  * @param bm BlockMeta для блоков (стрипов).
  * @param qdOps Список qd-операций для постройки контента (quill-delta).
  */
case class MJdtProps1(
                       bgColor  : Option[MColorData]        = None,
                       bgImg    : Option[MImgEdgeWithOps]   = None,
                       bm       : Option[BlockMeta]         = None,
                       topLeft  : Option[MCoords2di]        = None,
                       qdOps    : Seq[MQdOp]                = Nil,
                     )
  extends EmptyProduct
{

  def withBgColor(bgColor: Option[MColorData])        = copy(bgColor = bgColor)
  def withBgImg(bgImg: Option[MImgEdgeWithOps])       = copy(bgImg = bgImg)
  def withBm(bm: Option[BlockMeta])                   = copy(bm = bm)
  def withTopLeft(topLeft: Option[MCoords2di])        = copy(topLeft = topLeft)
  def withQdOps(qdOps: Seq[MQdOp])                    = copy(qdOps = qdOps)

}
