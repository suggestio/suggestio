package io.suggest.jd.tags

import io.suggest.ad.blk.BlockMeta
import io.suggest.color.MColorData
import io.suggest.common.coll.Lists
import io.suggest.common.empty.EmptyUtil
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.jd.MJdEdgeId
import io.suggest.jd.tags.qd.{MQdOp, MQdOpTypes}
import io.suggest.model.n2.edge.EdgeUid_t
import io.suggest.primo.{IEqualsEq, IHashCodeLazyVal}
import io.suggest.scalaz.NodePath_t
import japgolly.univeq._
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.annotation.tailrec
import scalaz.Tree

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.08.17 12:19
  * Description: Интерфейс каждого элемента структуры документа.
  * Структура аналогична html/xml-тегам, но завязана на JSON и названа структурой, чтобы не путаться.
  */
object IDocTag {

  object Fields {
    val TYPE_FN = "t"
    val PROPS_FN = "p"
    /** Имя поля с дочерними элементами. По идее -- оно одно на все реализации. */
    val CHILDREN_FN = "c"
  }


  /** Полиморфная поддержка play-json. */
  implicit val IDOC_TAG_FORMAT: OFormat[IDocTag] = (
    (__ \ Fields.TYPE_FN).format[MJdTagName] and
    (__ \ Fields.PROPS_FN).formatNullable[MJdtProps1]
      .inmap[MJdtProps1](
        EmptyUtil.opt2ImplMEmptyF(MJdtProps1),
        EmptyUtil.implEmpty2OptF
      ) and
    (__ \ Fields.CHILDREN_FN).lazyFormatNullable( implicitly[Format[Seq[IDocTag]]] )
      .inmap[Seq[IDocTag]](
        EmptyUtil.opt2ImplEmpty1F(Nil),
        { chs => if (chs.isEmpty) None else Some(chs) }
      )
  )(apply, unlift(unapply))

  implicit def univEq: UnivEq[IDocTag] = UnivEq.force


  /** Билдер-функция для более удобной ручной сборки инстансов [[IDocTag]] по сравнению с apply.
    * Нельзя объединить с apply() из-за ограничений copy().
    */
  def a(jdTagName : MJdTagName, props1: MJdtProps1 = MJdtProps1.empty)
       (children: IDocTag*): IDocTag = {
    apply(jdTagName, props1, children)
  }


  def batchUpdateOne(source: IDocTag)(batches: JdBatch_t*): Seq[IDocTag] = {
    batchUpdateOne2(source, batches)
  }
  def batchUpdateOne2(source: IDocTag, batches: JdBatches_t): Seq[IDocTag] = {
    // Отработать children
    val children0 = source.children
    val source2 = if (children0.nonEmpty) {
      val children2 = batchUpdate2(children0, batches)
      // Изменилось ли хоть что-то?
      if ( Lists.isElemsEqs(children0, children2) ) {
        // Дочерние элементы не изменились.
        source
      } else {
        // Да, есть изменение -- заливаем новые children внутрь.
        source.withChildren( children2 )
      }
    } else {
      // Нет children -- пропускаем их обработку.
      source
    }
    // Отработать текущий тег.
    // Пройтись по списку batch'ей на предмет совпадения тега.
    batches.foldLeft [Seq[IDocTag]] ( source2 :: Nil ) {
      case (acc0, (fdt, f)) =>
        if (source ==* fdt) {
          // Применить текущую batch-функцию к аккамулятору.
          f(acc0)
        } else {
          acc0
        }
    }
  }
  def batchUpdate(source: IDocTag*)(batches: JdBatch_t*): Seq[IDocTag] = {
    batchUpdate2(source, batches)
  }
  /** Пакетный апдейт списка исходных тегов с помощью списка функций. */
  def batchUpdate2(sources: Seq[IDocTag], batches: JdBatches_t): Seq[IDocTag] = {
    sources.flatMap { jdt0 =>
      batchUpdateOne2(jdt0, batches)
    }
  }

