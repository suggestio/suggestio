package io.suggest.jd

import diode.FastEq
import io.suggest.ad.blk.MBlockExpandMode
import io.suggest.common.empty.{EmptyProduct, EmptyUtil, IEmpty}
import io.suggest.common.html.HtmlConstants
import io.suggest.jd.tags.JdTag
import io.suggest.primo.IHashCodeLazyVal
import io.suggest.scalaz.NodePath_t
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.collection.immutable.HashMap

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.03.18 22:06
  * Description: Модель стабильных id для каждого блока и тега плитки,
  * чтобы убрать автоматические полурандомные названия стилей в ScalaCSS,
  * которые неэффективны в react-плитке, вызывая пере-рендеры на любое изменение в css.
  *
  * Эта модель нужна для связывания css-стиля блока с html-вёрсткой через промежуточный идентификатор,
  * задаваемый этой моделью.
  *
  * Если какой-то блок повторяется в плитке, то проблемы в этом нет - id не является обязательно уникальным,
  * ибо тут связывающий id, ключ для доступа к стилю, и не более.
  *
  * Это нужно для оптимизации jd-рендера, чтобы убрать пере-рендеры из-за нарушения порядка стилей в css.
  */
object MJdTagId extends IEmpty {

  override type T = MJdTagId
  override def empty = apply()

  implicit def univEq: UnivEq[MJdTagId] = UnivEq.derive

  implicit def jdTagIdJson: OFormat[MJdTagId] = (
    (__ \ "i").formatNullable[String] and
    (__ \ "p").formatNullable[NodePath_t]
      .inmap[NodePath_t](
        EmptyUtil.opt2ImplEmptyF( Nil ),
        np => if (np.isEmpty) None else Some(np),
      ) and
    (__ \ "e").formatNullable[MBlockExpandMode]
  )(apply, unlift(unapply))


  val selPathRev = GenLens[MJdTagId](_.selPathRev)
  val blockExpand = GenLens[MJdTagId](_.blockExpand)

  /** Добавление под-уровня в [[MJdTagId]] происходит обычно через это действие: */
  def selPathRevNextLevel(subIndex: Int) =
    selPathRev.modify(subIndex :: _)


  def mkTreeIndex(segments: Stream[(MJdTagId, JdTag)]*): HashMap[MJdTagId, JdTag] =
    mkTreeIndex1( segments )

  /** Сборка кусков-сегментов в единый индекс. */
  def mkTreeIndex1(segments: TraversableOnce[Stream[(MJdTagId, JdTag)]]): HashMap[MJdTagId, JdTag] = {
    (HashMap.newBuilder[MJdTagId, JdTag] ++= segments.toStream.iterator.flatten)
      .result()
  }


  object MJdTagIdFastEq extends FastEq[MJdTagId] {
    override def eqv(a: MJdTagId, b: MJdTagId): Boolean = {
      // Список selPathRev собирается динамически на основе zipWithIndex, поэтому инстансы у него очень динамические, в основном - одноразовые.
      ((a.selPathRev ===* b.selPathRev) || (a.selPathRev ==* b.selPathRev)) &&
      // Инстансы nodeId и blockExpand берутся напрямую из стабильных JdTag, поэтому эти инстансы стабильны:
      (a.nodeId ===* b.nodeId) &&
      (a.blockExpand ===* b.blockExpand)
    }
  }

}


/** Класс-контейнер данных идентификатора блока.
  *
  * @param nodeId id узла-карточки, если есть.
  * @param selPathRev Порядковый номер до тега внутри jd-документа (одной карточки).
  *                   НеЭквивалентен по смыслу к MJdRenderArgs.selPath
  * @param blockExpand Режим отображения блока.
  */
final case class MJdTagId(
                           nodeId           : Option[String]              = None,
                           selPathRev       : NodePath_t                  = Nil,
                           blockExpand      : Option[MBlockExpandMode]    = None,
                         )
  extends EmptyProduct
  with IHashCodeLazyVal
{

  override lazy val toString: String = {
    var acc: List[Any] = selPathRev
    for (bexp <- blockExpand)
      acc ::= bexp.value
    for (id <- nodeId)
      acc ::= id
    acc.mkString( HtmlConstants.MINUS )
  }

}
