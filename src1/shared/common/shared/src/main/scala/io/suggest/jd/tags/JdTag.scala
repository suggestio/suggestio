package io.suggest.jd.tags

import io.suggest.ad.blk.BlockMeta
import io.suggest.color.MColorData
import io.suggest.common.empty.EmptyUtil
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.jd.MJdEdgeId
import io.suggest.jd.tags.qd.{MQdOp, MQdOpTypes}
import io.suggest.n2.edge.EdgeUid_t
import io.suggest.primo.{IEqualsEq, IHashCodeLazyVal}
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.css.Css
import io.suggest.jd.tags.event.MJdtEvents
import io.suggest.jd.tags.html.{MJdHtml, MJdHtmlTypes}
import io.suggest.scalaz.ScalazUtil.Implicits._
import japgolly.univeq._
import monocle.macros.GenLens
import play.api.libs.functional.syntax._
import play.api.libs.json._
import scalaz.{EphemeralStream, Show, Tree, TreeLoc}
import io.suggest.scalaz.ZTreeUtil._
import io.suggest.text.StringUtil

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.08.17 12:19
  * Description: Интерфейс каждого элемента структуры документа.
  * Структура аналогична html/xml-тегам, но завязана на JSON и названа структурой, чтобы не путаться.
  */
object JdTag {

  object Fields {
    val TYPE_FN = "t"
    val PROPS_FN = "p"
    val QD_PROPS_FN = "q"
    val EVENTS_FN = "e"
    val HTML_FN = "html"
  }