  /** Типичные Batch-фунцкии и batch'и. */
  object Batches {
    /** Batch-функция удаления тега/тегов. */
    def delete(what: IDocTag): JdBatch_t = replace(what)

    def replaceF(replacements: IDocTag*): JdBatchF_t = {
      {_: Seq[IDocTag] => replacements }
    }
    def replace(what: IDocTag, replacements: IDocTag*): JdBatch_t = {
      what -> replaceF(replacements: _*)
    }
  }


  /** Краткая форма сборки top-level jd-тега с контентом. */
  def document(children: IDocTag*): IDocTag = {
    apply(MJdTagNames.DOCUMENT, children = children)
  }

  /** Сборка IDocTag, рендерящего примитивный текст по его id эджа. */
  def edgeQd(edgeUid: EdgeUid_t, topLeft: MCoords2di): IDocTag = {
    IDocTag.a(
      MJdTagNames.QUILL_DELTA,
      props1 = MJdtProps1(
        topLeft = Some(topLeft),
        qdOps   = List(
          MQdOp(
            opType    = MQdOpTypes.Insert,
            edgeInfo  = Some( MJdEdgeId(edgeUid) )
          )
        )
      )
    )()
  }

  /** Быстрая сборка стрипа. */
  def strip(bm: BlockMeta, bgColor: Option[MColorData] = None)(children: IDocTag*): IDocTag = {
    apply(
      MJdTagNames.STRIP,
      props1 = MJdtProps1(
        bgColor = bgColor,
        bm      = Some(bm)
      ),
      children = children
    )
  }


  /** Неявная утиль для тегов. */
  object Implicits {

    /** Дополнительные методы для Option[IDocTag]. */
    implicit class DocTagOptExt(val opt: Option[IDocTag]) extends AnyVal {

      /** Быстрая фильтрация Option'а по типу.  */
      def filterByType(jdtName: MJdTagName): Option[IDocTag] = {
        opt.filter(_.jdTagName ==* jdtName)
      }

    }

    /** Утиль для поддержки z.Tree с jd-тегами. */
    implicit class IDocTagZTreeOps(private val tree: Tree[IDocTag]) extends AnyVal {

      import io.suggest.scalaz.ZTreeUtil._

      def deepEdgesUidsIter: Iterator[EdgeUid_t] = {
        val jdt = tree.rootLabel
        val iter1 = jdt.props1.qdOps
          .iterator
          .flatMap(_.edgeInfo)
        val iter2 = jdt.props1.bgImg
          .iterator
          .map(_.imgEdge)
        val iter12 = (iter1 ++ iter2).map(_.edgeUid)
        val iterChs = tree.deepChildrenIter
          .flatMap(_.deepEdgesUidsIter)
        iter12 ++ iterChs
      }


      def deepChildrenOfTypeIter(jdtName: MJdTagName): Iterator[IDocTag] = {
        tree
          .deepChildrenIter
          .filter( _.jdTagName ==* jdtName )
      }

      def deepOfTypeIter(jdtName: MJdTagName): Iterator[IDocTag] = {
        val chIter = deepChildrenOfTypeIter(jdtName)
        val jdt = tree.rootLabel
        if (jdt.jdTagName ==* jdtName) {
          Iterator(jdt) ++ chIter
        } else {
          chIter
        }
      }


    }

  }

