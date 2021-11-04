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
import io.suggest.jd.tags.event.MJdtEvents
import io.suggest.jd.tags.html.MJdHtmlTag
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
    val HTML_FN = "h"
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
    (__ \ Fields.HTML_FN).formatNullable[MJdHtmlTag]
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
      EphemeralStream.toIterable(
        tree
          .flatten
          .flatMap { m =>
            (m: JdTag)
              .imgEdgeUids
              .toEphemeralStream
          }
      )
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


  implicit final class JdTagExt( private val jdt: JdTag ) extends AnyVal {

    /** Все эджи картинок, упомянутых в этом теге. */
    def imgEdgeUids: LazyList[MJdEdgeId] = {
      (
        // Ищем в фоновой картинке jd-тега:
        jdt.props1.bgImg #::
        // Ищем в html-теге:
        (for {
          htmlTag <- jdt.html
          // Ищем в аттрибуте <img.src> инфу по edgeUid. Возможно, есть какие-либо ещё варианты тегом-аттрибутов?
          if htmlTag.tagName ==* "img"
          imgSrcAV <- htmlTag.attrs.get( "src" )
          ei <- imgSrcAV.edgeUid
        } yield {
          ei
        }) #::
        // Ищем в quill-delta:
        jdt.qdProps.flatMap(_.edgeInfo) #::
        LazyList.empty
      )
        .flatten
    }

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
                        html      : Option[MJdHtmlTag] = None,
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
