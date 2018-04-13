package io.suggest.lk.c

import io.suggest.common.html.HtmlConstants
import io.suggest.err.ErrorConstants
import io.suggest.img.MImgEdgeWithOps
import io.suggest.jd.MJdEdgeId
import io.suggest.jd.tags.{JdTag, MJdTagNames}
import io.suggest.lk.m.MFormResourceKey
import io.suggest.model.n2.edge.{EdgeUid_t, MPredicates}
import scalaz.Tree
import japgolly.univeq._

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
  def get(view: V, resKey: MFormResourceKey): Option[MImgEdgeWithOps]

  /** Обновить указатель на картинку, вернув обновлённый V. */
  def updated(view: V, resKey: MFormResourceKey)(newValue: Option[MImgEdgeWithOps]): V

  def updateWith(view: V, resKey: MFormResourceKey)(f: Option[MImgEdgeWithOps] => Option[MImgEdgeWithOps]): V = {
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
  implicit object DummyAdp extends IPictureViewAdp[Option[MImgEdgeWithOps]] {
    override def get(view: Option[MImgEdgeWithOps], resKey: MFormResourceKey): Option[MImgEdgeWithOps] = {
      view
    }
    override def updated(view: Option[MImgEdgeWithOps], resKey: MFormResourceKey)(newValue: Option[MImgEdgeWithOps]): Option[MImgEdgeWithOps] = {
      newValue
    }
    override def forgetEdge(view: Option[MImgEdgeWithOps], edgeUid: EdgeUid_t): Option[MImgEdgeWithOps] = {
      view
        .filterNot { p =>
          p.imgEdge.edgeUid ==* edgeUid
        }
    }
  }


  /** Поддержка управления представлением картинок, которые хранятся в дереве jd-тегов. */
  implicit object JdTagTreeBgImgAdp extends IPictureViewAdp[Tree[JdTag]] {

    import io.suggest.scalaz.ZTreeUtil.ZTreeJdOps

    private def _cmpEdgeUid(keyEdgeUid: Option[EdgeUid_t], edgeUids: Iterable[MJdEdgeId]): Boolean = {
      (keyEdgeUid.isEmpty && edgeUids.isEmpty) ||
        keyEdgeUid.exists(ei => edgeUids.exists(_.edgeUid ==* ei))
    }

    override def get(view: Tree[JdTag], resKey: MFormResourceKey): Option[MImgEdgeWithOps] = {
      // Найти в дереве тег, у которого bgImg подпадает по ключ, вернуть bgImg.
      resKey.pred.flatMap {
        case MPredicates.JdContent.Image =>
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
                  MImgEdgeWithOps(ei)
                }
              }
          } yield {
            imgEdgeWithOps
          }

        case _ =>
          throw new UnsupportedOperationException( resKey.toString  )
      }
    }

    override def updated(view: Tree[JdTag], resKey: MFormResourceKey)(newValue: Option[MImgEdgeWithOps]): Tree[JdTag] = {
      // Найти и обновить bgImg для тега, который подпадает под запрашиваемый ключ.
      val pred = resKey.pred.get
      pred match {
        case MPredicates.JdContent.Image =>
          val nodePath = resKey.nodePath.get
          val jdtLoc = view.pathToNode(nodePath).get
          val jdt = jdtLoc.getLabel
          ErrorConstants.assertArg( _cmpEdgeUid(resKey.edgeUid, jdt.edgeUids) )

          jdtLoc
            .setLabel {
              jdt.name match {
                case MJdTagNames.QD_OP =>
                  jdt.withQdProps(
                    jdt.qdProps.map { qdProps =>
                      qdProps.withEdgeInfo(
                        newValue.map(_.imgEdge)
                      )
                    }
                  )
                case MJdTagNames.STRIP =>
                  jdt.withProps1(
                    jdt.props1.withBgImg( newValue )
                  )
                case jdtName =>
                  throw new UnsupportedOperationException(resKey + HtmlConstants.SPACE + newValue + HtmlConstants.SPACE + jdtName)
              }
            }
            .toTree
        case _ =>
          throw new UnsupportedOperationException( resKey.toString  )
      }
    }

    override def forgetEdge(view: Tree[JdTag], edgeUid: EdgeUid_t): Tree[JdTag] = {
      // Аккуратно отмаппить все теги: если bgImg содержит edgeUid, то обнулить bgImg.
      for (jdt0 <- view) yield {
        if (jdt0.props1.bgImg.exists(_.imgEdge.edgeUid ==* edgeUid)) {
          jdt0.withProps1(
            jdt0.props1
              .withBgImg( None )
          )
        } else {
          jdt0
        }
      }
    }

  }

}