  /** Реализация операций для управления деревом, которое прямо внутри [[IDocTag]].
    * Это устаревшее дерево, надо заменить его на scalaz.Tree[IDocTag].
    *
    * @param tree Дерево IDocTag-тегов.
    */
  implicit final class LegacyJdTreeOps(private val tree: IDocTag) extends AnyVal {

    def nodeToPath(el: IDocTag): Option[NodePath_t] = {
      if (tree ==* el) {
        Some(Nil)
      } else if (tree.children.isEmpty) {
        None
      } else {
        tree
          .children
          .iterator
          .zipWithIndex
          .flatMap { case (node, i) =>
            node
              .nodeToPath(el)
              .map(i :: _)
          }
          .toStream
          .headOption
      }
    }

    @tailrec
    def pathToNode(path: NodePath_t): Option[IDocTag] = {
      if (path.isEmpty) {
        Some(tree)
      } else {
        val hd :: tl = path
        if (tree.children isDefinedAt hd) {
          val ch = tree.children(hd)
          ch.pathToNode( tl )
        } else {
          None
        }
      }
    }

    def deepMap(f: IDocTag => IDocTag): IDocTag = {
      val tree1 = if (tree.children.isEmpty) {
        tree
      } else {
        val chs2 = for (ch <- tree.children) yield {
          ch.deepMap(f)
        }
        if (Lists.isElemsEqs(tree.children, chs2)) {
          tree
        } else {
          tree.withChildren( chs2 )
        }
      }
      f(tree1)
    }

  }

}


/** Интерфейс для всех "тегов" структуры документа.
  *
  * IIdentityFastEq:
  * Теги из дерева используются как ключи в ScalaCSS styleF Map[X,_] прямо во время рендера.
  * Во время тормозных react-рендеров и перерендеров в браузере, ключи должны **очень** быстро работать,
  * поэтому всё оптимизировано по самые уши ценой невозможности сравнивания разных тегов между собой.
  */
