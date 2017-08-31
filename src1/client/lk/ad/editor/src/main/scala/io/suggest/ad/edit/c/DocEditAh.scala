package io.suggest.ad.edit.c

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.ad.blk.{BlockHeights, BlockWidths}
import io.suggest.ad.edit.m.{BlockSizeBtnClick, MDocS, TextChanged}
import io.suggest.ad.edit.u.QuillDeltaJsUtil
import io.suggest.common.MHands
import io.suggest.jd.render.m.{IJdTagClick, MJdArgs}
import io.suggest.jd.render.v.JdCssFactory
import io.suggest.jd.tags.{JsonDocument, Strip, Text}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.08.17 14:34
  * Description: Контроллер событий редактирования документа.
  */
class DocEditAh[M](
                    jdCssFactory  : JdCssFactory,
                    modelRW       : ModelRW[M, MDocS]
                  )
  extends ActionHandler( modelRW )
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    case m: TextChanged =>
      // TODO
      noChange

    // Клик по элементу карточки.
    case m: IJdTagClick =>
      val v0 = value
      if (v0.jdArgs.selectedTag contains m.jdTag) {
        // Бывают повторные щелчки по уже выбранным элементам, это нормально.
        noChange

      } else {
        // Юзер выбрал какой-то новый элемент.
        val v1 = v0.withJdArgs(
          v0.jdArgs.withSelectedTag( Some(m.jdTag) )
        )
        val v2 = m.jdTag match {
          case t: Text =>
            // Нужно собрать и залить текущую дельту текста в состояние.
            val delta2 = QuillDeltaJsUtil.text2delta(t, v0.jdArgs.renderArgs.edges)
            v1.withQDelta( Some(delta2) )
          case _ => v1
        }
        updated( v2 )
      }

    // Клик по кнопкам управления размером текущего блока
    case m: BlockSizeBtnClick =>
      val v0 = value
      val strip0 = v0.jdArgs.selectedTag.get.asInstanceOf[Strip]
      val bm0 = strip0.bm.get
      val bm2 = m.model match {
        case bhs @ BlockHeights =>
          val sz0 = bm0.h
          val szOpt2 = m.direction match {
            case MHands.Left  => bhs.previousOf( sz0 )
            case MHands.Right => bhs.nextOf( sz0 )
          }
          val sz2 = szOpt2.get
          bm0.withHeight( sz2 )

        case bws @ BlockWidths =>
          val sz0 = bm0.w
          val szOpt2 = m.direction match {
            case MHands.Left  => bws.previousOf( sz0 )
            case MHands.Right => bws.nextOf( sz0 )
          }
          val sz2 = szOpt2.get
          bm0.withWidth( sz2 )
      }

      val strip2 = strip0.withBlockMeta(
        Some( bm2 )
      )

      // Обновить и дерево, и currentTag новым инстансом.
      val v2 = v0.withJdArgs(
        jdArgs = {
          val jdArgs1 = v0.jdArgs
            .withSelectedTag( Some( strip2 ) )
            .withTemplate(
              v0.jdArgs
                .template
                .deepUpdateOne( strip0, strip2 :: Nil )
                .head
                .asInstanceOf[JsonDocument]
            )
          val jdCss = jdCssFactory.mkJdCss( MJdArgs.singleCssArgs(jdArgs1) )
          jdArgs1.withJdCss( jdCss )
        }
      )

      updated( v2 )

  }

}