  /** Полиморфная поддержка play-json. */
  implicit val JD_TAG_FORMAT: OFormat[JdTag] = (
    (__ \ Fields.TYPE_FN).format[MJdTagName] and
    (__ \ Fields.PROPS_FN).formatNullable[MJdProps1]
      .inmap[MJdProps1](
        EmptyUtil.opt2ImplMEmptyF(MJdProps1),
        EmptyUtil.implEmpty2OptF
      ) and
    (__ \ Fields.QD_PROPS_FN).formatNullable[MQdOp] and
    (__ \ Fields.EVENTS_FN).formatNullable[MJdtEvents]
      .inmap[MJdtEvents](
        EmptyUtil.opt2ImplMEmptyF(MJdtEvents),
        jdtEvents => Option.when( jdtEvents.nonEmpty )( jdtEvents ),
      ) and
    (__ \ Fields.HTML_FN).formatNullable[MJdHtml]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[JdTag] = UnivEq.derive


  /** Краткая форма сборки top-level jd-тега с контентом. */
  def document: JdTag = {
    apply(MJdTagNames.DOCUMENT)
  }

  /** Сборка IDocTag, рендерящего примитивный текст по его id эджа. */
  def qd(topLeft: MCoords2di = null): JdTag = {
    JdTag(
      MJdTagNames.QD_CONTENT,
      props1 = Option(topLeft).fold(MJdProps1.empty) { tL =>
        MJdProps1(
          topLeft = Some(tL)
        )
      }
    )
  }

  def edgeQdOp(edgeUid: EdgeUid_t): JdTag = {
    qdOp(
      MQdOp(
        opType    = MQdOpTypes.Insert,
        edgeInfo  = Some( MJdEdgeId(edgeUid) )
      )
    )
  }

  def edgeQdTree(edgeUid: EdgeUid_t, coords: MCoords2di): Tree[JdTag] = {
    Tree.Node(
      JdTag.qd(coords),
      forest = EphemeralStream(
        Tree.Leaf(
          JdTag.edgeQdOp( edgeUid )
        )
      )
    )
  }

  def qdOp(qdOp: MQdOp): JdTag = {
    JdTag(
      name    = MJdTagNames.QD_OP,
      qdProps = Some( qdOp )
    )
  }

  /** Быстрая сборка стрипа. */
  def block(bm: BlockMeta, bgColor: Option[MColorData] = None): JdTag = {
    apply(
      MJdTagNames.STRIP,
      props1 = MJdProps1(
        bgColor     = bgColor,
        widthPx     = Some(bm.width),
        heightPx    = Some(bm.height),
        expandMode  = bm.expandMode,
      )
    )
  }


  /** Утиль для поддержки z.Tree с jd-тегами. */
  implicit final class JdTagTreeOps[From: IJdTagGetter](private val tree: Tree[From]) {

    def qdOps: EphemeralStream[MQdOp] = {
      tree
        .flatten
        .tailOption
        .getOrElse( EphemeralStream[From] )
        .flatMap( _.qdProps.toEphemeralStream )
    }

    def deepEdgesUids: EphemeralStream[EdgeUid_t] = {
      (
        (
          tree
            .qdOps
            .flatMap( _.edgeInfo.toEphemeralStream ) ##::
          tree
            .rootLabel
            .props1
            .bgImg
            .toEphemeralStream ##::
          tree.rootLabel.events.events.toEphemeralStream
            .flatMap(_.actions.toEphemeralStream)
            .flatMap(_.jdEdgeIds.toEphemeralStream) ##::
          EphemeralStream[EphemeralStream[MJdEdgeId]]
        )
          .flatten
          .map( _.edgeUid )
      ) ++
      tree
        .subForest
        .flatMap( _.deepEdgesUids )
    }


    def deepChildrenOfType(jdtName: MJdTagName): EphemeralStream[From] = {
      tree
        .deepChildren
        .filter( _.name ==* jdtName )
    }

    def deepOfType(jdtName: MJdTagName): EphemeralStream[From] = {
      val chs = deepChildrenOfType(jdtName)
      val jdt = tree.rootLabel
      if (jdt.name ==* jdtName) {
        jdt ##:: chs
      } else {
        chs
      }
    }

    /** Вернуть главый блок, либо первый блок. */
    def getMainBlock: Option[(Tree[From], Int)] = {
      tree
        .subForest
        .zipWithIndex
        .filter( _._1.rootLabel.props1.isMain.getOrElseFalse )
        .headOption
    }

    /** Вернуть главый блок, либо первый блок.
      *
      * @param blockOnly В качестве блока возвращать только главный БЛОК или допускать вариации?
      *                   true - жестко возвращать MJdTagNames.STRIP или exception.
      *                   false - возвращать что угодно.
      *                   None - попытаться вернуть БЛОК, но если блоков нет, то вернуть то, что возможно.
      * @return
      */
    def getMainBlockOrFirst(blockOnly: Option[Boolean] = None): Option[(Tree[From], Int)] = {
      tree.rootLabel.name match {
        case MJdTagNames.DOCUMENT =>
          getMainBlock
            .orElse {
              // main-блок не найден. Выбрать первый блок:
              val allForestInx = tree
                .subForest
                .zipWithIndex
              var forestInx = allForestInx

              // 2021.03.17 При обсчёте тарифов размещения карточки требуется именно БЛОК, а не qd-bl контент, иначе будет ошибка.
              if (!(blockOnly contains[Boolean] false)) {
                forestInx = forestInx
                  .filter(_._1.rootLabel.name ==* MJdTagNames.STRIP)

                // Если чёткий критерий blocksOnly не задан, но блоков не найдено, вернуть то, что есть.
                if (blockOnly.isEmpty && forestInx.isEmpty)
                  forestInx = allForestInx
              }

              forestInx
                .headOption
            }
        case jdtName if !(blockOnly contains[Boolean] true) || (jdtName ==* MJdTagNames.STRIP) =>
          // По идее, поиск главного блока в блоке - это логическая ошибка. Но шаблон бывает разный, и это может быть предосторожность, поэтому реагируем молча.
          Some( tree -> 0 )
        case _ =>
          None
      }
    }

    def getMainBgColor: Option[MColorData] = {
      for {
        (mainJdtTree, _) <- getMainBlockOrFirst()
        bgColor <- mainJdtTree.rootLabel.props1.bgColor
      } yield {
        bgColor
      }
    }

    def edgesUidsMap: Map[EdgeUid_t, MJdEdgeId] = {
      tree
        .flatten
        .flatMap { m =>
          val jdt = m: JdTag
          jdt.allEdgeIds
        }
        .iterator
        .zipWithIdIter[EdgeUid_t]
        .to( Map )
    }

    def gridItemsIter: Iterator[Tree[From]] = {
      tree.rootLabel.name match {
        case MJdTagNames.DOCUMENT =>
          tree.subForest.iterator
        case _ =>
          Iterator.single( tree )
      }
    }

    def allImgEdgeUids: EphemeralStream[MJdEdgeId] = {
      val htmlImgEdgeUidsCache = new HtmlImgEdgeUidsCache
      tree
        .cobindLoc
        .flatten
        .flatMap { treeLoc =>
          (treeLoc.getLabel: JdTag).legacyEdgeUids ++
          treeLoc.htmlImgEdgeUids( htmlImgEdgeUidsCache )
        }
    }

  }


  /** Several cached instances for calling treeLoc.htmlImgEdgeUids() inside loop. */
  final class HtmlImgEdgeUidsCache {
    val EMPTY_ESTREAM = EphemeralStream[MJdEdgeId]
    lazy val CSS_IMAGE_ATTRS = Css.Images.IMAGE_ATTR_NAMES_SET
  }


  /** Дополнительная утиль для TreeLoc[IDocTag]. */
  implicit final class JdTagTreeLocOps[From: IJdTagGetter](private val treeLoc: TreeLoc[From]) {

    def findUpByType(types: MJdTagName*): Option[TreeLoc[From]] = {
      treeLoc.findUp( treeLocByTypeFilterF(types) )
    }

    def findByType(types: MJdTagName*): Option[TreeLoc[From]] = {
      treeLoc.find( treeLocByTypeFilterF(types) )
    }

    def findByEdgeUid(edgeUid: EdgeUid_t): Option[TreeLoc[From]] = {
      treeLoc.find {
        _.getLabel.qdProps.exists(
          _.edgeInfo.exists(
            _.edgeUid ==* edgeUid))
      }
    }


    /** All image edges in current html tag or attr.
      * HTML-tag attributes and style attributes are stored in JD-tree as jd-subtags with html prop.
      * To properly detect for only-images-related edgeUids, we need to walk parent tree.
      *
      * Note: this method does not look into child nodes. For example, if calling this method on 'img' tag
      * with child attributes will return no results []. Method only may look up to parent tag (tags).
      */
    def htmlImgEdgeUids(cache: HtmlImgEdgeUidsCache = new HtmlImgEdgeUidsCache): EphemeralStream[MJdEdgeId] = {
      val jdt = treeLoc.getLabel
      (for {
        jdHtml <- jdt.html.toEphemeralStream
        if jdHtml.edgeUid.nonEmpty
        r <- (jdHtml.htmlType match {
          // We only process here current tree node with possible information from parent nodes.
          case MJdHtmlTypes.Attribute =>
            if (
              // Check for img.src HTML-attribute:
              (
                (jdHtml.key contains[String] "src") &&
                // 'src' attribute here. Is it related to img tag?
                treeLoc.parent.exists(
                  _.getLabel.html.exists { parentJdHtml =>
                    (parentJdHtml.key contains[String] "img") &&
                    (parentJdHtml.htmlType ==* MJdHtmlTypes.Tag)
                  }
                )
              ) || (
                // Check for image-related CSS styles names:
                jdHtml.key.exists( cache.CSS_IMAGE_ATTRS.contains ) &&
                treeLoc.parent.exists(
                  _.getLabel.html.exists { parentJdHtml =>
                    (parentJdHtml.htmlType ==* MJdHtmlTypes.Attribute) &&
                    (parentJdHtml.key contains[String] "style")
                  }
                )
              )
            ) {
              // Return all edgeUids here.
              jdHtml.edgeUid.toEphemeralStream

            } else {
              // All current edgeUids here are unrelated to images.
              cache.EMPTY_ESTREAM
            }

          // Do not touch html-tag's child attributes here: tree-walk cycle should be run somewhere outside.
          case _ => cache.EMPTY_ESTREAM
        })
      } yield {
        r
      })
    }

    def allImgEdgeUids(cache: HtmlImgEdgeUidsCache = new HtmlImgEdgeUidsCache): EphemeralStream[MJdEdgeId] = {
      htmlImgEdgeUids(cache) ++ (treeLoc.getLabel: JdTag).legacyEdgeUids
    }

  }


  def treeLocByTypeFilterF[From: IJdTagGetter](types: Iterable[MJdTagName]): TreeLoc[From] => Boolean = {
    if (types.isEmpty) {
      throw new IllegalArgumentException( types.toString() )
    } else {
      loc: TreeLoc[From] =>
        types.exists(_ ==* loc.getLabel.name)
    }
  }


  /** toString для scalaz. */
  implicit def jdTagShow: Show[JdTag] = Show.showFromToString[JdTag]


  /** Поиск и устранение неиспользуемых эджей.
    *
    * @param tpl Шаблон документа.
    * @param edgesMap Карта эджей, где значение не важно абсолютно.
    * @tparam E Тип эджа (любой).
    * @return Прочищенная карта эджей.
    */
  def purgeUnusedEdges[E](tpl: Tree[JdTag], edgesMap: Map[EdgeUid_t, E]): Map[EdgeUid_t, E] = {
    val deepEdgesUidsEph = tpl.deepEdgesUids
    if (deepEdgesUidsEph.isEmpty) {
      Map.empty
    } else {
      val neededEdgesUids = deepEdgesUidsEph
        .iterator
        .toSet

      val keys2del = edgesMap.keySet -- neededEdgesUids
      if (keys2del.isEmpty) {
        edgesMap
      } else {
        edgesMap -- keys2del
      }
    }
  }


  def name = GenLens[JdTag](_.name)
  def props1 = GenLens[JdTag](_.props1)
  def qdProps = GenLens[JdTag](_.qdProps)
  def events = GenLens[JdTag](_.events)
  def html = GenLens[JdTag](_.html)


  /** JdTag API extensions. */
  implicit final class JdTagExt( private val jdt: JdTag ) extends AnyVal {

    /** Все эджи картинок, упомянутых в этом теге. */
    def legacyEdgeUids: EphemeralStream[MJdEdgeId] = {
      (
        jdt.props1.bgImg ##::
        // Inspect quill-delta:
        jdt.qdProps.flatMap(_.edgeInfo) ##::
        EphemeralStream[Option[MJdEdgeId]]
      )
        .flatMap(_.toEphemeralStream)
    }

    /** Return any html edges, defined inside current tag. */
    def htmlEdgeIds: List[MJdEdgeId] = {
      (for {
        jdHtml <- jdt.html
      } yield {
        jdHtml.edgeUid
      })
        .getOrElse( Nil )
    }


    /** Return all edges uids. */
    def allEdgeIds: EphemeralStream[MJdEdgeId] =
      legacyEdgeUids ++ htmlEdgeIds.toEphemeralStream

  }

}


/** Интерфейс для всех "тегов" структуры документа.
  *
  * IEqualsEq:
  * Теги из дерева используются как ключи в ScalaCSS styleF Map[X,_] прямо во время рендера.
  * Во время тормозных react-рендеров и перерендеров в браузере, ключи должны **очень** быстро работать,
  * поэтому всё оптимизировано по самые уши ценой невозможности сравнивания разных тегов между собой.
  *
  * @param qdProps Список qd-операций для постройки контента (quill-delta).
  * @param events Контейнер данных по событиям.
  * @param html HTML-tag description.
  *             If defined, current JdTag must have name=HTML, and will be rendered into described HTML tag.
  */
final case class JdTag(
                        name      : MJdTagName,
                        props1    : MJdProps1     = MJdProps1.empty,
                        qdProps   : Option[MQdOp] = None,
                        events    : MJdtEvents    = MJdtEvents.empty,
                        html      : Option[MJdHtml] = None,
                      )
  // lazy val hashCode: на клиенте желательно val, на сервере - просто дефолт (def). Что тут делать, elidable нужен какой-то?
  // TODO После ввода MJdTagId становится не ясно, надо ли *val* hashCode. Может теперь def достаточно?
  extends IHashCodeLazyVal
  with IEqualsEq
{

  override def toString: String = {
    StringUtil.toStringHelper(this, 256) { renderF =>
      val F = JdTag.Fields
      val render0 = renderF("")
      render0(name)
      if (props1.nonEmpty) render0(props1)
      qdProps foreach renderF( F.QD_PROPS_FN )
      html foreach renderF( F.HTML_FN )
    }
  }

}