final case class IDocTag(
                          jdTagName : MJdTagName,
                          props1    : MJdtProps1    = MJdtProps1.empty,
                          children  : Seq[IDocTag]  = Nil
                        )
  extends IHashCodeLazyVal
  // TODO Opt: lazy val: на клиенте желательно val, на сервере - просто дефолт (def). Что тут делать, elidable нужен какой-то?
  with IEqualsEq
  with IJdElement
{

  def withJdTagName(jdTagName: MJdTagName)    = copy(jdTagName = jdTagName)

  def withProps1(props1: MJdtProps1)          = copy(props1 = props1)
  /** Для удобства написания тестов, props1 можно обновлять функцией. */
  def updateProps1(f: MJdtProps1 => MJdtProps1) = withProps1(f(props1))

  def withChildren(children: Seq[IDocTag])    = copy(children = children)


  /** Итератор текущего элемента и всех его под-элементов со всех под-уровней. */
  def deepIter: Iterator[IDocTag] = {
    Iterator(this) ++ deepChildrenIter
  }

  /** Итератор всех дочерних элементов со всех под-уровней. */
  def deepChildrenIter: Iterator[IDocTag] = {
    children.iterator
      .flatMap { _.deepIter }
  }

  def deepEdgesUidsIter: Iterator[EdgeUid_t] = {
    val iter1 = props1.qdOps.iterator
      .flatMap(_.edgeInfo)
    val iter2 = props1.bgImg.iterator
      .map(_.imgEdge)
    val iter12 = (iter1 ++ iter2).map(_.edgeUid)
    val iterChs = deepChildrenIter.flatMap(_.deepEdgesUidsIter)
    iter12 ++ iterChs
  }

  def deepChildrenOfTypeIter(jdtName: MJdTagName): Iterator[IDocTag] = {
    deepChildrenIter
      .filter( _.jdTagName ==* jdtName )
  }

  def deepOfTypeIter(jdtName: MJdTagName): Iterator[IDocTag] = {
    val chIter = deepChildrenOfTypeIter(jdtName)
    if (jdTagName ==* jdtName) {
      Iterator(this) ++ chIter
    } else {
      chIter
    }
  }


  /** Найти в дереве указанный тег в дереве и обновить его с помощью функции. */
  def deepUpdateOne(what: IDocTag, updated: Seq[IDocTag]): Seq[IDocTag] = {
    // Обновляем текущий тег
    if (this ==* what) {
      updated
    } else {
      // Попробовать пообнавлять children'ов.
      deepUpdateChild( what, updated ) :: Nil
    }
  }


  /** Найти в дереве указанный тег в дочерних поддеревьях и обновить его с помощью функции.
    * Поиск в дереве идёт исходят из того, что элемент там есть, и он должен быть найден
    * как можно ближе к корню дерева. Поэтому сначала обрабатывается полностью над-уровень, и
    * только если там ничего не найдено, то происходит рекурсивное погружение на следующий уровень.
    *
    * Если вдруг одинаковый инстанс тега встречается несколько раз на разных уровнях,
    * то будет обновлён только наиболее верхний уровень с найденными тегами. Но это считается
    * вообще ненормальной и неправильной ситуацией, поэтому не следует использовать boopickle
    * для редактируемых json-документов.
    *
    * @param what Инстанс искомого тега.
    * @param updated Функция обновления дерева.
    * @return Обновлённое дерево.
    */
  def deepUpdateChild(what: IDocTag, updated: Seq[IDocTag]): IDocTag = {
    val _children = children
    if (_children.isEmpty) {
      this

    } else {
      val this2 = if (_children contains what) {
        // Обновление элемента на текущем уровне
        val children2 = _children.flatMap { jdt =>
          if (jdt ==* what) {
            updated
          } else {
            jdt :: Nil
          }
        }
        withChildren(children2)

      } else {
        // Обновление элементов где-то на подуровнях дочерних элементов.
        val children2 = _children.flatMap { jdt =>
          jdt.deepUpdateChild(what, updated) :: Nil
        }
        // Возможно, что ничего не изменилось. И тогда можно возвращать исходный элемент вместо пересобранного инстанса.
        if ( Lists.isElemsEqs(_children, children2) ) {
          this
        } else {
          withChildren( children2 )
        }
      }

      this2
    }
  }


  /** Глубинный flatMap(): на каждом уровне сначала отрабатываются дочерние элементы, затем родительский. */
  /*
  def flatMap(f: IDocTag => Seq[IDocTag]): Seq[IDocTag] = {
    val this2 = if (children.isEmpty) {
      this
    } else {
      withChildren(
        children.flatMap { ch =>
          ch.flatMap(f)
        }
      )
    }
    f(this2)
  }
  */

/*
  def foreach[U](f: IDocTag => U): Unit = {
    for (ch <- children) {
      ch.foreach(f)
    }
    f(this)
  }*/


  def contains(jdt: IDocTag): Boolean = {
    deepChildrenIter.contains( jdt )
  }


  // IJdElement:

  override def deepElMap(f: (IJdElement) => IJdElement): IDocTag = {
    val jdt0 = this

    // Отработать qdOps с учётом того, что они могут не измениться.
    val qdOps0 = jdt0.props1.qdOps
    val qdOps2 = qdOps0
      .map( _.deepElMap(f) )
    val jdt1 = if (Lists.isElemsEqs(qdOps0, qdOps2)) {
      jdt0
    } else {
      jdt0.withProps1(
        jdt0.props1.withQdOps(
          qdOps2
        )
      )
    }

    // Отработать children, которые могут не измениться:
    val children1 = jdt1.children
    val children2 = children1
      .map( _.deepElMap(f) )
    val jdt2 = if (Lists.isElemsEqs(children1, children2)) {
      jdt1
    } else {
      jdt1
        .withChildren( children2 )
    }

    f(jdt2)
      .asInstanceOf[IDocTag]
  }


  override def bgImgEdgeId = props1.bgImg.map(_.imgEdge)

  override def setBgColor(bgColor: Option[MColorData]): IDocTag = {
    withProps1(
      props1.withBgColor(bgColor)
    )
  }

  override def toScalazTree: Tree[IJdElement] = {
    Tree.Node(
      root   = this,
      forest = {
        Iterator(
          children,
          props1.qdOps
        )
          .filter(_.nonEmpty)
          .flatten
          .map(_.toScalazTree)
          .toStream
      }
    )
  }


  def findTagsByChildQdEl(el: IJdElement): TraversableOnce[IDocTag] = {
    val thisIter = if (props1.qdOps.contains(el)) {
      Iterator(this)
    } else {
      Iterator.empty
    }
    thisIter ++ children.iterator.flatMap(_.findTagsByChildQdEl(el))
  }

}
