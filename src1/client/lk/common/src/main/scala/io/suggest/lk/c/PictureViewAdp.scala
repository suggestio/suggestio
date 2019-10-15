package io.suggest.lk.c

import io.suggest.common.html.HtmlConstants
import io.suggest.err.ErrorConstants
import io.suggest.form.MFormResourceKey
import io.suggest.jd.MJdEdgeId
import io.suggest.jd.tags.qd.MQdOp
import io.suggest.jd.tags.{JdTag, MJdTagNames, MJdtProps1}
import io.suggest.model.n2.edge.EdgeUid_t
import scalaz.Tree
import scalaz.std.option._
import japgolly.univeq._
import monocle.Traversal

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.18 17:32
  * Description: После выноса [[PictureAh]]-контроллера, возникла проблема со сложностью сохранения
  * данных по картинке уровня представления: карта эджей описывает только низкоуровневые связи,
  * но не высокоуровневые вещи: кроп, id'шник, наличие картинки вообще.
  *
  * type-class нужен как простая прослойка между абстрактной моделью представления (дерево jd-тегов, adn-узел, и т.д.)
  * и
  */
trait IPictureViewAdp[V] {

  /** Прочитать текущие данные по отображению изображения. */
  def get(view: V, resKey: MFormResourceKey): Option[MJdEdgeId]

  /** Обновить указатель на картинку, вернув обновлённый V. */
  def updated(view: V, resKey: MFormResourceKey)(newValue: Option[MJdEdgeId]): V

  def updateWith(view: V, resKey: MFormResourceKey)(f: Option[MJdEdgeId] => Option[MJdEdgeId]): V = {
    updated(view, resKey) {
      f( get(view, resKey) )
    }
  }

  /** Стереть все упоминония картинки с указанным id эджа из хранилища.
    * Пригодно для всяких экстренных случаев, например когда выяснилось, что картинка невалидна. */
  def forgetEdge(view: V, edgeUid: EdgeUid_t): V

}


object IPictureViewAdp {

  /** Самый твивиальный view-контейнер значения - это само значение. Тут typeclass поддержки такого контейнера. */
  implicit object DummyAdp extends IPictureViewAdp[Option[MJdEdgeId]] {
    override def get(view: Option[MJdEdgeId], resKey: MFormResourceKey): Option[MJdEdgeId] = {
      view
    }
    override def updated(view: Option[MJdEdgeId], resKey: MFormResourceKey)(newValue: Option[MJdEdgeId]): Option[MJdEdgeId] = {
      newValue
    }
    override def forgetEdge(view: Option[MJdEdgeId], edgeUid: EdgeUid_t): Option[MJdEdgeId] = {
      view
        .filterNot { p =>
          p.edgeUid ==* edgeUid
        }
    }
  }


  /** Поддержка управления представлением картинок, которые хранятся в дереве jd-тегов. */
  implicit object JdTagTreeBgImgAdp extends IPictureViewAdp[Tree[JdTag]] {

    import io.suggest.scalaz.ZTreeUtil.ZTreeOps

    private def _cmpEdgeUid(keyEdgeUid: Option[EdgeUid_t], edgeUids: Iterable[MJdEdgeId]): Boolean = {
      (keyEdgeUid.isEmpty && edgeUids.isEmpty) ||
        keyEdgeUid.exists(ei => edgeUids.exists(_.edgeUid ==* ei))
    }

    override def get(view: Tree[JdTag], resKey: MFormResourceKey): Option[MJdEdgeId] = {
      val nodePath = resKey.nodePath.get
      for {
        jdtLoc   <- view.pathToNode( nodePath )
        jdt = jdtLoc.getLabel
        // Само-контроль: убедится, что данные тега содержат или НЕ содержат эдж, указанный в resKey:
        if _cmpEdgeUid(resKey.edgeUid, jdt.edgeUids)
        // Извлечь или собрать инстанс MImgEdgeWithOps.
        imgEdgeWithOps <- jdt.props1
          .bgImg
          .orElse {
            for {
              qdProps <- jdt.qdProps
              ei <- qdProps.edgeInfo
            } yield {
              ei
            }
          }
      } yield {
        imgEdgeWithOps
      }
    }

    override def updated(view: Tree[JdTag], resKey: MFormResourceKey)(newValue: Option[MJdEdgeId]): Tree[JdTag] = {
      // Найти и обновить bgImg для тега, который подпадает под запрашиваемый ключ.
      val nodePath = resKey.nodePath.get
      val jdtLoc = view.pathToNode(nodePath).get
      val jdt = jdtLoc.getLabel
      ErrorConstants.assertArg( _cmpEdgeUid(resKey.edgeUid, jdt.edgeUids) )

      jdtLoc
        .setLabel {
          jdt.name match {
            case MJdTagNames.QD_OP =>
              JdTag.qdProps
                .composeTraversal( Traversal.fromTraverse[Option, MQdOp] )
                .composeLens( MQdOp.edgeInfo )
                .set( newValue )(jdt)
            case MJdTagNames.STRIP =>
              _jdtag_p1_bgImg_LENS
                .set( newValue )(jdt)
            case jdtName =>
              throw new UnsupportedOperationException(resKey + HtmlConstants.SPACE + newValue + HtmlConstants.SPACE + jdtName)
          }
        }
        .toTree
    }

    override def forgetEdge(view: Tree[JdTag], edgeUid: EdgeUid_t): Tree[JdTag] = {
      // Аккуратно отмаппить все теги: если bgImg содержит edgeUid, то обнулить bgImg.
      val lens = _jdtag_p1_bgImg_LENS
      for (jdt0 <- view) yield {
        if ( lens.get(jdt0).exists(_.edgeUid ==* edgeUid) ) {
          lens.set(None)(jdt0)
        } else {
          jdt0
        }
      }
    }

  }


  private def _jdtag_p1_bgImg_LENS = {
    JdTag.props1
      .composeLens( MJdtProps1.bgImg )
  }

}

