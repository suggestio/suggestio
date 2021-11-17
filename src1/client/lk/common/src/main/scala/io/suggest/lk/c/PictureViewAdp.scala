package io.suggest.lk.c

import io.suggest.common.html.HtmlConstants
import io.suggest.err.ErrorConstants
import io.suggest.form.MFormResourceKey
import io.suggest.jd.MJdEdgeId
import io.suggest.jd.tags.qd.MQdOp
import io.suggest.jd.tags.{JdTag, MJdProps1, MJdTagNames}
import io.suggest.scalaz.ZTreeUtil._
import io.suggest.n2.edge.EdgeUid_t
import io.suggest.scalaz.ScalazUtil.Implicits.EphStreamExt
import scalaz.{EphemeralStream, Tree}
import scalaz.std.option._
import japgolly.univeq._
import monocle.Traversal

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.18 17:32
  * Description: После выноса [[UploadAh]]-контроллера, возникла проблема со сложностью сохранения
  * данных по картинке уровня представления: карта эджей описывает только низкоуровневые связи,
  * но не высокоуровневые вещи: кроп, id'шник, наличие картинки вообще.
  *
  * type-class нужен как простая прослойка между абстрактной моделью представления (дерево jd-тегов, adn-узел, и т.д.)
  */
trait IJdEdgeIdViewAdp[V] {

  /** Прочитать текущие данные по отображению изображения. */
  def get(view: V, resKey: MFormResourceKey): Option[MJdEdgeId]

  /** Обновить указатель на картинку, вернув обновлённый V. */
  def updated(view: V, resKey: MFormResourceKey)(newValue: Option[MJdEdgeId]): V

  def updateWith(view: V, resKey: MFormResourceKey)(f: Option[MJdEdgeId] => Option[MJdEdgeId]): V = {
    val v2 = f( get(view, resKey) )
    updated(view, resKey)(v2)
  }

  /** Стереть все упоминания картинки с указанным id эджа из хранилища.
    * Пригодно для всяких экстренных случаев, например когда выяснилось, что картинка невалидна. */
  def forgetEdge(view: V, edgeUid: EdgeUid_t): V

}


object IJdEdgeIdViewAdp {

  /** Самый твивиальный view-контейнер значения - это само значение. Тут typeclass поддержки такого контейнера. */
  implicit object PlainAdp extends IJdEdgeIdViewAdp[Option[MJdEdgeId]] {
    override def get(view: Option[MJdEdgeId], resKey: MFormResourceKey): Option[MJdEdgeId] =
      view

    override def updated(view: Option[MJdEdgeId], resKey: MFormResourceKey)(newValue: Option[MJdEdgeId]): Option[MJdEdgeId] =
      newValue

    override def forgetEdge(view: Option[MJdEdgeId], edgeUid: EdgeUid_t): Option[MJdEdgeId] = {
      view
        .filterNot { p =>
          p.edgeUid ==* edgeUid
        }
    }
  }


  /** Поддержка управления представлением картинок, которые хранятся в дереве jd-тегов. */
  implicit object JdTagTreeBgImgAdp extends IJdEdgeIdViewAdp[Tree[JdTag]] {

    private def _cmpEdgeUid(keyEdgeUid: Option[EdgeUid_t], edgeUids: EphemeralStream[MJdEdgeId]): Boolean = {
      (keyEdgeUid.isEmpty && edgeUids.isEmpty) ||
      keyEdgeUid.exists { ei =>
        edgeUids.iterator.exists(_.edgeUid ==* ei)
      }
    }

    override def get(view: Tree[JdTag], resKey: MFormResourceKey): Option[MJdEdgeId] = {
      val nodePath = resKey.nodePath.get
      for {
        jdtLoc <- view
          .loc
          .pathToNode( nodePath )
        // Само-контроль: убедится, что данные тега содержат или НЕ содержат эдж, указанный в resKey:
        if _cmpEdgeUid(resKey.edgeUid, jdtLoc.allImgEdgeUids())
        jdt = jdtLoc.getLabel
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
      val jdtLoc = view
        .loc
        .pathToNode(nodePath)
        .get
      val jdt = jdtLoc.getLabel
      ErrorConstants.assertArg( _cmpEdgeUid(resKey.edgeUid, jdtLoc.allImgEdgeUids()), s"edgeUID#${resKey.edgeUid}? ![${jdt.legacyEdgeUids.iterator.mkString(",")}]" )

      jdtLoc
        .setLabel {
          val setF = jdt.name match {
            case MJdTagNames.QD_OP =>
              JdTag.qdProps
                .andThen( Traversal.fromTraverse[Option, MQdOp] )
                .andThen( MQdOp.edgeInfo )
                .replace _
            case MJdTagNames.STRIP =>
              _jdtag_p1_bgImg_LENS
                .replace _
            case jdtName =>
              throw new UnsupportedOperationException(resKey.toString + HtmlConstants.SPACE + newValue + HtmlConstants.SPACE + jdtName)
          }
          setF( newValue )(jdt)
        }
        .toTree
    }

    override def forgetEdge(view: Tree[JdTag], edgeUid: EdgeUid_t): Tree[JdTag] = {
      // Аккуратно отмаппить все теги: если bgImg содержит edgeUid, то обнулить bgImg.
      val lens = _jdtag_p1_bgImg_LENS
      for (jdt0 <- view) yield {
        if ( lens.get(jdt0).exists(_.edgeUid ==* edgeUid) ) {
          (lens replace None)(jdt0)
        } else {
          jdt0
        }
      }
    }

  }


  private def _jdtag_p1_bgImg_LENS = {
    JdTag.props1
      .andThen( MJdProps1.bgImg )
  }

}

