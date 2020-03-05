package io.suggest.jd.tags

import io.suggest.ad.blk.BlockMeta
import io.suggest.color.MColorData
import io.suggest.common.empty.EmptyUtil
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.jd.MJdEdgeId
import io.suggest.jd.tags.qd.{MQdOp, MQdOpTypes}
import io.suggest.n2.edge.{EdgeUid_t, EdgesUtil}
import io.suggest.primo.{IEqualsEq, IHashCodeLazyVal}
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.msg.ErrorMsgs
import japgolly.univeq._
import monocle.macros.GenLens
import play.api.libs.functional.syntax._
import play.api.libs.json._
import scalaz.{Show, Tree, TreeLoc}
import io.suggest.scalaz.ZTreeUtil._

import scala.collection.MapView

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
    /** Пропертисы для одной qd-операции, если есть. */
    val QD_PROPS_FN = "q"
  }


  /** Полиморфная поддержка play-json. */
  implicit val JD_TAG_FORMAT: OFormat[JdTag] = (
    (__ \ Fields.TYPE_FN).format[MJdTagName] and
    (__ \ Fields.PROPS_FN).formatNullable[MJdtProps1]
      .inmap[MJdtProps1](
        EmptyUtil.opt2ImplMEmptyF(MJdtProps1),
        EmptyUtil.implEmpty2OptF
      ) and
    (__ \ Fields.QD_PROPS_FN).formatNullable[MQdOp]
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
      props1 = Option(topLeft).fold(MJdtProps1.empty) { tL =>
        MJdtProps1(
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
      forest = Stream(
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
      props1 = MJdtProps1(
        bgColor     = bgColor,
        widthPx     = Some(bm.width),
        heightPx    = Some(bm.height),
        expandMode  = bm.expandMode,
      )
    )
  }


  /** Утиль для поддержки z.Tree с jd-тегами. */
  implicit class JdTagTreeOps[From: IJdTagGetter](private val tree: Tree[From]) {

    def qdOps: LazyList[MQdOp] = {
      tree
        .flatten
        .tail
        .iterator
        .flatMap(_.qdProps)
        .to( LazyList )
    }

    def deepEdgesUids: LazyList[EdgeUid_t] = {
      (
        (
          tree
            .qdOps
            .flatMap(_.edgeInfo) #:::
          tree
            .rootLabel
            .props1
            .bgImg
            .to(LazyList)
        )
        .map(_.edgeUid)
      ) #::: tree
        .subForest
        .iterator
        .flatMap(_.deepEdgesUids)
        .to(LazyList)
    }


    def deepChildrenOfType(jdtName: MJdTagName): Stream[From] = {
      tree
        .deepChildren
        .filter( _.name ==* jdtName )
    }

    def deepOfType(jdtName: MJdTagName): Stream[From] = {
      val chs = deepChildrenOfType(jdtName)
      val jdt = tree.rootLabel
      if (jdt.name ==* jdtName) {
        jdt #:: chs
      } else {
        chs
      }
    }

    /** Вернуть главый блок, либо первый блок. */
    def getMainBlock: Option[(Tree[From], Int)] = {
      tree
        .subForest
        .iterator
        .zipWithIndex
        .find( _._1.rootLabel.props1.isMain.getOrElseFalse )
    }

    /** Вернуть главый блок, либо первый блок. */
    def getMainBlockOrFirst: (Tree[From], Int) = {
      tree.rootLabel.name match {
        case MJdTagNames.DOCUMENT =>
          getMainBlock
            .orElse {
              tree
                .subForest
                .zipWithIndex
                .headOption
            }
            .getOrElse {
              // Поиск блока в пустом документе - это ненормально, падаем.
              throw new IllegalStateException( (ErrorMsgs.JD_TREE_UNEXPECTED_CHILDREN, tree.rootLabel).toString() )
            }
        case MJdTagNames.STRIP =>
          // По идее, поиск главного блока в блоке - это логическая ошибка. Но шаблон бывает разный, и это может быть предосторожность, поэтому реагируем молча.
          tree -> 0
        case other =>
          throw new IllegalArgumentException( (ErrorMsgs.JD_TREE_UNEXPECTED_ROOT_TAG, other).toString() )
      }
    }

    def edgesUidsMap: Map[EdgeUid_t, MJdEdgeId] = {
      tree
        .flatten
        .iterator
        .flatMap(_.edgeUids)
        .zipWithIdIter[EdgeUid_t]
        .to( Map )
    }

  }


  /** Дополнительная утиль для TreeLoc[IDocTag]. */
  implicit class JdTagTreeLocOps[From: IJdTagGetter](private val treeLoc: TreeLoc[From]) {

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
  def purgeUnusedEdges[E](tpl: Tree[JdTag], edgesMap: Map[EdgeUid_t, E]): MapView[EdgeUid_t, E] = {
    EdgesUtil.purgeUnusedEdgesFromMap(
      usedEdgeIds = tpl.deepEdgesUids.toSet,
      edgesMap    = edgesMap
    )
  }


  val name = GenLens[JdTag](_.name)
  val props1 = GenLens[JdTag](_.props1)
  val qdProps = GenLens[JdTag](_.qdProps)

}


/** Интерфейс для всех "тегов" структуры документа.
  *
  * IEqualsEq:
  * Теги из дерева используются как ключи в ScalaCSS styleF Map[X,_] прямо во время рендера.
  * Во время тормозных react-рендеров и перерендеров в браузере, ключи должны **очень** быстро работать,
  * поэтому всё оптимизировано по самые уши ценой невозможности сравнивания разных тегов между собой.
  *
  * @param qdProps Список qd-операций для постройки контента (quill-delta).
  */
final case class JdTag(
                        name      : MJdTagName,
                        props1    : MJdtProps1    = MJdtProps1.empty,
                        qdProps   : Option[MQdOp] = None
                      )
  // lazy val hashCode: на клиенте желательно val, на сервере - просто дефолт (def). Что тут делать, elidable нужен какой-то?
  // TODO После ввода MJdTagId становится не ясно, надо ли *val* hashCode. Может теперь def достаточно?
  extends IHashCodeLazyVal
  with IEqualsEq
{

  def edgeUids: LazyList[MJdEdgeId] = {
    (props1.bgImg #::
      qdProps.flatMap(_.edgeInfo) #::
      LazyList.empty
    )
      .flatten
  }

  override def toString: String = {
    "#" + name +
      (if (props1.isEmpty) "" else "," + props1) +
      qdProps.fold("")(_.toString)
  }

}
