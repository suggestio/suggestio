package io.suggest.ad.edit.c

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.ad.blk.{BlockHeights, BlockMeta, BlockWidths}
import io.suggest.ad.edit.m.edit.strip.MStripEdS
import io.suggest.ad.edit.m._
import io.suggest.ad.edit.m.edit.MAddS
import io.suggest.common.MHands
import io.suggest.common.coll.Lists
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.i18n.MsgCodes
import io.suggest.jd.MJdEditEdge
import io.suggest.jd.render.m._
import io.suggest.jd.render.v.JdCssFactory
import io.suggest.jd.tags.qd.{MQdEdgeInfo, MQdOp, MQdOpTypes, QdTag}
import io.suggest.jd.tags._
import io.suggest.model.n2.edge.{EdgeUid_t, MPredicates}
import io.suggest.model.n2.node.meta.colors.MColorData
import io.suggest.quill.m.TextChanged
import io.suggest.quill.u.QuillDeltaJsUtil
import io.suggest.sjs.common.i18n.Messages
import io.suggest.sjs.common.log.Log
import japgolly.univeq._

import scala.util.Random

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

  override protected val handle: PartialFunction[Any, ActionResult[M]] = {

    // Набор текста в wysiwyg-редакторе.
    case m: TextChanged =>
      val v0 = value

      if (v0.qDelta contains m.fullDelta) {
        // Бывают ложные срабатывания. Например, прямо при инициализации редактора. Но не факт конечно, что они тут подавляются.
        noChange

      } else {
        // Текст действительно изменился. Пересобрать json-document.
        //println( JSON.stringify(m.fullDelta) )
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
          case _: Strip =>
            v2.withStripEd(
              Some(MStripEdS(
                isLastStrip = {
                  val hasManyStrips = v0.jdArgs.template
                    .deepOfTypeIter[Strip]
                    // Оптимизация: НЕ проходим весь strip-итератор, а считаем только первые два стрипа.
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

        // Может быть, был какой-то qd-tag и весь текст теперь в нём удалён? Удалить, если старый тег, если осталась дельта
        val v4 = v0.jdArgs.selectedTag.fold(v3) {
          case qd: QdTag if qd.isEmpty(v0.jdArgs.renderArgs.edges) && v3.jdArgs.template.contains(qd) =>
            val tpl2 = v3.jdArgs.template.deepUpdateOne(qd, Nil)
              .head
              // Нужно shrink'ать, потому что иначе могут быть пустые AbsPos() теги.
              .shrink
              .head
              .asInstanceOf[JsonDocument]
            // Очистить эджи от лишнего контента
            val edgesMap2 = quillDeltaJsUtil.purgeUnusedEdges(tpl2, v3.jdArgs.renderArgs.edges)
            v3.withJdArgs(
              v3.jdArgs.copy(
                template    = tpl2,
                renderArgs  = v3.jdArgs.renderArgs.copy(
                  edges = edgesMap2
                ),
                jdCss       = jdCssFactory.mkJdCss(
                  MJdCssArgs.singleCssArgs(tpl2, v3.jdArgs.conf)
                )
              )
            )

          case _ => v3
        }

        updated( v4 )
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
        val strip4del = v0.jdArgs
          .selectedTag
          .get
          .asInstanceOf[Strip]

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


    // Началось перетаскивание какого-то jd-тега из текущего документа.
    case m: JdTagDragStart =>
      val v0 = value
      if (v0.jdArgs.dnd.jdt contains m.jdTag) {
        noChange
      } else {
        val v2 = v0.withJdArgs(
          v0.jdArgs.withDnd(
            v0.jdArgs.dnd.withJdt(
              jdt = Some( m.jdTag )
            )
          )
        )
        updated( v2 )
      }


    case m: JdTagDragEnd =>
      val v0 = value
      if (v0.jdArgs.dnd.jdt contains m.jdTag) {
        val v2 = v0.withJdArgs(
          v0.jdArgs.withDnd(
            v0.jdArgs.dnd.withJdt(
              jdt = None
            )
          )
        )
        updated( v2 )
      } else {
        noChange
      }


    // Юзер отпустил перетаскиваемый объект на какой-то стрип. Нужно запихать этот объект в дерево текущего стрипа.
    case m: JdDropContent =>
      val v0 = value
      val tpl0 = v0.jdArgs.template

      // Удалить текущий тег из старого места, если возможно. Готовим через batch, чтобы избежать двойной пересборки дерева.
      val delOldJdBatchOpt = for (currTag <- v0.jdArgs.dnd.jdt) yield {
        IDocTag.Batches.delete(currTag)
      }

      // Получить на руки инстанс сброшенного тега
      val dndJdt = m.foreignTag
        .orElse( v0.jdArgs.dnd.jdt )
        .get

      val clXy0 = m.clXy
      // TODO Если strip изменился, то надо изменить Y на высоту пройденных стрипов от исходного стрипа (включительно) до конечного стрипа (исключительно).
      // Найти исходный strip в исходном шаблоне
      val fromStripOpt = tpl0
        .deepOfTypeIter[Strip]
        .find { s =>
          s.children contains dndJdt
        }

      // Смотрим, изменился ли strip...
      val clXy2 = fromStripOpt.fold(clXy0) { fromStrip =>
        if (fromStrip == m.strip) {
          // Перемещение в рамках одного стрипа
          clXy0
        } else {
          // Перемещение между разными strip'ами. Надо пофиксить координату Y, иначе добавляемый элемент отрендерится где-то за экраном.
          val strips = tpl0
            .deepOfTypeIter[Strip]
            .toSeq
          // TODO Отработать ситуацию, когда хотя бы один из index'ов == -1
          val fromStripIndex = strips.indexOf(fromStrip)
          val toStripIndex = strips.indexOf(m.strip)
          val (topStrip, bottomStrip, yModSign) = if (fromStripIndex <= toStripIndex) {
            (fromStrip, m.strip, -1)
          } else {
            (m.strip, fromStrip, +1)
          }
          // Собрать все стрипы от [текущего до целевого), просуммировать высоту блоков, вычесть из Y
          val iter = tpl0
            .deepOfTypeIter[Strip]
            .dropWhile(_ !=* topStrip)
            .takeWhile(_ !=* bottomStrip)
            .flatMap(_.bm)
          if (iter.nonEmpty) {
            val yDiff = iter
              .map { _.h.value }
              .sum
            val y2 = clXy0.y + yModSign * yDiff
            //println(s"mod Y: ${clXy0.y} by $yDiff => $y2")
            clXy0.withY( y2 )
          } else {
            // Странно: нет пройденных стрипов, хотя они должны бы быть
            LOG.warn( msg = s"$clXy0 [$fromStrip => ${m.strip})" )
            clXy0
          }
        }
      }
      // Рассчитать пиксельную высоту-разницу от исходного fromStrip до текущего m.strip


      // Выставить новые координаты тегу
      val apJdt2 = dndJdt match {
        case ap: AbsPos =>
          ap.copy(
            topLeft = clXy2
          )
        case _ =>
          AbsPos.a(clXy2)(dndJdt)
      }

      // Добавить перетащенный тег в текущий стрип.
      val batches0 = List[JdBatch_t](
        m.strip -> { strip1s: Seq[IDocTag] =>
          val childrenTail = Seq(apJdt2)
          for (s <- strip1s) yield {
            s.withChildren( s.children ++ childrenTail )
          }
        }
      )

      val batches2 = Lists.prependOpt(delOldJdBatchOpt)( batches0 )

      val tpl2 = IDocTag.batchUpdateOne2(tpl0, batches2)
        .head
        .asInstanceOf[JsonDocument]

      // Пересобрать данные для рендера.
      val v2 = v0.withJdArgs(
        v0.jdArgs.copy(
          template    = tpl2,
          jdCss       = jdCssFactory.mkJdCss( MJdCssArgs.singleCssArgs(tpl2, v0.jdArgs.conf) ),
          dnd         = MJdDndS.empty,
          // Один или два strip'а будет перестроено, поэтому обнуляем selectedTag только если там strip.
          // TODO Нужно выставлять сюда инстанс нового/старого стрипа, если там был новый или старый стрип.
          selectedTag = v0.jdArgs.selectedTag
            .filterNot(_.isInstanceOf[Strip])
        )
      )

      updated(v2)

    // Реакцие на завершение перетаскивания целого стрипа.
    case m: JdDropStrip =>
      val v0 = value
      val droppedStrip = v0.jdArgs.dnd.jdt.get
      val targetStrip = m.targetStrip
      if (droppedStrip == targetStrip) {
        noChange
      } else {
        val strips2 = v0.jdArgs.template
          .deepOfTypeIter[Strip]
          .filter(_ != droppedStrip)
          .flatMap { s =>
            if (s == targetStrip) {
              if (m.isUpper) {
                // Поместить dropped strip сверху
                droppedStrip :: s :: Nil
              } else {
                // Поместить перемещаемый стрип снизу
                s :: droppedStrip :: Nil
              }
            } else {
              // Это не тот стрип, на который шло перемещение.
              s :: Nil
            }
          }
          .toList
        // Залить обновлённый список стрипов в исходный документ
        val tpl2 = v0.jdArgs.template
          .withChildren(strips2)
        val v2 = v0.withJdArgs(
          v0.jdArgs.withTemplate(tpl2)
        )
        updated(v2)
      }


    // Реакция на клик по кнопке создания нового элемента.
    case AddBtnClick =>
      val v0 = value
      if (v0.addS.nonEmpty) {
        noChange
      } else {
        val v2 = v0.withAddS(
          Some( MAddS.default )
        )
        updated(v2)
      }

    case AddContentClick =>
      val v0 = value
      val intoStrip0 = v0.jdArgs.selectedTag.fold[Strip] {
        v0.jdArgs.template
          .deepOfTypeIter[Strip]
          .next()
      } {
        // Если выбран strip, то его и вернуть
        case s: Strip =>
          s
        // Если выбран какой-то не-strip элемент, то найти его strip
        case selJdt =>
          v0.jdArgs.template
            .deepOfTypeIter[Strip]
            .find { s =>
              s.contains(selJdt)
            }
            .get
      }

      val rnd = new Random()
      val bm0 = intoStrip0.bm.getOrElse( BlockMeta.DEFAULT )
      val coordsRnd = MCoords2di(
        x = rnd.nextInt( bm0.w.value/3 ) + 10,
        y = rnd.nextInt( (bm0.h.value * 0.75).toInt ) + (bm0.h.value * 0.12).toInt
      )

      val textL10ed = Messages( MsgCodes.`Example.text` )

      val textPred = MPredicates.JdContent.Text
      val edgesMap0 = v0.jdArgs.renderArgs.edges
      val (edgesMap2, edgeUid) = edgesMap0
        .valuesIterator
        .find { e =>
          e.predicate == textPred &&
            e.text.contains( textL10ed )
        }
        .fold [(Map[EdgeUid_t, MJdEditEdge], Int)] {
          // Нет примера текста в эджах: добавить его туда.
          val existEdgeUids = edgesMap0.keySet
          val nextEdgeUid = if (existEdgeUids.isEmpty)  0  else  existEdgeUids.max + 1
          val e = MJdEditEdge(
            predicate = textPred,
            id        = nextEdgeUid,
            text      = Some(textL10ed)
          )
          val edgesMap1 = edgesMap0 + (nextEdgeUid -> e)
          (edgesMap1, nextEdgeUid)
        } { exampleTextEdge =>
          (edgesMap0, exampleTextEdge.id)
        }

      val qdt = QdTag(Seq(
          MQdOp(
            opType = MQdOpTypes.Insert,
            edgeInfo = Some(MQdEdgeInfo(
              edgeUid = edgeUid
            ))
          )
        ))
      val contentJdt = AbsPos.a(coordsRnd)(qdt)

      val intoStrip2 = intoStrip0.withChildren {
        intoStrip0.children ++ Seq( contentJdt )
      }

      val tpl2 = v0.jdArgs.template
        .deepUpdateChild(intoStrip0, intoStrip2 :: Nil)
        .head
        .asInstanceOf[JsonDocument]

      val v2 = v0.copy(
        jdArgs = v0.jdArgs.copy(
          template    = tpl2,
          renderArgs  = v0.jdArgs.renderArgs
            .withEdges( edgesMap2 ),
          jdCss       = jdCssFactory.mkJdCss(
            MJdCssArgs.singleCssArgs(tpl2, v0.jdArgs.conf)
          ),
          selectedTag = Some(qdt)
        ),
        qDelta = Some {
          quillDeltaJsUtil.qdTag2delta(qdt, edgesMap2)
        },
        stripEd = None,
        addS = None
      )
      updated(v2)

    // Клик по кнопке добавления нового стрипа.
    case AddStripClick =>
      val v0 = value
      val beforeStripOpt = v0.jdArgs
        // Попробовать текущеий выделенный strip
        .selectedTag
        .flatMap {
          case s: Strip => Some(s)
          case _        => None
        }
        .orElse {
          // добавить в конец списка стрипов, если есть хотя бы один стрип.
          v0.jdArgs.template
            .deepOfTypeIter[Strip]
            .toStream
            .lastOption
        }

      val iter0 = v0.jdArgs.template
        .deepOfTypeIter[Strip]

      val newStrip = Strip.a(
        bm      = Some(BlockMeta.DEFAULT),
        bgColor = Some(MColorData("ffffff"))
      )()

      val iter2 = beforeStripOpt.fold {
        iter0 ++ Seq(newStrip)
      } { beforeStrip =>
        v0.jdArgs.template
          .deepOfTypeIter[Strip]
          .flatMap { s =>
            if (s == beforeStrip) {
              s :: newStrip :: Nil
            } else {
              s :: Nil
            }
          }
      }

      val tpl2 = v0.jdArgs.template.withChildren( iter2.toSeq )

      val v2 = v0
        .withAddS(None)
        .withJdArgs(
          v0.jdArgs.copy(
            template    = tpl2,
            selectedTag = Some(newStrip),
            jdCss       = jdCssFactory.mkJdCss( MJdCssArgs.singleCssArgs(tpl2, v0.jdArgs.conf) )
          )
        )
      updated(v2)

    // Отмена добавления чего-либо.
    case AddCancelClick =>
      val v0 = value
      if (v0.addS.isEmpty) {
        noChange
      } else {
        val v2 = v0.withAddS( None )
        updated( v2 )
      }

  }

}
