package io.suggest.jd.tags

import io.suggest.ad.blk.BlockMeta
import io.suggest.color.MColorData
import io.suggest.common.empty.EmptyUtil
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.jd.MJdEdgeId
import io.suggest.jd.tags.qd.{MQdOp, MQdOpTypes}
import io.suggest.model.n2.edge.{EdgeUid_t, EdgesUtil}
import io.suggest.primo.{IEqualsEq, IHashCodeLazyVal}
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.primo.id.IId
import japgolly.univeq._
import monocle.macros.GenLens
import play.api.libs.functional.syntax._
import play.api.libs.json._
import scalaz.{Show, Tree, TreeLoc}

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


  /** Билдер-функция для более удобной ручной сборки инстансов [[JdTag]] по сравнению с apply.
    * Нельзя объединить с apply() из-за ограничений copy().
    */
  def a(jdTagName : MJdTagName, props1: MJdtProps1 = MJdtProps1.empty, qdProps: Option[MQdOp] = None): JdTag = {
    apply(jdTagName, props1, qdProps)
  }


  /** Краткая форма сборки top-level jd-тега с контентом. */
  def document: JdTag = {
    apply(MJdTagNames.DOCUMENT)
  }

  /** Сборка IDocTag, рендерящего примитивный текст по его id эджа. */
  def qd(topLeft: MCoords2di): JdTag = {
    JdTag.a(
      MJdTagNames.QD_CONTENT,
      props1 = MJdtProps1(
        topLeft = Some(topLeft)
      )
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
  def strip(bm: BlockMeta, bgColor: Option[MColorData] = None): JdTag = {
    apply(
      MJdTagNames.STRIP,
      props1 = MJdtProps1(
        bgColor = bgColor,
        bm      = Some(bm)
      )
    )
  }



  /** Дополнительные методы для Option[IDocTag]. */
  implicit class DocTagOptExt(val opt: Option[JdTag]) extends AnyVal {

    /** Быстрая фильтрация Option'а по типу.  */
    def filterByType(jdtName: MJdTagName): Option[JdTag] = {
      opt.filter(_.name ==* jdtName)
    }

  }

  /** Утиль для поддержки z.Tree с jd-тегами. */
  implicit class JdTagTreeOps(private val tree: Tree[JdTag]) extends AnyVal {

    import io.suggest.scalaz.ZTreeUtil._

    def qdOpsIter: Iterator[MQdOp] = {
      tree.flatten
        .tail
        .iterator
        .flatMap(_.qdProps)
    }

    def deepEdgesUidsIter: Iterator[EdgeUid_t] = {
      val jdt = tree.rootLabel
      val iter1 = tree
        .qdOpsIter
        .flatMap(_.edgeInfo)
      val iter2 = jdt.props1.bgImg
        .iterator
      val iter12 = (iter1 ++ iter2)
        .map(_.edgeUid)
      val iterChs = tree.subForest
        .iterator
        .flatMap(_.deepEdgesUidsIter)
      iter12 ++ iterChs
    }


    def deepChildrenOfTypeIter(jdtName: MJdTagName): Iterator[JdTag] = {
      tree
        .deepChildren
        .iterator
        .filter( _.name ==* jdtName )
    }

    def deepOfTypeIter(jdtName: MJdTagName): Iterator[JdTag] = {
      def chIter = deepChildrenOfTypeIter(jdtName)
      val jdt = tree.rootLabel
      if (jdt.name ==* jdtName) {
        Iterator.single(jdt) ++ chIter
      } else {
        chIter
      }
    }

    /** Вернуть главый блок, либо первый блок. */
    def getMainBlock: Option[Tree[JdTag]] = {
      tree.subForest
        .find( _.rootLabel.props1.isMain.getOrElseFalse )
    }

    /** Вернуть главый блок, либо первый блок. */
    def getMainBlockOrFirst: Tree[JdTag] = {
      getMainBlock.getOrElse {
        tree.subForest.head
      }
    }

    def edgesUidsMap: Map[EdgeUid_t, MJdEdgeId] = {
      IId.els2idMap[EdgeUid_t, MJdEdgeId](
        tree.flatten
          .iterator
          .flatMap(_.edgeUids)
      )
    }

  }


  /** Дополнительная утиль для TreeLoc[IDocTag]. */
  implicit class JdTagTreeLocOps(private val treeLoc: TreeLoc[JdTag]) extends AnyVal {

    // TODO Унести в ScalazUtil
    def findUp(f: TreeLoc[JdTag] => Boolean): Option[TreeLoc[JdTag]] = {
      if ( f(treeLoc) ) {
        Some(treeLoc)
      } else {
        treeLoc
          .parent
          .flatMap( _.findUp(f) )
      }
    }

    def findUpByType(typ: MJdTagName): Option[TreeLoc[JdTag]] = {
      findUp( treeLocByTypeFilterF(typ) )
    }

    def findByType(typ: MJdTagName): Option[TreeLoc[JdTag]] = {
      treeLoc.find( treeLocByTypeFilterF(typ) )
    }

  }


  def treeLocByTypeFilterF(typ: MJdTagName) = {
    loc: TreeLoc[JdTag] =>
      loc.getLabel.name ==* typ
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
    EdgesUtil.purgeUnusedEdgesFromMap(
      usedEdgeIds = tpl.deepEdgesUidsIter.toSet,
      edgesMap    = edgesMap
    )
  }


  val name = GenLens[JdTag](_.name)
  val props1 = GenLens[JdTag](_.props1)
  val qdProps = GenLens[JdTag](_.qdProps)

}


/** Интерфейс для всех "тегов" структуры документа.
  *
  * IIdentityFastEq:
  * Теги из дерева используются как ключи в ScalaCSS styleF Map[X,_] прямо во время рендера.
  * Во время тормозных react-рендеров и перерендеров в браузере, ключи должны **очень** быстро работать,
  * поэтому всё оптимизировано по самые уши ценой невозможности сравнивания разных тегов между собой.
  * @param qdProps Список qd-операций для постройки контента (quill-delta).
  */
final case class JdTag(
                        name      : MJdTagName,
                        props1    : MJdtProps1    = MJdtProps1.empty,
                        qdProps   : Option[MQdOp] = None
                      )
  extends IHashCodeLazyVal
  // TODO Opt: lazy val: на клиенте желательно val, на сервере - просто дефолт (def). Что тут делать, elidable нужен какой-то?
  with IEqualsEq
{

  def withProps1(props1: MJdtProps1)              = copy(props1 = props1)
  /** Для удобства написания тестов, props1 можно обновлять функцией. */
  def updateProps1(f: MJdtProps1 => MJdtProps1)   = withProps1(f(props1))

  def withQdProps(qdProps: Option[MQdOp])         = copy(qdProps = qdProps)

  def edgeUids: Iterable[MJdEdgeId] = {
    props1.bgImg ++
      qdProps.flatMap(_.edgeInfo)
  }

  override def toString: String = {
    "#" + name +
      (if (props1.isEmpty) "" else "," + props1) +
      qdProps.fold("")(_.toString)
  }

}
