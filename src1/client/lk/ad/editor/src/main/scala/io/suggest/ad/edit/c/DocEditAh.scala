package io.suggest.ad.edit.c

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.ad.blk.{BlockHeights, BlockWidths}
import io.suggest.ad.edit.m.edit.strip.MStripEdS
import io.suggest.ad.edit.m.{BlockSizeBtnClick, MDocS, StripDelete, StripDeleteCancel}
import io.suggest.common.MHands
import io.suggest.jd.render.m.{IJdTagClick, MJdCssArgs}
import io.suggest.jd.render.v.JdCssFactory
import io.suggest.jd.tags.qd.QdTag
import io.suggest.jd.tags.{JsonDocument, Strip}
import io.suggest.quill.m.TextChanged
import io.suggest.quill.u.QuillDeltaJsUtil
import io.suggest.sjs.common.log.Log

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.08.17 14:34
  * Description: Контроллер событий редактирования документа.
  */
class DocEditAh[M](
                    modelRW           : ModelRW[M, MDocS],
                    jdCssFactory      : JdCssFactory,
                    quillDeltaJsUtil  : QuillDeltaJsUtil
                  )
  extends ActionHandler( modelRW )
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Набор текста в wysiwyg-редакторе.
    case m: TextChanged =>
      val v0 = value

      if (v0.qDelta contains m.fullDelta) {
        // Бывают ложные срабатывания. Например, прямо при инициализации редактора. Но не факт конечно, что они тут подавляются.
        noChange

      } else {
        // Текст действительно изменился. Пересобрать json-document.
        val currTag0 = v0.jdArgs.selectedTag.get
        val (qdTag2, edges2) = quillDeltaJsUtil.delta2qdTag(m.fullDelta, currTag0, v0.jdArgs.renderArgs.edges)

        // Собрать новый json-document
        val jsonDoc2 = v0.jdArgs
          .template
          .deepUpdateChild( currTag0, qdTag2 :: Nil )
          .head
          .asInstanceOf[JsonDocument]

        val jdArgs2 = v0.jdArgs.copy(
          template    = jsonDoc2,
          renderArgs  = v0.jdArgs.renderArgs.withEdges(edges2),
          selectedTag = Some(qdTag2),
          jdCss       = jdCssFactory.mkJdCss(
            MJdCssArgs.singleCssArgs(jsonDoc2, v0.jdArgs.conf)
          )
        )

        // Залить все данные в новое состояние.
        val v2 = v0.withJdArgs(
          jdArgs = jdArgs2
          //qDelta = Some(m.fullDelta)    // Не обновляем дельту при редактировании, т.к. у нас тут только initial-значения
        )

        updated(v2)
      }


    // Клик по элементу карточки.
    case m: IJdTagClick =>
      val v0 = value
      if (v0.jdArgs.selectedTag contains m.jdTag) {
        // Бывают повторные щелчки по уже выбранным элементам, это нормально.
        noChange

      } else {
        // Юзер выбрал какой-то новый элемент. Залить новый тег в seleted:
        val v1 = v0.withJdArgs(
          v0.jdArgs.withSelectedTag( Some(m.jdTag) )
        )

        // Если это QdTag, то отработать состояние quill-delta:
        val v2 = m.jdTag match {
          // Это qd-тег, значит нужно собрать и залить текущую дельту текста в состояние.
          case qdt: QdTag =>
            val delta2 = quillDeltaJsUtil.qdTag2delta(qdt, v0.jdArgs.renderArgs.edges)
            v1.withQDelta( Some(delta2) )
          // Очистить состояние от дельты.
          case _ =>
            v1.withOutQDelta
        }

        // Если это strip, то активировать состояние strip-редактора.
        val v3 = m.jdTag match {
          // Переключение на новый стрип. Инициализировать состояние stripEd:
          case s: Strip =>
            v2.withStripEd(
              Some(MStripEdS(
                isLastStrip = {
                  // Оптимизация: НЕ проходим весь strip-итератор, а только считаем первые два стрипа.
                  val hasManyStrips = v0.jdArgs.template
                    .deepOfTypeIter[Strip]
                    .slice(0, 2)
                    .size > 1
                  !hasManyStrips
                }
              ))
            )
          // Это не strip, обнулить состояние stripEd, если оно существует:
          case _ =>
            v2.withOutStripEd
        }

        updated( v3 )
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

      val template2 = v0.jdArgs
        .template
        .deepUpdateOne( strip0, strip2 :: Nil )
        .head
        .asInstanceOf[JsonDocument]

      // Обновить и дерево, и currentTag новым инстансом.
      val v2 = v0.withJdArgs(
        jdArgs = v0.jdArgs.copy(
          selectedTag = Some( strip2 ),
          template    = template2,
          jdCss       = jdCssFactory.mkJdCss( MJdCssArgs.singleCssArgs(template2, v0.jdArgs.conf) )
        )
      )

      updated( v2 )


    // Клик по кнопке удаления или подтверждения удаления текущего стрипа
    case m: StripDelete =>
      val v0 = value
      val stripEdS0 = v0.stripEd.get

      if (!m.confirmed) {
        // Юзер нажал на обычную кнопку удаления стрипа.
        if (stripEdS0.confirmingDelete) {
          // Кнопка первого шага удаления уже была нажата: игнорим дубликат неактуального события
          noChange
        } else {
          val v2 = v0.withStripEd(
            Some( stripEdS0.withConfirmDelete( true ) )
          )
          updated(v2)
        }

      } else {
        // Второй шаг удаления, и юзер подтвердил удаление.
        val strip4del = v0.jdArgs.selectedTag.get.asInstanceOf[Strip]

        val tpl2 = v0.jdArgs
          .template
          .deepUpdateOne(strip4del, Nil)
          .head
          .asInstanceOf[JsonDocument]

        if (tpl2.children.nonEmpty) {

          val jdCss2 = jdCssFactory.mkJdCss(
            MJdCssArgs.singleCssArgs(tpl2, v0.jdArgs.conf)
          )

          val v2 = v0.copy(
            jdArgs = v0.jdArgs.copy(
              template    = tpl2,
              jdCss       = jdCss2,
              selectedTag = None
            ),
            qDelta  = None,   // Вроде бы это сюда не относится, ну да и ладно: сбросим заодно и текстовый редактор.
            stripEd = None
          )

          updated( v2 )

        } else {
          // Нельзя удалять из карточки последний оставшийся блок.
          noChange
        }
      }


    // Юзер передумал удалять текущий блок.
    case StripDeleteCancel =>
      val v0 = value
      val stripEdS0 = v0.stripEd.get
      if (stripEdS0.confirmingDelete) {
        // Юзер отменяет удаление
        val v2 = v0.withStripEd(
          Some( stripEdS0.withConfirmDelete(false) )
        )
        updated(v2)
      } else {
        // Какой-то левый экшен пришёл. Возможно, просто дублирующийся.
        noChange
      }

  }


}
